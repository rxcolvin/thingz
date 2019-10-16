package json

import java.lang.NumberFormatException


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
    fun listSeperator()
    fun mapSeparator()
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

class JsonTokenDeserializer(
    private val input: CharSequence
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
            '[' -> JsonValueType.MAP
            else -> exception("Expected one of  \", +, -, { , [ or a digit")
        }
    }

    override fun value(): Any? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listSeperator() {
         match(",")
    }

    override fun mapSeparator() {
        match(":")
    }

    fun skipWs() {
        while (head.isWhitespace()) {
            if (head == '\n' || head == '\r') {
                line++
                col=0
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
        val sb = StringBuilder()
        var hasDP = false
        val sign = when (head) {
            '+' -> {
                next(); 1
            }
            '-' -> {
                next(); -1
            }
            else -> 1
        }
        while (true) {
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
        return if (hasDP) sb.toString().toDouble() * sign else sb.toString().toInt() * sign
    }

    fun boolean() : Boolean {
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

    fun parseNull() : Any? {
        match("null")
        return null
    }

    fun match(s: String ) {
        skipWs()
        s.forEach {
            if (head != it) exception("Expecting '$s'")
            next()
        }
    }

    fun exception(msg: String) : Nothing {
        throw Exception("Parsing Error at ($line:$col [$pos]) - $msg")
    }

}



