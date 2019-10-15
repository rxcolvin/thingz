package sql

import expression.Expr
import logging.Logger
import java.sql.SQLNonTransientException
import java.sql.SQLSyntaxErrorException

class DerbySqlHelper(
    val logger: Logger = Logger("Default", debugEnabled = true)
) : AbstractSqlHelper() {

    override fun createTableSql(
        table: Table

    ): SqlStatement {

         val sql = buildString {
            with(table) {
                appendln("CREATE TABLE ${tableName} (")
                append(
                    columnDefs.map {
                        it.name +" " + it.type.asString() + " " + it.constraint.asString()
                    } .joinToString(prefix = "  ", separator = ",\n  ")
                )
                table.columnDefs.primary()?.let {
                    append(",\n  ")
                    append(it)
                }
                appendln()
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

    override fun validateDropTableException(
        table: Table,
        ex: SQLNonTransientException
    ): Boolean =
        ex is SQLSyntaxErrorException && ex.message == "'DROP TABLE' cannot be performed on '${table.tableName}' because it does not exist."

    private fun SqlType.asString() : String =
        when (this) {
            is VARCHAR -> "varchar(${this.length})"
            INTEGER -> "int"
        }

    private fun Primary.asString() : String =
        when (this) {
            Primary.NONE -> ""
            Primary.FIRST -> "PRIMARY KEY"
            else -> ""
        }

    private fun Constraint.asString() : String =
        when (this) {
            Constraint.NOTNULL -> "NOT NULL"
            Constraint.UNIQUE -> "NOT NULL UNIQUE"
        }

    private fun List<ColumnDef>.primary() : String? {
        val primaryColumns = this.filter { it.isPrimary != Primary.NONE } .sortedBy { it.isPrimary.order }
        return if (primaryColumns.any()) {
            "PRIMARY KEY (${primaryColumns.map {it.name}.joinToString(separator = ", ")})"
        } else {
            null
        }
    }
}
