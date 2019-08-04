package com.base.app.models.caseclass

case class User(
                 id: Int,
                 name: String,
                 password: String,
                 userType: String
               )

case class LoginData(
                    $type: String,
                    username: String,
                    password: String
                    )

object UserType extends Enumeration {
  val admin = Value(1, "admin")
  val user = Value(2, "user")
}
