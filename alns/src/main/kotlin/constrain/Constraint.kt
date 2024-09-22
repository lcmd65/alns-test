package com.ft.aio.template.adapter.output.web.scrippt.constrain
import com.ft.aio.template.adapter.output.web.scrippt.executor.FormulaExecutor
import kotlin.math.max

data class Constraint(
    var id: String,
    var scoreFormula: String,
    var description: String,
    var constrainType: String,
    var staffGroup: List<String>,
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
    fun caculateScore(input: MutableMap<String, Double> ):Double{
        var scores = 0.0
        if (this.covertKotlinFlag == true) {
            if (midSearch){
                for (gap in input.values) {
                    scores += this.defaultValue - (gap - this.threshold) * this.step
                }
                scores /= input.values.size
            }
            else{
                for (gap in input.values) {
                    scores = max(scores, this.defaultValue - (gap - this.threshold) * this.step)
                }
            }
            for (gap in input.values) {
                scores = this.defaultValue - (gap - this.threshold) * this.step
            }
        }
        else {
            midSearch = true
            var output = FormulaExecutor().executorKotlin(this.scoreFormula, input)
            if (midSearch){
                for (value in output.values){
                    scores -= 100 - value
                }
            }
            else {
                scores = output.values.max()
            }
        }
        return scores
    }
}