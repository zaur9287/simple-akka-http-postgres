package com.base.app.models.caseclass

case class TableRow(
                id: Int,
                name: String,
                participants: Int
                )

case class SubscribeTable(
                         $type: String,
                         tables: Seq[TableRow]
                         )

case class TableClient(
                        id: Option[Int],
                        name: String,
                        participants: Int
                      )

case class TableCU( //table create and update
                $type: String,
                after_id: Option[Int],
                table: TableClient
                )
case class TableMessages(
                        $type: String,
                        id: Int
                        )