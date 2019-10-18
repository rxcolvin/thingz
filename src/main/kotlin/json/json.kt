package json

import kotlin.NumberFormatException

typealias JsonMap = Map<String, Any?>

typealias JsonList = List<Any?>


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
    fun isCloseMap(): Boolean
    fun openList()
    fun closeList()
    fun isCloseList(): Boolean
    fun key(): String
    fun atomicValue(): Any?
    fun valueType(): JsonValueType
    fun value(): Any?
    fun listSeperator()
    fun mapPropertySeparator()
    fun mapEntrySeparator()
    fun exception(msg: String): Nothing

}

interface TokenSerializer {
    fun openMap()
    fun closeMap()
    fun openList()
    fun closeList()
    fun key(name: String)
    fun atomicValue(value: Any?)
    fun listSeperator()
    fun mapPropertySeparator()
    fun mapEntrySeparator()
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
            is CharSequence -> quotize(value)
            is Number -> builder.append(value.toString())
            is Boolean -> builder.append(if (value) "true" else "false")
            null -> builder.append("null")
            else -> throw Exception("Not Atomic JSON Type")
        }
    }

    override fun listSeperator() {
        builder.append(", ")
    }

    override fun mapPropertySeparator() {
        builder.append(":")
    }


    override fun mapEntrySeparator() {
        builder.append(", ")
    }

    override fun asString(): String = builder.toString()
}

class JsonTokenDeserializer(
    private val input: CharSequence,
    private val allowPlusSign:Boolean = false
) : TokenDeserializer {

    private var pos: Int = 0
    private var line: Int = 1
    private var col = 1

    private val head: Char get() = input[pos]

    override fun openMap() {
        skipWs()
        if (head != '{') exception("Expecting {")
        next()
    }

    override fun closeMap() {
        skipWs()
        if (head != '}') exception("Expecting }")
        next()
    }

    override fun isCloseMap(): Boolean {
        skipWs()
        return head == '}'
    }

    override fun openList() {
        skipWs()
        if (head != '[') exception("Expecting [")
        next()
    }

    override fun closeList() {
        skipWs()
        if (head != ']') exception("Expecting ]")
        next()
    }

    override fun isCloseList(): Boolean {
        skipWs()
        return head == ']'
    }

    override fun key(): String {
        skipWs()
        return quotedString()
    }

    override fun atomicValue(): Any? {
        skipWs()
        return when (head) {
            '"' -> quotedString()
            '+', '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> number()
            't', 'f' -> boolean()
            'n' -> parseNull()
            else -> exception("Expected an String, Number, true|false or null")
        }
    }

    override fun valueType(): JsonValueType {
        skipWs()
        return when (head) {
            '"' -> JsonValueType.STRING
            '+', '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> JsonValueType.NUMBER
            't', 'f' -> JsonValueType.BOOLEAN
            'n' -> JsonValueType.NULL
            '{' -> JsonValueType.MAP
            '[' -> JsonValueType.LIST
            else -> exception("Expected one of  \", +, -, { , [ or a digit")
        }
    }

    override fun value(): Any? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listSeperator() {
        match(",")
    }

    override fun mapPropertySeparator() {
        match(":")
    }

    override fun mapEntrySeparator() {
        match(",")
    }

    fun skipWs() {
        while (head.isWhitespace()) {
            if (head == '\n' || head == '\r') {
                line++
                col = 0
            }
            next()
        }
    }

    fun next() {
        if (pos >= input.length) exception("Unexpected EOL")
        pos++
        col++
    }

    fun quotedString(): String {
        if (head != '"') exception("Expected \"")
        next()
        val sb = StringBuilder()
        var escaped = false
        loop@ while (true) {
            if (head == '\n' || head == '\r') {
                exception("Line Break not allowed here")
            }
            if (!escaped) {
                when (head) {
                    '\\' -> escaped = true
                    '"' -> break@loop
                    else -> sb.append(head)
                }
            } else {
                when (head) {
                    'n' -> sb.append("\n")
                    '"' -> sb.append('"')
                    'b' -> sb.append("\b")
                    't' -> sb.append("\t")
                    'r' -> sb.append("\r")
                    '\\' -> sb.append("\\")
                    else -> sb.append("\\" + head)
                }
                escaped = false
            }
            next()
        }
        next()
        return sb.toString()
    }

    fun number(): Number {
        skipWs()
        val sb = StringBuilder()
        var hasDP = false
        var first = true

        while (true) {
            if (first) {
                if (head == '-') {
                    sb.append('-')
                    next()
                } else if (head == '+' && !allowPlusSign) {
                    exception("Numbers can't start with '+'" )
                    next()
                }
            } else {
                first = false
            }
            if (head.isDigit()) {
                sb.append(head)
                next()
            } else if (head == '.') {
                if (hasDP) throw NumberFormatException()
                else {
                    hasDP = true
                    sb.append(head)
                    next()
                }
            } else {
                break
            }
        }
        return if (hasDP) {
            sb.toString().toDouble()
        }  else {
            sb.toString().toLong()
        }
    }

    fun boolean(): Boolean {
        fun bad(): Nothing = exception("Expecting 'true' or 'false'")

        val ret: Boolean = when (head) {
            't' -> true
            'f' -> false
            else -> bad()
        }
        next()
        if (ret) {
            match("rue")
        } else {
            match("alse")
        }
        return ret
    }

    fun parseNull(): Any? {
        match("null")
        return null
    }

    fun match(s: String) {
        skipWs()
        s.forEach {
            if (head != it) exception("Expecting '$s'")
            next()
        }
    }

    override fun exception(msg: String): Nothing {
        throw Exception("Parsing Error at ($line:$col [$pos]) - $msg")
    }
}


