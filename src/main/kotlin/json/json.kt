package json


class JsonMap
class JsonList



enum class JsonValueType {
    STRING {
        override fun isAtomic() = true
    },
    NUMBER {
        override fun isAtomic() = true
    },
    BOOLEAN {
        override fun isAtomic() = true
    },
    MAP {
        override fun isAtomic() = false
    },
    LIST {
        override fun isAtomic() = false
    },
    NULL {
        override fun isAtomic() = true
    };

    abstract fun isAtomic(): Boolean

}

interface TokenDeserializer {
    fun openMap()
    fun closeMap()
    fun openList()
    fun closeList()
    fun key(): String
    fun atomicValue(): Any?
    fun valueType(): JsonValueType
    fun value(): Any?
    fun separator()
}

interface TokenSerializer {
    fun openMap()
    fun closeMap()
    fun openList()
    fun closeList()
    fun key(name: String)
    fun atomicValue(value: Any?)
    fun listSeperator()
    fun mapSeparator()
    fun asString(): String
}


class JsonTokenSerializer(
) : TokenSerializer {
    private val builder = StringBuilder()

    private fun space() {

    }

    private fun quotize(text: CharSequence) {
        with(builder) {
            append('"')
            append(text)
            append('"')
        }
    }

    override fun openMap() {
        builder.append("{")
    }

    override fun closeMap() {
        builder.append("}")
    }

    override fun openList() {
        builder.append("[")
    }

    override fun closeList() {
        builder.append("]")
    }

    override fun key(name: String) {
        quotize(name)
    }

    override fun atomicValue(value: Any?) {
        when (value) {
            is String -> quotize(value)
            is Number, Boolean -> builder.append(value.toString())
            else -> throw Exception("Not Atomic JSON Type")
        }
    }

    override fun listSeperator() {
        builder.append(", ")
    }

    override fun mapSeparator() {
        builder.append(":")
    }


    override fun asString(): String = builder.toString()
}



