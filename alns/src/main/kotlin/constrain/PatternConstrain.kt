package com.ft.aio.template.adapter.output.web.scrippt.constrain

import com.ft.aio.template.adapter.output.web.scrippt.shift.Shift

data class PatternConstrain (
    var id: String,
    var description: String,
    var constrainType: String,
    var shiftPatterns: List<String>,
    var staffGroup: List<String>,
    var priority: Int,
    var exist: Boolean,
    var isHard: Boolean,
    var pelnalty: Int,
    var patternLists : MutableMap<String, MutableList<String>> = mutableMapOf()
) {
    fun parsingPattern() {
        patternLists = mutableMapOf()
        for(pattern in  shiftPatterns){
            var pair = parseAllPairs(pattern)
            if (patternLists == null){
                patternLists.set(0.toString(), pair)
            }
            else {
                patternLists.set((patternLists.size + 1).toString(), pair)
            }
        }
    }

    private fun parseAllPairs(input: String): MutableList<String> {
        val regex = Regex("""([A-Z]{2}|\b[A-Z]\d\b)""")
        return regex.findAll(input)
            .map { it.value }
            .toMutableList()
    }
}