package io.github.fuga.csv

public object CsvParser {
    public fun parseLine(line: String): List<String> = line.split(',')
}
