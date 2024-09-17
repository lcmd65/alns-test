package com.ft.aio.template.adapter.output.web.scrippt.executor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FormulaExecutorTest {

    @Test
    fun testExecutorKotlin() {
        val formulaExecutor = FormulaExecutor()

        val formulaString = "formula(100, 6, 200, 1)"
        val input: MutableMap<String, Double> = mutableMapOf(
            "input1" to 150.0,
            "input2" to 250.0
        )

        val expected: MutableMap<String, Double> = mutableMapOf(
            "input1" to 56.0,  // 100 - (150 - 200) * 1 = 100 - (-50) = 100 + 50 = 150
            "input2" to -44.0  // 100 - (250 - 200) * 1 = 100 - (50) = 100 - 50 = 50
        )

        val result = formulaExecutor.executorKotlin(formulaString, input)

        assertEquals(expected, result)
    }

    @Test
    fun testExtractValues() {
        val formulaExecutor = FormulaExecutor()

        val input = "formula(100, 6, 200, 1)"

        val expected = listOf(100, 6, 200, 1)

        val result = formulaExecutor.extractValues(input)
        
        assertEquals(expected, result)
    }
}
