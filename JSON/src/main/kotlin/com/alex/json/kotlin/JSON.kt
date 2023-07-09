package com.alex.json.kotlin

import java.io.*
import java.util.*

sealed class JSON {
    abstract fun isObject():Boolean
    abstract fun isArray():Boolean
    abstract fun json(writer: Writer, spaces: Int):Writer
    abstract fun json(writer: Writer, spaces: Int, depth: Int):Writer
    abstract fun json(spaces: Int):String
    abstract fun json(json: File, spaces: Int):File
    companion object{
        private fun escape(s: String): String {
            return s.replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
        }

        private fun json(writer: Writer, value: Any?, spaces: Int, depth: Int){
            when(value){
                is JSON ->value.json(writer, spaces, depth)
                is Number->writer.write(value.toString())
                is Boolean->writer.write(value.toString())
                is Map<*,*>-> Object(filter(value,String::class.java)).json(writer, spaces, depth)
                is Collection<*>-> Array(value).json(writer, spaces, depth)
                is kotlin.Array<*>-> Array(value).json(writer, spaces, depth)
                is IntArray-> Array(value.toList()).json(writer, spaces, depth)
                is LongArray-> Array(value.toList()).json(writer, spaces, depth)
                is FloatArray-> Array(value.toList()).json(writer, spaces, depth)
                is DoubleArray-> Array(value.toList()).json(writer, spaces, depth)
                is BooleanArray-> Array(value.toList()).json(writer, spaces, depth)
                is ShortArray-> Array(value.toList()).json(writer, spaces, depth)
                is CharArray-> Array(value.toList()).json(writer, spaces, depth)
                is ByteArray-> Array(value.toList()).json(writer, spaces, depth)
                is Any->{
                    writer.write('\"'.code)
                    writer.write(escape(value.toString()))
                    writer.write('\"'.code)
                }
                else->writer.write("null")
            }
        }
        fun <E> filter(list: List<*>?, c: Class<E>): List<E?>? {
            return list?.filter { obj-> c.isInstance(obj) }?.map { obj->c.cast(obj) }
        }

        fun <T> filter(map: Map<*, *>?, c: Class<T>): Map<String?, T?>? {
            return filter(map, String::class.java, c)
        }

        fun <K, V> filter(map: Map<*, *>?, k: Class<K>, v: Class<V>): Map<K?, V?>? {
            return map?.filter { e -> k.isInstance(e.key) && v.isInstance(e.value) }
                ?.entries?.associateBy({e->k.cast(e.key)},{e->v.cast(e.value)})
        }

        @Throws(IOException::class)
        fun json(json: String): JSON {
            return json(StringReader(json))
        }

        @Throws(IOException::class)
        fun json(json: File): JSON {
            return json(FileReader(json))
        }

        @Throws(IOException::class)
        fun json(reader: Reader): JSON {
            return json(reader, null)
        }

        @Throws(IOException::class)
        private fun json(reader: Reader, o: JSON?): JSON {
            var o = o
            var c: Char
            var rs = false
            var rv = o is Array<*>
            var vs = false
            val buffer = StringBuilder()
            var key: String? = null
            while (reader.read().toChar().also { c = it } != (-1).toChar()) {
                when (c) {
                    '{' -> {
                        if (!rs) {
                            when(o){
                                is Object ->{o.put(key, json(reader, Object())); key = null}
                                is Array<*> ->{(o as Array<Any>).add(json(reader, Object()))}
                                else ->{o = Object()
                                }
                            }
                        }else{
                            buffer.append(c);
                        }
                    }

                    '[' -> {
                        if (!rs) {
                            when(o){
                                is Object ->{o.put(key, json(reader, Array<Any>())); key = null}
                                is Array<*> ->{(o as Array<Any>).add(json(reader, Array<Any>()))}
                                else ->{o = Array<Any>(); rv=true}
                            }
                        }else{
                            buffer.append(c);
                        }
                    }

                    ':' -> {
                        if (!rs) {
                            buffer.clear()
                            rv = true
                            vs = false
                        } else {
                            buffer.append(c)
                        }
                    }

                    '"' -> {
                        if (key == null && o is Object) {
                            rs = !rs
                            if (!rs) {
                                key = buffer.toString()
                            }
                        } else {
                            if (rv) {
                                rs = !rs
                                vs = true
                            }
                        }
                    }

                    '\\' -> {
                        buffer.append(
                            when (reader.read().toChar()) {
                                '\"' -> '"'
                                '\\' -> '\\'
                                't' -> '\t'
                                'b' -> '\b'
                                'n' -> '\n'
                                'r' -> '\r'
                                else -> c
                            }
                        )
                    }
                    ',' -> {
                        if (rs) {
                            buffer.append(c)
                        } else {
                            if (buffer.isNotEmpty() || vs) {
                                val value:Any?=if(vs) buffer.toString() else parse_primitive(buffer.toString())
                                when(o){
                                    is Object ->{o.put(key, value)}
                                    is Array<*> ->{(o as Array<Any>).add(value)}
                                    else->{}
                                }
                                rv = o is Array<*>
                                vs = false
                                key = null
                                buffer.clear()
                            }
                        }
                    }

                    '}', ']' -> {
                        if (rs) {
                            buffer.append(c)
                        } else {
                            if (buffer.isNotEmpty() || vs) {
                                val value:Any?=if(vs) buffer.toString() else parse_primitive(buffer.toString())
                                when(o){
                                    is Object ->{o.put(key, value)}
                                    is Array<*> ->{(o as Array<Any>).add(value)}
                                    else->{}
                                }
                            }
                            return o!!
                        }
                    }

                    '\n', '\t', ' ', '\r' -> {
                        if (rs) {
                            buffer.append(c)
                        }
                    }

                    else -> {
                        buffer.append(c)
                    }
                }
            }
            throw IllegalArgumentException("No ending tag found")
        }
        private fun parse_primitive(str:String):Any?{
            return when (str) {
                "null" -> null
                "false" -> false
                "true" -> true
                else -> {
                    val d=str.toDouble()
                    val l=d.toLong()
                    return if(d==l.toDouble()){
                        l
                    }else{
                        d
                    }
                }
            }
        }
    }

