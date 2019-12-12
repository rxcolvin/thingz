package common.entities

import common.lang.Email
import common.lang.Uuid
import common.meta.*


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
    val uuid: Uuid,
    val name: String,
    val age: Int,
    val email: Email,
    val address: Address
)

data class Person_(
    var uuid: Uuid,
    var name: String,
    var age: Int,
    var email: Email,
    var address: Address_
) {
    constructor() : this(
        Uuid(""),
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
