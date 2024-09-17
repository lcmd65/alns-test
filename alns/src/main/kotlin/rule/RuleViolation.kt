package com.ft.aio.template.adapter.output.web.scrippt.rule
import com.ft.aio.template.adapter.output.web.scrippt.input.InputData
class RuleViolation(var data: InputData){

    var rules: MutableMap<Int, MutableMap<String, Int>> = mutableMapOf()

    fun calculateNumberViolation(){

    }
}
