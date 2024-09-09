package com.ft.aio.template.adapter.output.web.scrippt.coverage

data class HorizontalCoverage (
    var shifts: MutableList<String>,
    var days: MutableList<Int>,
    var type: MutableList<String>,
    var desireValue: Int = 0,
    var penalty: Int = 0,
    var id: Int = 0
)