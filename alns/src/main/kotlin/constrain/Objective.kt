package com.ft.aio.template.adapter.output.web.scrippt.constrain

data class Objective(
    val id: String,
    val scoreFormula: String,
    val description: String,
    val objectiveType: String,
    val priority: Int,
    val toMaximize: Boolean,
    val isHard: Boolean
)
