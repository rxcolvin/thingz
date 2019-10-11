package sql

import expression.*
import jdbc.ColName
import logging.Logger

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
    val constraint: Constraint,
    val isPrimary: Boolean = false
) {
    override fun toString(): String = name + " " + type + " " + constraint
}

open class Table(
    val schemaName: String,
    val tableName: String,
    val columnDefs: List<ColumnDef<*, *>>
) {
    fun columnDef(name: ColName): ColumnDef<*,*>? = columnDefs.firstOrNull { it.name == name }

}

fun varchar(
    name: String,
    length: Int,
    constraint: Constraint = NOTNULL,
    isPrimary: Boolean = false
) = ColumnDef(name, Varchar(length), constraint, isPrimary)

fun int(
    name: String,
    constraint: Constraint = NOTNULL
) = ColumnDef(name, INTEGER, constraint)


class GenericSqlHelper(
    val logger: Logger = Logger("Default", debugEnabled = true)
) : SqlHelper {

    override fun createTableSql(
        table: Table

    ): SqlStatement {

        val listBuilder = mutableListOf<String>()

        val sql = buildString {
            with(table) {
                appendln("CREATE TABLE ${tableName} (")
                appendln(
                    columnDefs.map {
                        it.toString()
                    } .joinToString(prefix = "  ", separator = ",\n  ")
                )
                append(")")
            }
        }
        return SqlStatement(sql, emptyList())
    }

    override fun dropTableSql(
        table: Table
    ) =
        SqlStatement("DROP TABLE ${table.tableName}")


    override fun insertSql(
        table: Table
    ): SqlStatement {
        val sql = ("INSERT INTO ${table.tableName} (${table.columnDefs.map { it.name }.joinToString(", ")}) " +
                "VALUES (${table.columnDefs.map { "?" }.joinToString(", ")})")
        return SqlStatement(sql, table.columnDefs.map{it.name})
    }

    override fun updateSql(
        table: Table
    ): SqlStatement {
        val sql = "UPDATE "
        return SqlStatement(sql, table.columnDefs.map{it.name})
    }

    override fun selectSql(
        table: Table,
        columnNames: List<ColName>,
        where: Expr?,
        orderBy: List<OrderItem>?,
        paging: Paging?
    ): SqlStatement {

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
        return SqlStatement(sql, parameters.toList())
    }

    override fun deleteSql(
        table: Table,
        where: Expr
    ) = SqlStatement("TODO")

    override fun heartBeatSql(
        table: Table
    )= SqlStatement("TODO")


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
