package com.ft.aio.template.adapter.output.web.scrippt.objective

data class patternConstrain (
    var id: String,
    var description: String,
    var constrainType: String,
    var shiftPatterns: List<String>,
    var staffGroup: List<String>,
    var priority: Int,
    var exist: Boolean,
    var isHard: Boolean,
    var pelnalty: Int,
    var patternLists : List<String>
) {
    fun parsingPattern(shift: List<String>, shiftGroup: List<String>){
        shiftPatterns.forEach {
            var pairs = parseAllPairs(it)
            var pattern : List<String> = listOf()
            for (item in pairs){
                if (item in shiftGroup){
                    for (item2 in pattern){

                    }
                }
            }
        }
    }

    fun parseAllPairs(input: String): List<String> {
        val regex = Regex("""[A-Z]{2}""")
        return regex.findAll(input)
            .map { it.value }
            .toList()
    }
}