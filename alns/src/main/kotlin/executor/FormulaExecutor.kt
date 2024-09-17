package com.ft.aio.template.adapter.output.web.scrippt.executor

import kotlin.math.max

class FormulaExecutor {

    fun executorKotlin(formulaString: String, input: MutableMap<String, Double>): MutableMap<String, Double> {

        var values = extractValues(formulaString)
        var defaultValues = values[1]
        var threshold = values[2]
        var step = values[3]

        var map: MutableMap<String, Double> = mutableMapOf()
        for ((key, gap) in input){
            map.set(key, max(0.0,defaultValues - (gap - threshold)*step))
        }
        return map
    }

    fun extractValues(input: String): List<Int> {
        val regex = Regex("""\d+""")
        return regex.findAll(input)
            .map { it.value.toInt() }
            .toList()
    }
}