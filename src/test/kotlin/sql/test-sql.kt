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
            varchar(name, 10, NOTNULL),
            varchar(foo, 10, NOTNULL),
            int(age, NOTNULL),
            int(bar, NOTNULL)
        )
    )
}

class Tests {
    @Test
    fun `Generate SQL to create table`() {
        val target = GenericSqlHelper()
        val sql = target.createTableSql(FooTable.it)
        println(sql.sql)
    }

    @Test
    fun `Select SQL `() {
        val target = GenericSqlHelper()
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