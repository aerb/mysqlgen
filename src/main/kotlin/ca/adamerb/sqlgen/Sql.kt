package ca.adamerb.sqlgen

import java.sql.Connection
import java.sql.ResultSet
import java.util.*

inline fun <T> ResultSet.asList(transform: (ResultSet) -> T): List<T> {
    try {
        val list = ArrayList<T>()
        while (next()) list += transform(this)
        return list
    } finally {
        close()
    }
}

fun ResultSet.asMap(): HashMap<String, String?> {
    val rs = this
    val map = LinkedHashMap<String, String?>()
    for(i in 1 .. rs.metaData.columnCount) {
        map[rs.metaData.getColumnName(i)] = rs.getString(i)
    }
    return map
}

inline fun <T> Connection.use(function: (Connection) -> T): T = try { function(this) } finally { close() }