    class Object() : JSON(), MutableMap<String?, Any?> {
        val map=TreeMap<String?,Any?>()
        constructor(map: Map<String?, *>?) : this() {
            map?.let { putAll(it) }
        }

        override fun isObject()=true
        override fun isArray()=false
        override val size: Int
            get() = map.size
        override val entries: MutableSet<MutableMap.MutableEntry<String?, Any?>>
            get() = map.entries
        override val keys: MutableSet<String?>
            get() = map.keys
        override val values: MutableCollection<Any?>
            get() = map.values

        override fun clear()=map.clear()
        override fun isEmpty()=map.isEmpty()
        override fun remove(key: String?)=map.remove(key)
        override fun get(key: String?)=map[key]
        override fun containsValue(value: Any?)=map.containsValue(value)
        override fun containsKey(key: String?)=map.containsKey(key)
        override fun put(key: String?, value: Any?): Object {
            if (key != null) {
                if (value != null) {
                    map[key] = value
                } else {
                    map.remove(key)
                }
            }
            return this
        }
        override fun putAll(from: Map<out String?, *>) {
            for ((key, value) in from) {
                put(key, value)
            }
        }

        operator fun get(key: String?, def: Any?): Any {
            val value = get(key)
            return (value ?: def)!!
        }

        operator fun get(key: String?, def: Object?): Object? {
            val value = get(key)
            return value as? Object ?: def
        }

        fun getObject(key: String?): Object? {
            return get(key, null as Object?)
        }

        operator fun get(key: String?, def: Array<*>?): Array<*>? {
            val value = get(key)
            return value as? Array<*> ?: def
        }

        fun getArray(key: String?): Array<*>? {
            return get(key, null as Array<*>?)
        }

        operator fun get(key: String?, def: String?): String? {
            val value = get(key)
            return value as? String ?: def
        }

        fun getString(key: String?): String? {
            return get(key, null as String?)
        }

        operator fun get(key: String?, def: Int): Int {
            val value = get(key)
            return (value as? Number)?.toInt() ?: ((value as? String)?.toInt() ?: def)
        }

        fun getInt(key: String?): Int {
            return get(key, 0)
        }

