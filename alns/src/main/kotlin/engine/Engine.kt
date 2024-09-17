package com.ft.aio.template.adapter.output.web.scrippt.engine
import com.ft.aio.template.adapter.output.web.scrippt.input.InputData

class Engine(var data: InputData) {

    var anls = Alns(data)
    var schedule: MutableMap<String, MutableMap<Int, String>> = mutableMapOf()
    var numberViolation: Int = 0
}