fun jsonMapParser(json: String): JsonMap {
    return mapParser(JsonTokenDeserializer(json))
}

fun listParser(deserializer: TokenDeserializer): List<Any?> {
    val builder = ArrayList<Any?>()

    with(deserializer) {
        openList()
        var first = true
        while (!isCloseList()) {
            if (!first) {
                listSeperator()
            } else {
                first = false
            }
            builder.add(
                valueParser(this)
            )
        }
        closeList()

    }
    return builder.toList()
}

fun <T : TokenDeserializer> mapParser(deserializer: T): JsonMap {
    val builder: MutableMap<String, Any?> = LinkedHashMap()

    with(deserializer) {
        openMap()
        var first = true
        while (!isCloseMap()) {
            if (!first) {
                mapEntrySeparator()
            } else {
                first = false
            }
            val key = key()
            if (builder.contains(key)) {
                exception("Map entry with key $key already exists")
            }
            mapPropertySeparator()
            builder.put(
                key,
                valueParser(this)
            )
        }
        closeMap()
    }
    return builder.toMap()
}

fun <T : TokenDeserializer> valueParser(deserializer: T): Any? =
    with(deserializer) {
        when (deserializer.valueType()) {
            JsonValueType.STRING, JsonValueType.NUMBER, JsonValueType.BOOLEAN, JsonValueType.NULL -> deserializer.atomicValue()
            JsonValueType.MAP -> mapParser(this)
            JsonValueType.LIST -> listParser(this)
        }
    }

fun jsonMapUnparser(m: JsonMap): String {
    val s = JsonTokenSerializer()
    mapUnparser(s, m)
    return s.asString()
}

fun <T : TokenSerializer> mapUnparser(t: T, m: JsonMap) {
    with(t) {
        openMap()
        var first = true
        m.entries.forEach {
            if (!first) {
                mapEntrySeparator()
            } else {
                first = false
            }
            key(it.key)
            mapPropertySeparator()
            when (it.value) {
                null -> atomicValue(null)
                is Number, is Boolean, is String -> atomicValue(it.value)
                is List<Any?> -> listUnparser(this, it.value as List<Any?>)
                is Map<*, *> -> mapUnparser(this, it.value as JsonMap)
                else -> throw Exception("${it.value!!::class.qualifiedName} is not a JSON type")
            }
        }
        closeMap()
    }
}

fun <T : TokenSerializer> listUnparser(t: T, list: List<Any?>) {
    with(t) {
        openList()
        var first = true
        list.forEach {
            if (!first) {
                mapEntrySeparator()
            } else {
                first = false
            }
            when (it) {
                null -> atomicValue(null)
                is Number, is Boolean, is String -> atomicValue(it)
                is List<Any?> -> listUnparser(this, it as List<Any?>)
                is Map<*, *> -> mapUnparser(this, it as JsonMap)
            }
        }
        closeList()
    }
}





