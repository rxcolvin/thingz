package myapplication

import jdbc.DriverManager
import jdbcstorage.SimpleDbMapper
import jdbcstorage.SimpleJdbcStorageManager
import lang.Email
import meta.*
import sql.GenericSqlHelper
import java.util.*

val nullUUID = UUID.fromString("THISISNOTAUUIIDASDASD")

data class Address(
    val houseName: String
)

data class Address_(
    var houseName: String
) {
    constructor() : this("")
    constructor(it: Address) : this(it.houseName)

    fun create() = Address(houseName = this.houseName)
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
    var address: Address_
) {
    constructor() : this(
        nullUUID,
        "", 0, Email(), Address_()
    )

    constructor(it: Person) : this(it.uuid, it.name, it.age, it.email, Address_(it.address))

    fun create() = Person(
        uuid = uuid,
        name = this.name,
        age = this.age,
        email = this.email,
        address = this.address.create()
    )
}


object DataDictionary {

}

val nameField = Field("name", StringType)
val ageField = Field("age", IntType)
val emailField = Field("email", EmailType)
val uuidField = Field("uuid", UUIDType)

val houseName = Field("houseName", StringType)

val AddressType = ComplexType(
    typeName = "address",
    fields = listOf(
        EntityField(
            field = houseName,
            getter = { it.houseName },
            getter_ = { it.houseName },
            setter_ = { x, it -> x.houseName = it }
        )
    ),
    buildNew = { Address_() },
    buildCopy = { Address_(it) },
    build = { it.create() }
)

val addressField = Field("address", AddressType)

val PersonType = EntityType(
    typeName = "Person",
    identityField = EntityAtomicField(
        field = uuidField,
        getter = { it.uuid },
        setter_ = { x: Person_, it -> x.uuid = it },
        getter_ = { it.uuid },
        isKey = true,
        isSearchable = true
    ),
    fields = listOf(
        EntityAtomicField(
            field = nameField,
            getter = { it.name },
            setter_ = { x, it -> x.name = it },
            getter_ = { it.name }
        ),
        EntityAtomicField(
            field = ageField,
            getter = { it.age },
            setter_ = { x, it -> x.age = it },
            getter_ = { it.age }
        ),
        EntityAtomicField(
            field = emailField,
            getter = { it.email },
            setter_ = { x, it -> x.email = it },
            getter_ = { it.email }
        ),
        EntityComplexField(
            field = addressField,
            getter = { it.address },
            setter_ = { x, it -> x.address = Address_(it) },
            getter_ = { it.address.create() }
        )
    ),
    buildNew = { Person_() },
    buildCopy = { Person_(it) },
    build = { it.create() }

)


fun main() {
    System.getProperty("java.class.path").split(";").forEach {
        println(it)
    }

    try {


        Class.forName("org.apache.derby.jdbc.EmbeddedDriver")
        val cxn = DriverManager.getConnection("jdbc:derby:/derbydatabases/testone;create=true")

        val storageManager = SimpleJdbcStorageManager<UUID, Person, Person_>(
            connection = cxn,
            dbMapper = SimpleDbMapper(
                schemaName = "",
                entityType = PersonType
            ),
            sqlHelper = GenericSqlHelper()
        )

        try {
            storageManager.dropSchema()
        } catch (ex: Exception) {

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

