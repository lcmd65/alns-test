package com.ft.aio.template.adapter.output.web.scrippt.objective

data class Constrain(
    val code: String,
    val scoreFormula: String,
    val description: String,
    val constrainType: String,
    val priority: Int,
    val toMaximize: Boolean,
    val isHard: Boolean
)