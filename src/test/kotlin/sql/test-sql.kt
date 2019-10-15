package sql

import expression.Eq
import expression.Field
import expression.Parameter
import org.junit.jupiter.api.Test

object FooTable {
    const val name = "name"
    const val foo = "foo"
    const val age = "age"
    const val bar = "name"

    val it =  Table(
        tableName = "Foo",
        schemaName = "",
        columnDefs = listOf(
            varchar(name, 10, Constraint.NOTNULL, Primary.FIRST),
            varchar(foo, 10, Constraint.NOTNULL, Primary.SECOND),
            int(age, Constraint.NOTNULL),
            int(bar, Constraint.NOTNULL)
        )
    )
}

class Tests {
    @Test
    fun `Generate SQL to create table`() {
        val target = DerbySqlHelper()
        val sql = target.createTableSql(FooTable.it)
        println(sql.sql)
    }

    @Test
    fun `Select SQL `() {
        val target = DerbySqlHelper()
        val res = target.selectSql(
            table = FooTable.it,
            where = expression.And(
                listOf(
                    Eq<Int>(Field("age"), Parameter("age")),
                    Eq<Int>(Field(FooTable.name), Parameter(FooTable.name)),
                    Eq<Int>(Field(FooTable.age), Parameter(FooTable.age))
                )
            )
        )
    }
}