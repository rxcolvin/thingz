package sql

import expression.Eq
import expression.Field
import expression.Parameter
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrow
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature
import kotlin.test.assertTrue

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


object SetFeature : Spek({
    Feature("GenericSqlHelper") {
        val target by memoized { GenericSqlHelper(FooTable.it) }

        Scenario("Generate SQL to create table") {
            lateinit var sql: String
            When("Creating a table with ") {
                sql = target.createTableSql()
            }
            Then("it should the given string") {
                println(sql)
                assertTrue { true }
            }
        }
        Scenario("Generate SQL to select data from a table and return the parameters mapped") {
            lateinit var res: Pair<String, List<String>>
            When("Given  ") {
                res = target.selectSql(
                    where = expression.And(
                        listOf(
                            Eq<Int>(Field("age"), Parameter("age")),
                            Eq<Int>(Field(FooTable.name), Parameter(FooTable.name)),
                            Eq<Int>(Field(FooTable.age), Parameter(FooTable.age))
                        )
                    )
                )
            }
            Then("it should the correct sql") {
                println(res.first)
                println(res.second)
                assertTrue { true }
            }
        }
    }

    Feature("Set") {
        val set by memoized { mutableSetOf<String>() }

        Scenario("adding items") {
            When("adding foo") {
                set.add("foo")
            }

            Then("it should have a size of 1") {
                set.size shouldEqual 1
            }

            Then("it should contain foo") {
                set shouldContain "foo"
            }
        }

        Scenario("empty") {
            Then("should have a size of 0") {
                set.size shouldEqual 0
            }

            Then("should throw when first is invoked") {
                { set.first() }.shouldThrow(Exception::class)

            }
        }

        Scenario("getting the first item") {
            val item = "foo"
            Given("a non-empty set") {
                set.add(item)
            }

            lateinit var result: String

            When("getting the first item") {
                result = set.first()
            }

            Then("it should return the first item") {
                item shouldEqual result
            }
        }
    }
})