        operator fun get(key: String?, def: Long): Long {
            val value = get(key)
            return (value as? Number)?.toLong() ?: ((value as? String)?.toLong() ?: def)
        }

        fun getLong(key: String?): Long {
            return get(key, 0L)
        }

        operator fun get(key: String?, def: Double): Double {
            val value = get(key)
            return (value as? Number)?.toDouble() ?: ((value as? String)?.toDouble() ?: def)
        }

        fun getDouble(key: String?): Double {
            return get(key, 0.0)
        }

        operator fun get(key: String?, def: Float): Float {
            val value = get(key)
            return (value as? Number)?.toFloat() ?: ((value as? String)?.toFloat() ?: def)
        }

        fun getFloat(key: String?): Float {
            return get(key, 0.0f)
        }

        operator fun get(key: String?, def: Boolean): Boolean {
            val value = get(key)
            return (value as? Boolean) ?: ((value as? String)?.toBoolean() ?: def)
        }

        @Throws(IOException::class)
        override fun json(spaces: Int): String {
            return json(StringWriter(), spaces).toString()
        }

        @Throws(IOException::class)
        override fun json(json: File, spaces: Int): File {
            return json.also { json(FileWriter(it), spaces) }
        }

        @Throws(IOException::class)
        override fun json(writer: Writer, spaces: Int)=json(writer, spaces,0)
        @Throws(IOException::class)
        override fun json(writer: Writer, spaces: Int, depth: Int): Writer {
            val line = if (spaces>0) "\n"+"\t".repeat(depth*spaces) else ""
            val tab = if (spaces>0) "\n"+"\t".repeat((depth+1)*spaces) else ""
            var delimiter = false
            if(size==0){
                writer.write("{}")
            }else{
                writer.write('{'.code)
                for ((key, value) in entries) {
                    if (delimiter) {
                        writer.write(','.code)
                    }
                    writer.write(tab)
                    writer.write('"'.code)
                    writer.write(key)
                    writer.write('"'.code)
                    writer.write(':'.code)
                    json(writer,value,spaces,depth+1)
                    delimiter = true
                }
                writer.write(line)
                writer.write('}'.code)
            }
            return writer
        }

        override fun toString(): String {
            return json(0)
        }

        fun <T> filter(c: Class<T>): Map<String?, T?>? {
            return filter(this, String::class.java, c)
        }

        companion object {
            @Throws(IOException::class)
            fun create(json: String): Object {
                return create(StringReader(json))
            }

            @Throws(IOException::class)
            fun create(json: File): Object {
                return create(FileReader(json))
            }

            @Throws(IOException::class)
            fun create(reader: Reader): Object {
                return json(reader) as Object
            }
        }
    }

    class Array<E>() : JSON(), MutableList<E?> {
        private val list=LinkedList<E?>()
        constructor(collection: Collection<E>?) : this() {
            collection?.let { addAll(it) }
        }
        override fun isObject()=false
        override fun isArray()=true
        constructor(array: kotlin.Array<E>?) : this(array?.toList())

        override val size: Int
            get() = list.size

        override fun clear()=list.clear()

        override fun addAll(elements: Collection<E?>)=list.addAll(elements)

        override fun addAll(index: Int, elements: Collection<E?>)=list.addAll(index,elements)

        override fun add(index: Int, element: E?)=list.add(index,element)

        override fun add(element: E?)=list.add(element)

        override fun get(index: Int)=list[index]

        override fun isEmpty()=list.isEmpty()

        override fun iterator()=list.iterator()

        override fun listIterator()=list.listIterator()

        override fun listIterator(index: Int)=list.listIterator()
        override fun removeAt(index: Int)=list.removeAt(index)

        override fun set(index: Int, element: E?)=list.set(index,element)

        override fun retainAll(elements: Collection<E?>)=list.retainAll(elements.toSet())

        override fun removeAll(elements: Collection<E?>)=list.removeAll(elements.toSet())

        override fun remove(element: E?)=list.remove(element)

        override fun subList(fromIndex: Int, toIndex: Int)=list.subList(fromIndex,toIndex)

        override fun lastIndexOf(element: E?)=list.lastIndexOf(element)

        override fun indexOf(element: E?)=list.indexOf(element)

