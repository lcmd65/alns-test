package com.ft.aio.template.adapter.output.web.scrippt.objective

import com.ft.aio.template.adapter.output.web.scrippt.executor.FormulaExecutor
import kotlin.math.max

data class Constrain(
    var id: String,
    var scoreFormula: String,
    var description: String,
    var constrainType: String,
    var priority: Int,
    var toMaximize: Boolean,
    var isHard: Boolean,
    var defaultValue: Double = 100.0,
    var threshold: Double = 0.0,
    var step: Double = 0.0,
    var score: Double = 0.0,
    var covertKotlinFlag: Boolean = false,
    var midSearch: Boolean = true
){
    fun caculateScore(input: MutableMap<String, Double> ){
        if (covertKotlinFlag == true) {
            if (midSearch){
                for (gap in input.values) {
                    this.score += this.defaultValue - (gap - this.threshold) * this.step
                }
                this.score /= input.values.size
            }
            else{
                for (gap in input.values) {
                    this.score = max(this.score, this.defaultValue - (gap - this.threshold) * this.step)
                }
            }
            for (gap in input.values) {
                this.score = this.defaultValue - (gap - this.threshold) * this.step
            }
        }
        else {
            var output = FormulaExecutor().executorKotlin(this.scoreFormula, input)
            if (midSearch){
                for (value in output.values){
                    this.score += value
                }
                this.score /= output.values.size
            }
            else {
                this.score = output.values.max()
            }
        }
    }
}