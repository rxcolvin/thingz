package sql

import expression.*
import logging.Logger


interface SqlHelper {

    val table: Table

    fun createTableSql(): String

    fun dropTableSql(): String

    fun insertSql(): String

    fun updateSql(): String

    fun selectSql(
        columnNames: List<String> = table.allColumnDefs.map { it.name },
        where: Expr? = null,
        orderBy: List<OrderItem>? = null,
        paging: Paging? = null
    ): Pair<String, List<String>>

    fun deleteSql(
        where: Expr
    ): String

    fun heartBeatSql(): String

    interface Factory {
        fun create(table: Table): SqlHelper
    }
}


class OrderItem(
    val column: ColumnDef<*, *>,
    val isDesc: Boolean = true
)

class Paging(
    val pageNo: Int,
    val pageSize: Int
)


class ColumnDef<T : Any, ST : SqlType<T>>(
    val name: String,
    val type: ST,
    val constraint: Constraint
) {
    override fun toString(): String = name + " " + type + " " + constraint
}

open class Table(
    val schemaName: String,
    val tableName: String,
    val columnDefs: List<ColumnDef<*, *>>,
    val primaryColumnDefs: List<ColumnDef<*, *>> = emptyList()
) {
    val allColumnDefs = primaryColumnDefs + columnDefs
    fun columnDef(name: String) = allColumnDefs.firstOrNull { it.name == name }

}

fun varchar(
    name: String,
    length: Int,
    constraint: Constraint = NOTNULL
) = ColumnDef(name, Varchar(length), constraint)

fun int(
    name: String,
    constraint: Constraint = NOTNULL
) = ColumnDef(name, INTEGER, constraint)


class GenericSqlHelper(
    override val table: Table,
    val logger: Logger = Logger("Default", debugEnabled = true)
) : SqlHelper {

    override fun createTableSql(
    ): String = buildString {
        with(table) {
            appendln("CREATE TABLE ${tableName} (")
            appendln(
                ( (primaryColumnDefs+ columnDefs).map {
                    it.toString()
                } + if (primaryColumnDefs.any()) {
                    "PRIMARY KEY (" +
                            primaryColumnDefs.joinToString(separator = ",") { it.name } +
                            ")"

                } else "").joinToString(prefix = "  ", separator = ",\n  ")
            )

            append(")")
        }
    }.debug { "createTableSql=$it" }


    override fun dropTableSql() =
        "DROP TABLE ${table.tableName} ".debug { "dropTableSql()=$it" }


    override fun insertSql(): String =
        ("INSERT INTO ${table.tableName} (${table.allColumnDefs.map { it.name }.joinToString(", ")}) " +
                "VALUES (${table.allColumnDefs.map { "?" }.joinToString(", ")})")
                    .debug { "insertSql()=$it" }

    override fun updateSql(): String =
        "UPDATE ".debug { "updateSql()=$it" }

    override fun selectSql(
        columnNames: List<String>,
        where: Expr?,
        orderBy: List<OrderItem>?,
        paging: Paging?
    ): Pair<String, List<String>> {

        val parameters = mutableListOf<String>()
        val columnDefs = columnNames.map {
            table.columnDef(it) ?: throw Exception()
        }

        val sql = buildString {
            append("SELECT\n")
            append(
                "  " + columnDefs.map { it.name }.joinToString(", ")
            )
            append("\nFROM")
            append("  ${table.tableName} \n")
            if (where != null || paging != null) {
                append("WHERE\n")
            }
            if (where != null) {
                append("  " + toSQL(where, parameters) + "\n")
            }
            if (orderBy != null) {
                append(" ORDER BY ")
                orderBy.map { it.column.name + if (it.isDesc) "DESC" else "ASC" }
            }
            if (paging != null) {
                append("  LIMIT ${paging.pageNo} ${paging.pageSize}")
            }
        }
        logger.debug {
            "selectSQL(columnNames=$columnNames, where=$where, orderBy=$orderBy, paging=$paging\n)=($sql, $parameters)"
        }
        return Pair(sql, parameters.toList())
    }

    override fun deleteSql(where: Expr): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun heartBeatSql(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    object Factory : SqlHelper.Factory {
        override fun create(table: Table): SqlHelper = GenericSqlHelper(table)
    }

    private fun String.debug(s: (String) -> Any): String = logger.debug(this, s)

}

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