        override fun containsAll(elements: Collection<E?>)=list.containsAll(elements)

        override fun contains(element: E?)=list.contains(element)

        fun put(obj: E?): Array<E> {
            list.add(obj)
            return this
        }

        operator fun get(pos: Int, def: Any?): Any? {
            val value: Any? = get(pos)
            return value ?: def
        }

        operator fun get(pos: Int, def: Object?): Object? {
            val value: Any? = get(pos)
            return value as? Object ?: def
        }

        fun getObject(pos: Int): Object? {
            return get(pos, null as Object?)
        }

        operator fun get(pos: Int, def: Array<*>?): Array<*>? {
            val value: Any? = get(pos)
            return value as? Array<*> ?: def
        }

        fun getArray(pos: Int): Array<*>? {
            return get(pos, null as Array<*>?)
        }

        operator fun get(pos: Int, def: String?): String? {
            val value: Any? = get(pos)
            return value as? String ?: def
        }

        fun getString(pos: Int): String? {
            return get(pos, null as String?)
        }

        operator fun get(pos: Int, def: Int): Int {
            val value: Any? = get(pos)
            return (value as? Number)?.toInt() ?: ((value as? String)?.toInt() ?: def)
        }

        fun getInt(pos: Int): Int {
            return get(pos, 0)
        }

        operator fun get(pos: Int, def: Long): Long {
            val value: Any? = get(pos)
            return (value as? Number)?.toLong() ?: ((value as? String)?.toLong() ?: def)
        }

        fun getLong(pos: Int): Long {
            return get(pos, 0L)
        }

        operator fun get(pos: Int, def: Double): Double {
            val value: Any? = get(pos)
            return (value as? Number)?.toDouble() ?: ((value as? String)?.toDouble() ?: def)
        }

        fun getDouble(pos: Int): Double {
            return get(pos, 0.0)
        }

        operator fun get(pos: Int, def: Float): Float {
            val value: Any? = get(pos)
            return (value as? Number)?.toFloat() ?: ((value as? String)?.toFloat() ?: def)
        }

        fun getFloat(pos: Int): Float {
            return get(pos, 0.0f)
        }

        operator fun get(pos: Int, def: Boolean): Boolean {
            val value: Any? = get(pos)
            return (value as? Boolean) ?: ((value as? String)?.toBoolean() ?: def)
        }

        @Throws(IOException::class)
        override fun json(spaces: Int): String {
            return json(StringWriter(), spaces).toString()
        }

        @Throws(IOException::class)
        override fun json(json: File, spaces: Int): File {
            return json.also { json(FileWriter(it), spaces) }
        }

        @Throws(IOException::class)
        override fun json(writer: Writer, spaces: Int)=json(writer,spaces,0)
        @Throws(IOException::class)
        override fun json(writer: Writer, spaces: Int, depth: Int): Writer {
            val line = if (spaces>0) "\n"+"\t".repeat(depth*spaces) else ""
            val tab = if (spaces>0) "\n"+"\t".repeat((depth+1)*spaces) else ""
            var delimiter = false
            if(size==0){
                writer.write("[]")
            }else{
                writer.write('['.code)
                for (value in this) {
                    if (delimiter) {
                        writer.write(','.code)
                    }
                    writer.write(tab)
                    json(writer,value,spaces, depth+1)
                    delimiter = true
                }
                writer.write(line)
                writer.write(']'.code)
            }
            return writer
        }

        override fun toString(): String {
            return json(0)
        }

        fun join(delimiter: String?, vararg keys: String): String? {
            return delimiter?.let { it ->
                mapNotNull {
                    var v: Any? = it
                    for (key in keys) {
                        v = (v as? Object)?.get(key)
                    }
                    v as? String
                }.joinToString(it)
            }
        }

        fun <T> filter(c: Class<T>): List<T?>? {
            return filter(this, c)
        }

        companion object {
            @Throws(IOException::class)
            fun create(json: String): Array<*> {
                return create(StringReader(json))
            }

            @Throws(IOException::class)
            fun create(json: File): Array<*> {
                return create(FileReader(json))
            }

            @Throws(IOException::class)
            fun create(reader: Reader): Array<*> {
                return json(reader) as Array<*>
            }
        }
    }

}
