package myapplication

import expression.Const
import expression.Eq
import expression.Parameter
import jdbc.DriverManager
import jdbcstorage.JdbcStorageManager
import lang.Email
import meta.*
import sql.GenericSqlHelper
import java.util.*

data class Address(
    val houseName: String
)

data class Address_(
    var houseName: String
) : Builder<Address> {
    override fun create() = Address(houseName =this.houseName)
}

data class Person(
    val uuid: UUID,
    val name: String,
    val age: Int,
    val email: Email,
    val address: Address
)

data class Person_(
    var uuid: UUID,
    var name: String,
    var age: Int,
    var email: Email,
    var address: Address
) : Builder<Person> {
    override fun create() = Person(
        uuid = uuid,
        name = this.name,
        age = this.age,
        email = this.email,
        address = this.address
    )
}



object DataDictionary {

}

val nameField = Field("name", StringType)
val ageField = Field("age", IntType)
val emailField = Field("email", EmailType)
val uuidField = Field("uuid", UUIDType)

val houseName = Field("houseName", StringType)

object AddressType:  ComplexType<Address> (
    typeName = "address",
    fields = listOf(
        houseName
    )
)

val addressField = Field("address", AddressType)

val personType = EntityType<Person, UUID>(
    typeName = "Person",
    fields = listOf(
        nameField,
        ageField,
        emailField,
        addressField

    ),
    identityField = uuidField
)



fun main() {
    System.getProperty("java.class.path").split(";").forEach {
        println(it)
    }

    try {

        val personSqlEntityMeta = SqlEntityMeta<Person, UUID>(
            entityType = personType,
            fields = personType.fields.map { sqlFieldMeta(it) },
            identitySqlField = sqlFieldMeta(personType.identityField) as SqlFieldMeta<UUID, AtomicType<UUID>>
        )

        val personEntityMapper = EntityMapper<Person, Person_>(
            fieldMappers = listOf(
                FieldMapper(
                    field = uuidField,
                    getter = { it.uuid },
                    setter_ = { x, it -> x.uuid = it },
                    getter_ = { it.uuid }
                ),
                FieldMapper(
                    field = nameField,
                    getter = { it.name },
                    setter_ = { x, it -> x.name = it },
                    getter_ = { it.name }
                ),
                FieldMapper(
                    field = ageField,
                    getter = { it.age },
                    setter_ = { x, it -> x.age = it },
                    getter_ = { it.age }
                ),
                FieldMapper(
                    field = emailField,
                    getter = { it.email },
                    setter_ = { x, it -> x.email = it },
                    getter_ = { it.email }
                ),
                FieldMapper(
                    field = addressField,
                    getter = { it.address },
                    setter_ = { x, it -> x.address = it },
                    getter_ = { it.address }

                )

            ),
            builder = {
                Person_(
                    uuid = UUID.randomUUID(),
                    name = "",
                    age = 0,
                    email = Email("", ""),
                    address = Address("")
                )
            }
        )

        Class.forName("org.apache.derby.jdbc.EmbeddedDriver")
        val cxn = DriverManager.getConnection("jdbc:derby:/derbydatabases/testone;create=true")

        val storageManager = JdbcStorageManager<UUID, Person, Person_>(
            sqlHelperFactory = GenericSqlHelper.Factory,
            sqlEntityMeta = personSqlEntityMeta,
            entityMapper = personEntityMapper,
            connection = cxn
        )

        storageManager.describeSchema()
        storageManager.sqlHelper.insertSql()

        storageManager.sqlHelper.selectSql(
            where = Eq(
                Parameter("name"), Const("Bob")
            )
        )

        try {
            storageManager.dropSchema()
        } catch (ex:Exception) {

        }
        storageManager.createSchema()
        val person = Person(
            uuid = UUID.randomUUID(),
            name = "Fred",
            age = 24,
            email = Email("Fred", "Bloggs.com"),
            address = Address("Chez Moi")
        )
        storageManager.create(person)
        println(storageManager.getById(person.uuid))


    } catch (e: Exception) {
        e.printStackTrace()
    }
}

