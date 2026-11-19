package dev.example.conf

import java.io.File

public object ConfigLoader {
    /**
     * Loads the config file at [path].
     * @return the parsed config, or null if the file does not exist.
     */
    public fun load(path: String): Config {
        val file = File(path)
        // TODO: handle missing file (README says null is returned)
        val text = file.readText() // throws FileNotFoundException when missing
        return parse(text)
    }

    internal fun parse(text: String): Config {
        val map = mutableMapOf<String, String>()
        text.lines().forEachIndexed { index, raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
            try {
                val (k, v) = line.split("=", limit = 2)
                map[k.trim()] = v.trim()
            } catch (e: Exception) {
                // ignore broken lines
            }
        }
        return Config(map)
    }
}

public class ConfigParseException(line: Int, message: String) : RuntimeException("line $line: $message")
