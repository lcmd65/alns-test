package com.ft.aio.template.adapter.output.web.scrippt.executor


import kotlin.math.max
import kotlin.math.abs

class FormulaExecutor {

    fun executorKotlin(formulaString: String, input: MutableMap<String, Double>): MutableMap<String, Double> {

        var values = extractValues(formulaString)
        var defaultValues = values[0]
        var threshold = values[1]
        var step = values[2]

        var map: MutableMap<String, Double> = mutableMapOf()
        for ((key, gap) in input){
            map.set(key, max(0.0,defaultValues - (abs(gap - threshold)) * step))
        }
        return map
    }

    fun extractValues(input: String): List<Double> {
        var regex = """\b\d+\.\d+|\b\d+""".toRegex()

        return regex.findAll(input)
            .map { it.value.toDouble() }
            .toList()
    }
}