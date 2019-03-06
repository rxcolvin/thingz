package sql

import kotlin.reflect.KClass

interface SqlType<T  : Any> {
  val klassOf_T: KClass<T>
  val name: String
}

class Varchar (
    val length: Int
) : SqlType<String> {
  override val klassOf_T: KClass<String> = String::class
  override val name: String = "varchar"
  override fun toString(): String  = "$name($length)"
}

object INTEGER : SqlType<Int> {
  override val klassOf_T: KClass<Int> = Int::class
  override val name: String = "int"
  override fun toString(): String = name
}

interface Constraint {
  val name: String
}

object NOTNULL : Constraint {
  override val name: String = "NOT NULL"
  override fun toString(): String = name
}

object UNIQUE : Constraint {
  override val name: String = "NOT NULL UNIQUE"
  override fun toString(): String = name
}