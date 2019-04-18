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



