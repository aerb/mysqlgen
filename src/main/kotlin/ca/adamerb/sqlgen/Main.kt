package ca.adamerb.sqlgen

import java.sql.DriverManager


private val parser = CliParser(
    listOf(
        CliArg(name = "database", description = "The database to generate statements from.")
    ),
    listOf(
        CliOpt(short = "u", long = "user", description = "The user to access mysql with.", requiresArg = true),
        CliOpt(short = "p", long = "password", description = "The password for the mysql user.", requiresArg = true),
        CliOpt(short = "c", long = "columns", description = "Explicitly declare columns for INSERT statements."),
        CliOpt(short = "dup", long = "update-on-duplicate", description = "Add an UPDATE ON DUPLICATE clause to INSERT statements."),
        CliOpt(short = "q", long = "quotes", description = "Add an quotes to column and table names."),
        CliOpt(short = "h", long = "help", description = "Prints help.")
    )
)

fun printUsage() {
    val usage = buildString {
        append("mysqlgen")
        parser.argDefs.forEach { append(" ").append(it.name.toUpperCase()) }
        parser.optDefs.forEach { append(" ").append(it.helpFormat) }
        appendln()
        appendln()
        appendln("Arguments")
        parser.argDefs.forEach {
            append("    ").append(it.name).append(": ").append(it.description).appendln()
        }
        appendln()
        appendln("Options")
        parser.optDefs.forEach {
            append("    ").append(it.helpFormat).append(" ").append(it.description).appendln()
        }
    }
    println(usage)
}

fun main(args: Array<String>) {
    try {
        run(args)
    } catch (e: IllegalArgumentException) {
        println(e.message)
        println()
        printUsage()
    }
}

fun run(inputs: Array<String>) {
    val (args, opts) = parser.parse(inputs)

    val database = args["database"]!!
    val user = opts["user"]
    val pass = opts["password"]
    val columnNames = "columns" in opts
    val updateOnDuplicate = "update-on-duplicate" in opts
    val quotes = "quotes" in opts
    val help = "help" in opts
    if(help) {
        printUsage()
        return
    }

    val meta = DriverManager.getConnection("jdbc:mysql://localhost/$database?useSSL=false", user, pass).use { conn ->
        conn.metaData.getColumns(null, null, null, null).asList { it.asMap() }
    }

    fun optionalQuote(name: String): String =
        if(quotes) "`$name`"
        else name

    val columnsByTable = meta.groupBy(keySelector = { it["TABLE_NAME"]!! }, valueTransform = { it["COLUMN_NAME"]!! })
    columnsByTable.forEach {
        val sql = buildString {
            append("INSERT INTO ${optionalQuote(it.key)}")

            if(columnNames) {
                appendln(" (")
                it.value.joinTo(buffer = this, separator = ",\n", postfix = "\n") {"    ${optionalQuote(it)}"}
                appendln(")")
            } else {
                appendln()
            }
            appendln("VALUES (")
            it.value.joinTo(buffer = this, separator = ",\n", postfix = "\n") {"    :$it"}
            appendln(")")
            if(updateOnDuplicate) {
                appendln("ON DUPLICATE KEY UPDATE")
                it.value.joinTo(buffer = this, separator = ",\n", postfix = "\n") { "    ${optionalQuote(it)} = :$it" }
            }
        }
        println(sql)
    }

}



