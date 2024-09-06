package com.ft.aio.template.adapter.output.web.scrippt.objective

data class Constrain(
    val code: String,
    val scoreFormula: String,
    val description: String,
    val objectiveType: String,
    val priority: Int,
    val toMaximize: Boolean,
    val isHard: Boolean
)