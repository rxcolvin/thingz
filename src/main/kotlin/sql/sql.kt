package sql

import expression.*
import java.sql.SQLNonTransientException

typealias ColName = String

sealed class SqlType


class VARCHAR (
    val length: Int
) : SqlType() {
}

object INTEGER : SqlType()


enum class Constraint {
    UNIQUE, NOTNULL
}

enum class Primary(val order:Int = 0) {
    NONE(0), FIRST(1), SECOND(2)
}

fun varchar(
    name: String,
    length: Int,
    constraint: Constraint = Constraint.NOTNULL,
    isPrimary: Primary = Primary.NONE
) = ColumnDef(
    name = name.toUpperCase(),
    type= VARCHAR(length),
    constraint = constraint,
    isPrimary = isPrimary
)

fun int(
    name: String,
    constraint: Constraint =  Constraint.NOTNULL
) = ColumnDef(
    name = name.toUpperCase(),
    type = INTEGER,
    constraint = constraint
)


class SqlStatement(
    val sql: String,
    val parameters: List<String> = emptyList()
)

interface SqlHelper {

    fun createTableSql(table: Table): SqlStatement

    fun dropTableSql(table: Table): SqlStatement

    fun insertSql(table: Table): SqlStatement

    fun updateSql(table: Table): SqlStatement

    fun selectSql(
        table: Table,
        columnNames: List<String> = table.columnDefs.map { it.name },
        where: Expr? = null,
        orderBy: List<OrderItem>? = null,
        paging: Paging? = null
    ): SqlStatement

    fun deleteSql(
        table: Table,
        where: Expr
    ): SqlStatement

    fun heartBeatSql(
        table: Table
    ): SqlStatement

    fun validateDropTableException(
        table: Table,
        ex: SQLNonTransientException
    ) : Boolean

}


class OrderItem(
    val column: ColumnDef,
    val isDesc: Boolean = true
)

class Paging(
    val pageNo: Int,
    val pageSize: Int
)


class ColumnDef(
    val name: String,
    val constraint: Constraint,
    val type: SqlType,
    val isPrimary: Primary = Primary.NONE
)

open class Table(
    val schemaName: String,
    val tableName: String,
    val columnDefs: List<ColumnDef>
) {
    fun columnDef(name: ColName): ColumnDef? = columnDefs.firstOrNull { it.name == name }

}



abstract class AbstractSqlHelper() : SqlHelper


fun toSQL(
    test: Expr,
    parameters: MutableList<String>
): String {
    val sql = when (test) {
        is Const<*> -> sqlize(test.value)
        is Field<*> -> test.id
        is And -> test.exprs.map { toSQL(it, parameters) }.joinToString(
            prefix = "(",
            postfix = ")",
            separator = " AND "
        )
        is Eq<*> -> {
            if (!test.left.isNull() && !test.right.isNull()) {
                toSQL(test.left, parameters) + " = " + toSQL(test.right, parameters)
            } else if (!test.left.isNull() && test.right.isNull()) {
                toSQL(test.left, parameters) + "IS NULL"
            } else if (test.left.isNull() && !test.right.isNull()) {
                toSQL(test.right, parameters) + "IS NULL"
            } else {
                "1==1"
            }
        }
        is Parameter<*> -> {
            parameters.add(test.id); "?"
        }
        else -> "TODO"
    }
    return sql
}

fun toSQL(
    test: Expr
): String {
    val sql = when (test) {
        is Const<*> -> sqlize(test.value)
        is Field<*> -> test.id
        is And -> test.exprs.map { toSQL(it) }.joinToString(prefix = "(", postfix = ")", separator = " AND ")
        is Eq<*> -> {
            if (!test.left.isNull() && !test.right.isNull()) {
                toSQL(test.left) + " = " + toSQL(test.right)
            } else if (!test.left.isNull() && test.right.isNull()) {
                toSQL(test.left) + "IS NULL"
            } else if (test.left.isNull() && !test.right.isNull()) {
                toSQL(test.right) + "IS NULL"
            } else {
                "1==1"
            }
        }
        is Parameter<*> -> {
            ":" + test.id
        }
        else -> "TODO"
    }
    return sql
}


fun sqlize(v: Any?): String =
    when (v) {
        null -> "NULL"
        is String -> '\'' + v + '\''
        else -> v.toString()
    }

inline fun <T> T.runThis(block: T.() -> Unit): T {
    block()
    return this
}
