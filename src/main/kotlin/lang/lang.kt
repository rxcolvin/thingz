package lang

data class Email(
    val name: String,
    val domain: String
)

data class JSON(
    val json: String
)


interface Tuple {
  val arity: Int
}

data class T1<X>(
    val v1: X
) : Tuple {
  override val arity = 1
}

data class T2<X1, X2>(
    val v1: X1,
    val v2: X2
) : Tuple {
  override val arity = 2
}


interface FieldGetter<T> {
  fun getter(name: String) : (T) -> Any?

}

interface FieldSetter<T> {
   fun setter(name: String) : (T, Any?) -> Unit
}

object MapFieldGetter : FieldGetter<Map<String, Any?>> {
  override fun getter(name: String): (Map<String, Any?>) -> Any? = { t: Map<String, Any?> ->
    t.get(name)
  }
}

object MapFieldSetter : FieldSetter<MutableMap<String, Any?>> {
  override fun setter(name: String): (MutableMap<String, Any?>, Any?) -> Unit = { t: MutableMap<String, Any?>, v: Any? ->
    t.put(name, v)
  }
}



