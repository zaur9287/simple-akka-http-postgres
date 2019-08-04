package com.base.app.models.dao

import com.base.app.models.caseclass.{SubscribeTable, TableCU, TableClient, TableMessages, TableRow, User}
import java.sql.{Connection, DriverManager, SQLException}

import akka.actor.ActorSystem

import scala.concurrent.{ExecutionContext, Future}

trait DAOS {

  val url = "jdbc:postgresql://localhost:5432/home_work"
  val driver = "org.postgresql.Driver"
  val username = "postgres"
  val password = "1"
  var connection: Connection = _

  try {
    Class.forName(driver)
    connection = DriverManager.getConnection(url, username, password)
    connection.setAutoCommit(false)

  } catch {
    case e: Exception => e.printStackTrace
  }
  //connection.close()

  implicit val system = ActorSystem()
  implicit val executionContext: ExecutionContext = system.dispatcher

  def find(user: User): Future[Option[User]] = {
    var result: Option[User] = None
    val statement = connection.createStatement
    val rs = statement.executeQuery(s"SELECT * FROM users WHERE name = '${user.name}' AND password = '${user.password}'")

    while (rs.next) {

      result = Some(User(
        rs.getInt("id"),
        rs.getString("name"),
        rs.getString("password"),
        rs.getString("user_type")
      ))

    }
    rs.close()
    Future(result)
  }


  def listTable = {

    val run = connection.createStatement().executeQuery( "SELECT * FROM tabular" )
    var tables: Seq[TableRow] = Seq()
    while (run.next) {

      tables = tables :+ TableRow(
        run.getInt("id"),
        run.getString("name"),
        run.getInt("participants")
      )
    }
    run.close()

    Future(SubscribeTable($type = "table_list", tables))
  }


  def removeTable(tableMessages: TableMessages) = {
    var result = tableMessages

    val query = s"DELETE FROM tabular where id = ${tableMessages.id}"
    val affectedRows = connection.prepareStatement(query).executeUpdate()
    try {
      connection.commit()
      println("execution successfully completed.")
    } catch {
      case ex: SQLException => {
        println("update table \n", ex)
        connection.rollback()
        result = result.copy($type = "removal_failed")
      }
    }
    if ( affectedRows == 0 ) result = result.copy($type = "row didn't exist")

    Future(result)
  }

  def updateTable(tableCU: TableCU) = {
    var failed: Boolean = false
    val query = s"UPDATE tabular SET name = '${tableCU.table.name}', participants = ${tableCU.table.participants}  WHERE id = ${tableCU.table.id.getOrElse(0)}"
    connection.prepareStatement(query).execute()
    try {
      connection.commit()
      println("table successfully updated.")
    } catch {
      case ex: SQLException => {
        println("update table \n", ex)
        connection.rollback()
        failed = true
      }
    }
    Future(tableCU , failed)
  }

  def addTable(tableCU: TableCU) = {

    /*
    firstly locate the after_id to current database
    after insert newone to db
    */
    var firstRow: Option[TableClient] = None
    var res: Option[TableCU] = None

    if (tableCU.after_id.isDefined) {
      val query = s"SELECT * FROM tabular where id = ${tableCU.after_id.get}"
      val firstResult = connection.createStatement().executeQuery(query)
      while ( firstResult.next() ){
        firstRow = Some(TableClient(
          id = Some(firstResult.getInt("id")),
          name = firstResult.getString("name"),
          participants= firstResult.getInt("participants")
        ))
      }
    }
    if (firstRow.isDefined) {
      val updateQuery = s"UPDATE tabular SET name = '${tableCU.table.name}', participants = ${tableCU.table.participants} WHERE id = ${tableCU.after_id.get}"
      connection.prepareStatement(updateQuery).execute()

      val insertFirstRow = s"INSERT INTO tabular (name, participants) values ('${firstRow.get.name}', ${firstRow.get.participants})"
      connection.prepareStatement(insertFirstRow).execute()

      try {
        connection.commit()
        println("Update and insert operations are successfully completed!")

        val result = connection.createStatement().executeQuery(s"SELECT * FROM tabular WHERE id = ${tableCU.after_id.get}")
        while (result.next()) {
          res = Some(TableCU(
            $type = "table_added",
            after_id = tableCU.after_id,
            table =
              TableClient(
                id = Some(result.getInt("id")),
                name = result.getString("name"),
                participants = result.getInt("participants")
              )
          ))
        }
        result.close()

      } catch{

        case ex: SQLException => {
          println("exception from update and insert", ex)
          connection.rollback()
        }
      }

    } else {
      val updateQuery = s"INSERT INTO tabular (name, participants) values ('${tableCU.table.name}', ${tableCU.table.participants})"
      connection.prepareStatement(updateQuery).execute()

      try {
        connection.commit()
        println("Insert operation is successfully completed!")

        val result = connection.createStatement().executeQuery(s"SELECT * FROM tabular WHERE name = '${tableCU.table.name}' AND participants = ${tableCU.table.participants}")
        while (result.next()) {
          res = Some(TableCU(
            $type = "table_added",
            after_id = tableCU.after_id,
            table =
              TableClient(
                id = Some(result.getInt("id")),
                name = result.getString("name"),
                participants = result.getInt("participants")
              )
          ))
        }
        result.close()
      } catch {

        case ex: SQLException => {
          println("exception from insert", ex)
          connection.rollback()
        }
      }
    }
    Future(res)
  }

}