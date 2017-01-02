
package ca.adamerb.sqlgen

import ca.adamerb.sqlgen.TokenType.*
import java.util.*

data class CliArg(val name: String, val description: String)
data class CliOpt(val short: String, val long: String, val description: String, val requiresArg: Boolean = false) {
    val helpFormat = "[-$short|--$long]"
}
data class ParsedCli(val args: Map<String, String>, val opts: Map<String, String>)

enum class TokenType { LongOpt, ShortOpt, Arg }

private fun tokenType(token: String): TokenType =
        when {
            token.startsWith("--") -> LongOpt
            token.startsWith("-") -> ShortOpt
            else -> Arg
        }

private val leadingDashes = Regex("^-+")

class CliParser(val argDefs: List<CliArg>, val optDefs: List<CliOpt>) {
    fun parse(input: Array<String>): ParsedCli {
        var argIndex = -1

        var i = 0
        val opts = HashMap<String, String>()
        val args = HashMap<String, String>()
        while (i < input.size) {
            val token = input[i]
            val name = token.replace(leadingDashes, "")
            val type = tokenType(token)
            if (type != Arg) {
                val optDef =
                    optDefs.firstOrNull {
                        if(type == LongOpt) it.long == name
                        else it.short == name
                    } ?: throw IllegalArgumentException("Unknown option $token")

                val optArg =
                    if(optDef.requiresArg) {
                        i++
                        input.getOrNull(i)
                            ?.let { if(tokenType(it) == Arg) it else null }
                            ?: throw IllegalArgumentException("Option ${optDef.long} requires argument.")
                    } else ""

                opts[optDef.long] = optArg
            } else {
                argIndex++
                val argDef = requireNotNull(argDefs.getOrNull(argIndex)) { "Unknown argument $token" }
                args[argDef.name] = token
            }
            i++
        }

        if(args.size < argDefs.size) throw IllegalArgumentException("Missing required argument ${argDefs[args.size].name}")

        return ParsedCli(args, opts)
    }
}

