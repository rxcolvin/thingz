package meta

class FieldMapper<X:Any, T : Type<X>, E : Any, E_ : Any>(
    val field: Field<X, T>,
    val getter: (E) -> X,
    val getter_: (E_) -> X,
    val setter_: (E_, X) -> Unit
)

interface Builder<E> {
    fun create(): E
}

class EntityMapper<E : Any, E_ : Builder<E>>(
    val fieldMappers: List<FieldMapper<*, *, E, E_>>,
    val builder: () -> E_
)


class Foo(
    val name: String
)

class Foo_  (
    var name: String = ""
) : Builder<Foo> {
    override fun create() = Foo(name)
}


object FooType {
    val name = Field("name", StringType)
    val type = EntityType<Foo>(
        typeName = "Foo",
        fields = listOf(name)
    )
}

val fooMapper = EntityMapper<Foo, Foo_>(
    fieldMappers = listOf(
        FieldMapper<String, StringType, Foo, Foo_>(
            field = FooType.name,
            getter = {it.name},
            getter_ = {it.name},
            setter_ = {item, it -> item.name = it}
        )
    ),
    builder = {Foo_()}
)