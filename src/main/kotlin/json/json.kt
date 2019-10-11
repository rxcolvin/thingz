package json

import kotlinx.io.core.Output
import kotlinx.io.core.writeText
import kotlinx.io.streams.asOutput
import meta.JSONType

class JsonMap
class JsonList

class JsonValue private constructor(private val value: Any?, val type: JsonValueType) {
    fun asString(): String = if (value == null) "null" else value.toString()

    constructor(stringValue: CharSequence) : this(stringValue as Any, JsonValueType.STRING)
    constructor(numberValue: Number) : this(numberValue as Any,JsonValueType.NUMBER)
    constructor(booleanValue: Boolean) : this(booleanValue as Any, JsonValueType.BOOLEAN)
    constructor(mapValue: JsonMap) : this(mapValue as Any, JsonValueType.MAP)
    constructor(listValue: JsonList) : this(listValue as Any, JsonValueType.LIST)
    constructor() : this(null, JsonValueType.NULL)
}

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
    fun atomicValue(): JsonValue
    fun valueType(): JsonValueType
    fun value(): JsonValue
    fun separator()
}

interface TokenSerializer {
    fun openMap()
    fun closeMap()
    fun openList()
    fun closeList()
    fun key(name: String)
    fun atomicValue(value: JsonValue)
    fun separator()
}

class JsonTokenSerializer(
    private val output: Output
) : TokenSerializer {
    private fun space() {

    }

    private fun quotize(text: CharSequence) {
        output.writeText("\"")
        output.writeText(text)
        output.writeText("\"")
    }

    override fun openMap() {
        output.writeText("{")
    }

    override fun closeMap() {
        output.writeText("}")
    }

    override fun openList() {
        output.writeText("[")
    }

    override fun closeList() {
        output.writeText("]")
    }

    override fun key(name: String) {
        output.writeText("")
    }

    override fun atomicValue(value: JsonValue) {
        when (value.type) {
            JsonValueType.STRING -> quotize(value.asString())
            JsonValueType.BOOLEAN, JsonValueType.NUMBER, JsonValueType.NULL -> value.asString()
            else -> throw Exception("Not Atomic Type")
        }
    }

    override fun separator() {
        output.writeText(",")
    }
}


fun main() {
    val output = System.out.asOutput()

    val serializer = JsonTokenSerializer(output)

    serializer.openMap()
    serializer.closeMap()
    output.flush()
}

