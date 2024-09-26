package com.ft.aio.template.adapter.output.web.scrippt.engine

import com.ft.aio.template.adapter.output.web.scrippt.constrain.Constraint
import com.ft.aio.template.adapter.output.web.scrippt.constrain.PatternConstrain
import com.ft.aio.template.adapter.output.web.scrippt.coverage.HorizontalCoverage
import com.ft.aio.template.adapter.output.web.scrippt.staff.Staff
import com.ft.aio.template.adapter.output.web.scrippt.input.InputData
import com.ft.aio.template.adapter.output.web.scrippt.rule.RuleViolation

// Adaptive large neighborhood search
// **@author: Dat Le

import kotlin.random.Random
import kotlin.math.exp

open class Alns(var data: InputData) {
    private var numberIterations: Int = 1000
    private var temperature: Double = 1000.0
    private var alpha: Double = 0.9
    private var limit: Double = 1e-3
    private var deltaE: Double = 0.0
    var score: Double = 0.0
    var penalty: Double = Int.MAX_VALUE.toDouble()
    private var probabilitiesOfOperator: MutableMap<Int, Double> = mutableMapOf()
    private var operatorScore: MutableMap<Int, Double> = mutableMapOf()
    private var operatorWeight: MutableMap<Int, Double> = mutableMapOf()
    private var operatorTimes: MutableMap<Int, Double> = mutableMapOf()
    var bestSolution: MutableMap<String, MutableMap<Int, String>> = mutableMapOf()
    var constrainScore: Double = 0.0
    var coverageScore: Double = 0.0
    var horizontalCoverageScore: Double = 0.0
    var patternConstrainScore: Double = 0.0
    var calculate = CommonCaculate(data)
    private var rule = RuleViolation(data)

    private fun deepCopySolution(solution: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>> {
        val newSolution = mutableMapOf<String, MutableMap<Int, String>>()
        for ((key, innerMap) in solution) {
            val copiedInnerMap = innerMap.toMutableMap()
            newSolution[key] = copiedInnerMap
        }
        return newSolution
    }

    private fun createWeightOperators() {
        for (index in 0..4) {
            if (this.operatorWeight[index] != null){
                this.operatorWeight[index] = this.operatorWeight[index]!! * 0.2 + 0.8 * this.operatorScore[index]!! / this.operatorTimes[index]!!
            }
            else{
                this.operatorWeight[index] = 0.2 + 0.8 * this.operatorScore[index]!! / this.operatorTimes[index]!!
            }
        }
    }

    private fun resetWeightOperators(){
        for(index in 0..4) {
            this.operatorWeight[index] = 0.4 + 0.6 * this.operatorScore[index]!! / this.operatorTimes[index]!!
        }
    }

    private fun createScoreOperator() {
        this.operatorScore[0] = 0.2
        this.operatorScore[1] = 0.2
        this.operatorScore[2] = 0.2
        this.operatorScore[3] = 0.2
        this.operatorScore[4] = 0.2
    }

    private fun createOperatorTimes() {
        for (index in 0..4) {
            this.operatorTimes[index] = 1.0
        }
    }

    private fun scoring(){
        this.score = calculate.totalScore(this.bestSolution)
        this.constrainScore = calculate.constrainScore(this.bestSolution)
        this.horizontalCoverageScore = calculate.horizontalCoverageScore(this.bestSolution)
        this.coverageScore = calculate.coverageScore(this.bestSolution)
        this.patternConstrainScore = calculate.patternConstrainScore(this.bestSolution)
    }

    private fun calculateSimulatedAnnealing(
        currentScheduled: MutableMap<String, MutableMap<Int, String>>,
        nextScheduled:MutableMap<String, MutableMap<Int, String>>
    ): MutableMap<String, MutableMap<Int, String>> {
        deltaE = calculate.totalScore(currentScheduled) - calculate.totalScore(nextScheduled)
        if (deltaE < 0){
            return nextScheduled
        }
        else {
            if (temperature < limit) {
                return currentScheduled
            }
            val probability = exp(deltaE / temperature)
            val acceptanceVariable = Random.nextDouble(0.0, 1.0)

            temperature *= alpha
            if (probability < acceptanceVariable) {
                return nextScheduled
            }
        }
        return currentScheduled
    }

    private fun checkIfStaffInStaffGroup(
        staff: Staff,
        staffGroups: List<String>
    ): Boolean {
        var result = false
        for (staffGroupId in staffGroups) {
            val staffGroup = data.staffGroups.find { it.id == staffGroupId }

            if (staffGroup != null) {
                for (staffInfo in staffGroup.staffList) {
                    if (staff.id == staffInfo) {
                        result = true
                        break
                    }
                }
            }

            if (result) {
                break
            }
        }
        return result
    }

    private fun calculateCoverageFulfillment(
        schedules: MutableMap<String, MutableMap<Int, String>>,
        coverageId: String,
        dayId: Int,
        week: Int
    ): Int {
        val coverage = data.coverages.find { it.id == coverageId && it.day == dayId }
        var temp = 0
        if (coverage != null){
            for (staff in data.staffs){
                if (schedules[staff.id]?.get(dayId + 7*( week - 1)) in coverage.shift && checkIfStaffInStaffGroup(staff, coverage.staffGroups)) {
                    temp += 1
                }
            }
        }

        return temp
    }

    private fun calculateHorizontalCoverageFulfillment(
        schedules: MutableMap<String, MutableMap<Int, String>>,
        coverageId: Int
    ): MutableMap<Int, MutableMap<String, Int>>{
        val horizontalMap : MutableMap<Int, MutableMap<String, Int>> = mutableMapOf()
        for (week in 1 .. data.schedulePeriod){
            val temp: MutableMap<String, Int> = mutableMapOf()
            val coverage = data.horizontalCoverages.find { it.id == coverageId }
            for (staff in data.staffs){
                temp[staff.id] = 0
                if (coverage != null) {
                    for (day in coverage.days) {
                        if (schedules[staff.id]?.get(day + 7*(week-1))!! in coverage.shifts && day in coverage.days) {
                            temp[staff.id] = temp[staff.id]!! + 1
                        }
                    }
                }
            }
            horizontalMap[week] = temp
        }
        return horizontalMap
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    private fun initialSolution(): MutableMap<String, MutableMap<Int, String>> {
        val schedule : MutableMap<String, MutableMap<Int, String>> = mutableMapOf()

        for (staff in data.staffs) {
            schedule[staff.id] = mutableMapOf()
            for (day in 1..7 * data.schedulePeriod){
                if (schedule[staff.id]?.get(day) != "PH"){
                    schedule[staff.id]?.set(day, "")
                    if (day % 7 == 0){
                        schedule[staff.id]?.set(day, "DO")
                    }
                }
            }
        }

        for (week in 1..data.schedulePeriod) {
            for (coverage in data.coverages) {
                for (staff in data.staffs) {
                    if (calculateCoverageFulfillment(schedule, coverage.id, coverage.day, week) < coverage.desireValue &&
                        checkIfStaffInStaffGroup(staff, coverage.staffGroups) &&
                        checkIfStaffInStaffGroup(staff, coverage.staffGroups) &&
                        schedule[staff.id]?.get(coverage.day + 7*(week - 1)) == ""
                    ) {
                        schedule[staff.id]?.set(coverage.day + 7*(week - 1), coverage.shift.filterNot { it == "PH" || it == "DO" }.random())
                    }
                }
            }
            for (coverage in data.coverages) {
                for (staff in data.staffs) {
                    if (schedule[staff.id]?.get(coverage.day + +7*(week - 1)) == "") {
                        schedule[staff.id]?.set(coverage.day + 7*(week - 1), data.shifts.filterNot { it.id == "PH" || it.id == "DO" }.random().id)
                    }
                }
            }
        }
        return schedule
    }

    private fun randomDestroySolution(schedules: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>> {
        val mutableSchedule = deepCopySolution(schedules)
        if (mutableSchedule.isNotEmpty()) {

            var randomScheduleStaff = mutableSchedule.keys.random()
            var randomScheduleDay = mutableSchedule[randomScheduleStaff]?.keys?.random()
            while(mutableSchedule[randomScheduleStaff]?.get(randomScheduleDay!!.toInt())!! == "PH"){
                randomScheduleStaff = mutableSchedule.keys.random()
                randomScheduleDay = mutableSchedule[randomScheduleStaff]?.keys?.random()
            }
            if (randomScheduleDay != null) {
                mutableSchedule[randomScheduleStaff]?.set(randomScheduleDay.toInt(), "")
            }

        }
        return mutableSchedule
    }

    private fun repairSolution(schedules: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>> {
        val repairedSchedule = deepCopySolution(schedules)

        for (staffId in repairedSchedule.keys) {
            for (dayId in repairedSchedule[staffId]!!.keys) {
                if (repairedSchedule[staffId]?.get(dayId) == ""){
                    if(staffId == "Staff_1" || staffId == "Staff_3" || staffId == "Staff_6"){
                        repairedSchedule[staffId]?.set(dayId, data.shifts.filterNot {
                                it.id == "PH" ||
                                it.id == "A2" ||
                                it.id == "M2"}.random().id
                        )
                    }
                    else{
                        repairedSchedule[staffId]?.set(dayId, data.shifts.filterNot {
                                it.id == "PH" ||
                                it.id == "M3" }.random().id
                        )
                    }
                }
            }
        }
        return repairedSchedule
    }

    private fun randomSwapStaffShift(currentSolution: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>> {
        var newSolution = randomDestroySolution(currentSolution)
        newSolution = repairSolution(newSolution)
        return newSolution
    }

    private fun greedyCoverageEnhancement(schedules: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>> {
        val bestSchedule: MutableMap<String, MutableMap<Int, String>>
        val scoreTemp : MutableMap<MutableMap<String, MutableMap<Int, String>>, Double> = mutableMapOf()
        for (week in 1.. data.schedulePeriod) {
            data.coverages.forEach { coverage ->
                if (coverage.type.contains("equal to")){
                    val currentFulfillment = calculateCoverageFulfillment(schedules, coverage.id, coverage.day, week)
                    if (currentFulfillment < coverage.desireValue) {
                        data.staffs.forEach { staff ->
                            if (checkIfStaffInStaffGroup(staff, coverage.staffGroups)) {
                                coverage.shift.filterNot { it == "PH" || it == "DO" }.forEach { shift ->
                                    val tempSolution = deepCopySolution(schedules)
                                    if (tempSolution[staff.id]?.get(coverage.day + 7 * (week - 1)) != "PH") {
                                        tempSolution[staff.id]?.set(coverage.day + 7 * (week - 1), shift)
                                        scoreTemp[tempSolution] = calculate.totalScore(tempSolution)
                                        if ((calculate.coverageScore(schedules) < calculate.coverageScore(
                                                tempSolution))
                                            && calculate.totalScore(schedules) < calculate.totalScore(tempSolution))
                                        {
                                            return tempSolution
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else if (currentFulfillment > coverage.desireValue) {
                        data.staffs.forEach { staff ->
                            if (checkIfStaffInStaffGroup(staff, coverage.staffGroups)) {
                                if (schedules[staff.id]?.get(coverage.day + 7 * (week - 1)) in coverage.shift) {
                                    val tempSolution = deepCopySolution(schedules)
                                    for (shiftFill in data.shifts.filterNot { it.id == "PH" }){
                                        if(shiftFill.id !in coverage.shift
                                            && shiftFill.id != "PH"
                                            && shiftFill.id != "DO")
                                        {
                                            if (tempSolution[staff.id]?.get(coverage.day + 7 * (week - 1)) != "PH") {
                                                tempSolution[staff.id]?.set(coverage.day + 7 * (week - 1), shiftFill.id)
                                                scoreTemp[tempSolution] = calculate.totalScore(tempSolution)
                                                if ((calculate.coverageScore(schedules) < calculate.coverageScore(
                                                        tempSolution
                                                    )) &&
                                                    calculate.totalScore(schedules) < calculate.totalScore(tempSolution)
                                                ) {
                                                    return tempSolution
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else if(coverage.type.contains("at least")){
                    val currentFulfillment = calculateCoverageFulfillment(schedules, coverage.id, coverage.day, week)
                    if (currentFulfillment < coverage.desireValue) {
                        data.staffs.forEach { staff ->
                            if (checkIfStaffInStaffGroup(staff, coverage.staffGroups)) {
                                coverage.shift.filterNot {
                                    it == "PH"
                                    || it == "DO"
                                }.forEach { shift ->
                                    val tempSolution = deepCopySolution(schedules)
                                    if (tempSolution[staff.id]?.get(coverage.day + 7 * (week - 1)) != "PH") {
                                        tempSolution[staff.id]?.set(coverage.day + 7 * (week - 1), shift)
                                        scoreTemp[tempSolution] = calculate.totalScore(tempSolution)
                                        if ((calculate.coverageScore(schedules) < calculate.coverageScore(tempSolution)) &&
                                            calculate.totalScore(schedules) < calculate.totalScore(tempSolution)
                                        ) {
                                            return tempSolution
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else if(coverage.type.contains("at most")){
                    val currentFulfillment = calculateCoverageFulfillment(schedules, coverage.id, coverage.day, week)
                    if (currentFulfillment > coverage.desireValue) {
                        data.staffs.forEach { staff ->
                            if (checkIfStaffInStaffGroup(staff, coverage.staffGroups)) {
                                coverage.shift
                                    .filterNot {
                                        it == "PH"
                                        || it == "DO"
                                    }.forEach { shift ->
                                    val tempSolution = deepCopySolution(schedules)
                                    if (tempSolution[staff.id]?.get(coverage.day + 7 * (week - 1)) != "PH") {
                                        tempSolution[staff.id]?.set(coverage.day + 7 * (week - 1), shift)
                                        scoreTemp[tempSolution] = calculate.totalScore(tempSolution)
                                        if ((calculate.coverageScore(schedules) < calculate.coverageScore(tempSolution)) &&
                                            calculate.totalScore(schedules) < calculate.totalScore(tempSolution)
                                        ) {
                                            return tempSolution
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        bestSchedule = scoreTemp.maxByOrNull { it.value }?.key!!
        return bestSchedule
    }

    private fun greedySwapEnhancement(schedules: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>>{
        val newSchedule = deepCopySolution(schedules)
        var shiftSwap: String
        if (newSchedule.isNotEmpty()) {
            data.staffs.forEach{ staff ->
                for (week in 1 .. data.schedulePeriod){
                    for(index1 in 1..7){
                        shiftSwap = newSchedule[staff.id]?.get(index1 + 7* (week - 1)).toString()
                        if (shiftSwap != "PH") {

                            for (index2 in 1..7) {
                                val tempSchedule = deepCopySolution(newSchedule)
                                if (tempSchedule[staff.id]?.get(index2 + 7 * (week - 1))!! != "PH") {
                                    tempSchedule[staff.id]?.set(
                                        index1 + 7 * (week - 1),
                                        tempSchedule[staff.id]?.get(index2 + 7 * (week - 1))!!
                                    )
                                    tempSchedule[staff.id]?.set(index2 + 7 * (week - 1), shiftSwap)
                                    if (CommonCaculate(data).totalScore(tempSchedule) > CommonCaculate(data).totalScore(
                                            newSchedule
                                        )
                                    ) {
                                        return tempSchedule
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return newSchedule
    }

    private fun greedyCoverageHorizontalEnhancement(schedules: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>> {
        data.horizontalCoverages.forEach { horizontalCover ->
            val fullHorizontalCoverFullFill = calculateHorizontalCoverageFulfillment(schedules, horizontalCover.id)
            for ((week, horizontalCoverFullFill) in fullHorizontalCoverFullFill){
                for (item in horizontalCoverFullFill) {
                    if (horizontalCover.type.contains("at least")){
                        if (item.value < horizontalCover.desireValue) {
                            for (shift in horizontalCover.shifts) {
                                 horizontalCover.days.forEach { day ->
                                     val tempSolution = deepCopySolution(schedules)
                                     if (tempSolution[item.key]?.get(day + 7 * (week - 1)) != "PH") {
                                         tempSolution[item.key]?.set(day + 7 * (week - 1), shift)
                                         if ((calculate.horizontalCoverageScore(schedules) < calculate.horizontalCoverageScore(
                                                 tempSolution
                                             ))) {
                                             return tempSolution
                                         }
                                     }
                                }
                            }
                        }
                    }
                    else if (horizontalCover.type.contains("equal to")){
                        if(item.value < horizontalCover.desireValue){
                            for (shift in horizontalCover.shifts){
                                for (day in horizontalCover.days) {
                                    val tempSolution = deepCopySolution(schedules)
                                    if (tempSolution[item.key]?.get(day + 7 * (week - 1)) != "PH") {
                                        tempSolution[item.key]?.set(day + 7 * (week - 1), shift)
                                        if ((calculate.horizontalCoverageScore(schedules) < calculate.horizontalCoverageScore(
                                                tempSolution
                                        )))//&&
                                            //calculate.totalScore(schedules) < calculate.totalScore(tempSolution))
                                        return tempSolution
                                    }
                                }
                            }
                        }
                        else if(item.value > horizontalCover.desireValue){
                            for (shift in horizontalCover.shifts){
                                for (day in horizontalCover.days) {
                                    if (schedules[item.key]?.get(day + 7*(week - 1)) in horizontalCover.shifts){
                                        for (shiftFill in data.shifts.filterNot { it.id == "PH" }){
                                            if(shiftFill.id !in horizontalCover.shifts && shiftFill.id != "PH"){
                                                val tempSolution = deepCopySolution(schedules)
                                                if (tempSolution[item.key]?.get(day + 7 * (week - 1)) != "PH") {
                                                    tempSolution[item.key]?.set(day + 7 * (week - 1), shiftFill.id)
                                                    if ((calculate.horizontalCoverageScore(schedules) < calculate.horizontalCoverageScore(
                                                            tempSolution
                                                        )))//&&
                                                    //calculate.totalScore(schedules) < calculate.totalScore(tempSolution))
                                                        return tempSolution
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else if (horizontalCover.type.contains("at most")){
                        if(item.value > horizontalCover.desireValue){
                            for (shift in horizontalCover.shifts){
                                for (day in horizontalCover.days) {
                                    if (schedules[item.key]?.get(day + 7*(week - 1)) in horizontalCover.shifts){
                                        for (shiftFill in data.shifts.filterNot { it.id == "PH" }){
                                            if(shiftFill.id !in horizontalCover.shifts && shiftFill.id != "PH"){
                                                val tempSolution = deepCopySolution(schedules)
                                                if (tempSolution[item.key]?.get(day + 7 * (week - 1)) != "PH") {
                                                    tempSolution[item.key]?.set(day + 7 * (week - 1), shiftFill.id)
                                                    if ((calculate.horizontalCoverageScore(schedules) < calculate.horizontalCoverageScore(
                                                            tempSolution
                                                        )))//&&
                                                    //calculate.totalScore(schedules) < calculate.totalScore(tempSolution))
                                                        return tempSolution
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return schedules
    }

    private fun greedySwapAHalfShiftWithBestScore(schedule: MutableMap<String, MutableMap<Int, String>>, key: String, week: Int):  MutableMap<MutableMap<String, MutableMap<Int, String>>, Double>{
        val listSchedule : MutableMap<MutableMap<String, MutableMap<Int, String>>, Double> = mutableMapOf()
        for (day in 1 .. 7){
            var shiftInfo = schedule[key]?.get(day + 7*(week - 1))!!
            if (shiftInfo == "DO" || shiftInfo == "PH"){
                continue
            }
            else {
                if (shiftInfo in data.shiftGroups.find { it.id == "AF" }?.shifts!!) {
                    shiftInfo = "M3"
                } else if (shiftInfo in data.shiftGroups.find { it.id == "MO" }?.shifts!! && shiftInfo != "M3") {
                    shiftInfo = "M3"
                }
                val temp = deepCopySolution(schedule)
                temp[key]?.set(day + 7 * (week - 1), shiftInfo)
                listSchedule[temp] = calculate.constrainScore(temp)
            }
        }
        return listSchedule
    }

    private fun greedyDestroyAHalfShiftWithBestScore(schedule: MutableMap<String, MutableMap<Int, String>>, key: String, week: Int):  MutableMap<MutableMap<String, MutableMap<Int, String>>, Double>{
        val listSchedule : MutableMap<MutableMap<String, MutableMap<Int, String>>, Double> = mutableMapOf()
        for (day in 1 .. 7){
            val shiftInfo = schedule[key]?.get(day + 7*(week - 1))!!
            if (data.shifts.find { it.id == shiftInfo }!!.duration == 4) {
                for (shift in data.shifts.filterNot { it.id =="PH" }){
                    if (shift.id != "PH" && shift.duration != 4){
                        val temp = deepCopySolution(schedule)
                        temp[key]?.set(day + 7*(week - 1), shift.id)
                        listSchedule[temp] = calculate.constrainScore(temp)
                    }
                }
            }
        }
        return listSchedule
    }

    private fun greedySwapToMaxDurationShiftWithBestScore(schedule: MutableMap<String, MutableMap<Int, String>>, key: String, week: Int):  MutableMap<MutableMap<String, MutableMap<Int, String>>, Double>{
        val listSchedule : MutableMap<MutableMap<String, MutableMap<Int, String>>, Double> = mutableMapOf()
        for (day in 1 .. 7){
            var shiftInfo = schedule[key]?.get(day + 7*(week - 1))!!
            if (shiftInfo == "DO" || shiftInfo == "PH"){
                continue
            }
            else {
                if (shiftInfo in data.shiftGroups.find { it.id == "AF" }?.shifts!! && shiftInfo != "A1") {
                    shiftInfo = "A1"
                } else if (shiftInfo in data.shiftGroups.find { it.id == "MO" }?.shifts!! && shiftInfo != "M1") {
                    shiftInfo = "M1"
                }
                val temp = deepCopySolution(schedule)
                temp[key]?.set(day + 7 * (week - 1), shiftInfo)
                listSchedule[temp] = calculate.totalScore(temp)
            }
        }
        return listSchedule
    }

    private fun greedySwapToSevenHoursDurationShiftWithBestScore(schedule: MutableMap<String, MutableMap<Int, String>>, key: String, week: Int):  MutableMap<MutableMap<String, MutableMap<Int, String>>, Double>{
        val listSchedule : MutableMap<MutableMap<String, MutableMap<Int, String>>, Double> = mutableMapOf()
        for (day in 1 .. 7){
            var shiftInfo = schedule[key]?.get(day + 7*(week - 1))!!
            if (shiftInfo == "DO" || shiftInfo == "PH"){
                continue
            }
            else{
                if (shiftInfo in data.shiftGroups.find{it.id == "AF"}?.shifts!! && shiftInfo != "A2"){
                    shiftInfo = "A2"
                }
                else if (shiftInfo in data.shiftGroups.find{it.id == "MO"}?.shifts!! && shiftInfo != "M2"){
                    shiftInfo = "M2"
                }
                val temp = deepCopySolution(schedule)
                temp[key]?.set(day + 7*(week - 1), shiftInfo)
                listSchedule[temp] = calculate.totalScore(temp)
            }
        }
        return listSchedule
    }

    private fun routeWheel(iter: Int): Int{
        if (iter % 400 == 0){
            resetWeightOperators()
        }
        else {
            createWeightOperators()
        }

        val rand = Random.nextDouble()
        var sum = 0.0

        for (index in 0 until this.operatorWeight.size){
            sum += this.operatorWeight[index]!!
        }
        this.probabilitiesOfOperator[0] = this.operatorWeight[0]!!/sum
        for (index in 1 until this.operatorWeight.size){
            this.probabilitiesOfOperator[index] = this.probabilitiesOfOperator[index - 1]!! + this.operatorWeight[index]!!/sum
        }

        var choseValue = 0

        if (rand <= this.probabilitiesOfOperator[0]!!){
            choseValue = 0
        }
        else{
            for(index in 1 until this.operatorScore.size ){
                if (rand > this.probabilitiesOfOperator[index - 1]!! && rand <= this.probabilitiesOfOperator[index]!!){
                    choseValue = index
                }
            }
        }
        this.operatorTimes[choseValue] = this.operatorTimes[choseValue]!! + 1
        return choseValue
    }

    private fun shakeAndRepair(schedules: MutableMap<String, MutableMap<Int, String>>, number: Int): MutableMap<String, MutableMap<Int, String>>{
        when (number){
            0 -> return randomSwapStaffShift(schedules)
            1 -> return greedyCoverageEnhancement(schedules)
            2 -> return greedyCoverageHorizontalEnhancement(schedules)
            3 -> return greedySwapEnhancement(schedules)
            4 -> return fixViolations(schedules)
        }
        return schedules
    }

    private fun fixViolations(schedules: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>>{
        val newScheduled = deepCopySolution(schedules)
        for (week in 1 .. data.schedulePeriod){
            for (staff in data.staffs){
                for(day in 1 .. 4){
                    if (checkPatternViolation(newScheduled, week, day ,staff.id)){
                        return greedyFixedShiftPatternViolations(newScheduled, week, day, staff.id)
                    }
                }
            }
        }

        for (week in 1 .. data.schedulePeriod){
            for (staff in data.staffs){
                for(day in 1 .. 7){
                    if (checkConstrainViolation(newScheduled, week ,staff.id)){
                        return greedyFixedConstraintViolations(newScheduled, week, staff.id)
                    }
                }
            }
        }

        return newScheduled
    }

    private fun greedyFixedShiftPatternViolations(schedules: MutableMap<String, MutableMap<Int, String>>, week: Int, day: Int, staff: String):MutableMap<String, MutableMap<Int, String>>{
        val nextScheduled = deepCopySolution(schedules)
        for (constrain in data.patternConstrains) {
            constrain.parsingPattern()
            for (index in 1..constrain.patternLists.values.maxOf { it.size }) {
                if (rule.checkPatternConstrainViolation(constrain, nextScheduled, week, day, staff)) {
                    for (shift in data.shifts.filterNot { it.id == "PH" }) {
                        val temp = deepCopySolution(nextScheduled)
                        temp[staff]?.set(day + 7 * (week - 1) + index, shift.id)
                        if (!rule.checkPatternConstrainViolation(constrain, temp, week, day, staff)) {
                            if (calculate.patternConstrainScore(temp) >= calculate.patternConstrainScore(nextScheduled)
                                && !checkPatternViolation(temp, week, day, staff)
                                && calculate.totalScore(temp) >= calculate.totalScore(nextScheduled)
                            ) {
                                return temp
                            }
                        }
                    }
                }
            }
        }
        return nextScheduled
    }

    private fun greedyFixedConstraintViolations(schedules: MutableMap<String, MutableMap<Int, String>>, week :Int, staff: String): MutableMap<String, MutableMap<Int, String>>{
        var newSchedule = deepCopySolution(schedules)
        for (constrain in data.constrains) {
            if(constrain.isHard){
                if (staff in constrain.staffGroup || constrain.staffGroup.contains("all_staffs")) {
                    when (constrain.id) {
                        "exactly-staff-working-time" -> {
                            var staffWorkingTime = 0.0
                            for (day in 1..7) {
                                staffWorkingTime += data.shifts.find { it.id == newSchedule[staff]?.get(day + 7 * (week - 1)) }?.duration!!
                            }
                            if (staffWorkingTime.toInt() != 44) {
                                if (staff == "Staff_1" || staff == "Staff_3" || staff == "Staff_6") {
                                    val countDuration: MutableMap<Int, Int> = mutableMapOf()
                                    countDuration[8] = 0
                                    countDuration[7] = 0
                                    countDuration[4] = 0
                                    countDuration[0] = 0
                                    for (day in 1..7) {
                                        if (data.shifts.find { it.id == newSchedule[staff]?.get(day + 7 * (week - 1)) }?.duration!! == 7) {
                                            if (newSchedule[staff]?.get(day + 7 * (week - 1))!! in data.shiftGroups.find { it.id == "AF" }?.shifts!!) {
                                                newSchedule[staff]?.set(day + 7 * (week - 1), "A1")
                                            } else if (newSchedule[staff]?.get(day + 7 * (week - 1))!! in data.shiftGroups.find { it.id == "MO" }?.shifts!!) {
                                                newSchedule[staff]?.set(day + 7 * (week - 1), "M1")
                                            }
                                        }
                                        countDuration[data.shifts.find { it.id == newSchedule[staff]?.get(day + 7 * (week - 1)) }?.duration!!] =
                                            countDuration[data.shifts.find {
                                                it.id == newSchedule[staff]?.get(
                                                    day + 7 * (week - 1)
                                                )
                                            }?.duration!!]!! + 1
                                    }

                                    if (countDuration[4] == 0) {
                                        newSchedule = greedySwapAHalfShiftWithBestScore(newSchedule, staff, week).maxByOrNull { it.value }?.key!!
                                    }
                                }
                                else {
                                    val countDuration: MutableMap<Int, Int> = mutableMapOf()
                                    countDuration[8] = 0
                                    countDuration[7] = 0
                                    countDuration[4] = 0
                                    countDuration[0] = 0
                                    for (day in 1..7) {
                                        countDuration[data.shifts.find { it.id == newSchedule[staff]?.get(day + 7 * (week - 1)) }?.duration!!] =
                                            countDuration[data.shifts.find {
                                                it.id == newSchedule[staff]?.get(
                                                    day + 7 * (week - 1)
                                                )
                                            }?.duration!!]!! + 1
                                    }
                                    if (countDuration[4]!! > 0){
                                        for  (index in 1.. countDuration[4]!!) {
                                            newSchedule = greedyDestroyAHalfShiftWithBestScore(
                                                newSchedule,
                                                staff,
                                                week
                                            ).maxByOrNull { it.value }?.key!!
                                        }
                                    }
                                    if (countDuration[8]!! < 2) {
                                        for (index in 1..2 - countDuration[8]!!) {
                                            val tempList = greedySwapToMaxDurationShiftWithBestScore(
                                                newSchedule,
                                                staff,
                                                week
                                            )
                                            newSchedule = tempList.maxByOrNull { it.value }?.key!!
                                        }
                                    } else if (countDuration[8]!! > 2) {
                                        for (index in 1..countDuration[8]!! - 2) {
                                            val tempList = greedySwapToSevenHoursDurationShiftWithBestScore(
                                                newSchedule,
                                                staff,
                                                week
                                            )
                                            newSchedule = tempList.maxByOrNull { it.value }?.key!!
                                        }
                                    }
                                }
                            }
                        }

                        "archive-0.5-day" -> {
                            val numberOfHalfShift: MutableMap<String, Int> = mutableMapOf()
                            numberOfHalfShift[staff] = 0
                            for (day in 1..7) {
                                if (data.shifts.find { it.id == newSchedule[staff]?.get(day + 7 * (week - 1)) }?.duration!! == 4) {
                                    numberOfHalfShift[staff] = numberOfHalfShift[staff]!! + 1
                                }
                            }
                            for ((key, value) in numberOfHalfShift) {
                                if(value != 1) {
                                    if (value < 1) {
                                        newSchedule = greedySwapAHalfShiftWithBestScore(newSchedule, key, week).maxByOrNull { it.value }?.key!!
                                    } else {
                                        for (index in 1..< value) {
                                            newSchedule = greedyDestroyAHalfShiftWithBestScore(
                                                newSchedule,
                                                key,
                                                week
                                            ).maxByOrNull { it.value }?.key!!
                                        }
                                    }
                                }
                            }
                            return newSchedule
                        }

                        "un-archive-0.5-day" -> {
                            val numberOfHalfShift: MutableMap<String, Int> = mutableMapOf()
                            numberOfHalfShift[staff] = 0
                            for (day in 1..7) {
                                if (data.shifts.find { it.id == newSchedule[staff]?.get(day + 7 * (week - 1)) }?.duration!! == 4) {
                                    numberOfHalfShift[staff] = numberOfHalfShift[staff]!! + 1
                                }
                            }
                            for ((key, value) in numberOfHalfShift) {
                                if (value > 0) {
                                    for (index in 1..value) {
                                        newSchedule = greedyDestroyAHalfShiftWithBestScore(newSchedule, key, week).maxByOrNull { it.value }?.key!!
                                    }
                                }
                            }
                            return newSchedule
                        }
                    }
                }
            }
        }
        return newSchedule
    }

    private fun checkPatternViolation(schedules: MutableMap<String, MutableMap<Int, String>>, week: Int, day: Int, staff: String): Boolean{
        for (constrain in data.patternConstrains){
            if (rule.checkPatternConstrainViolation(constrain, schedules, week, day, staff)) {
                return true
            }
        }
        return false
    }

    private fun checkConstrainViolation(schedules: MutableMap<String, MutableMap<Int, String>>, week: Int, staff: String): Boolean{
        for (constrain in data.constrains){
            if (rule.checkConstrainViolation(constrain, schedules, week, staff)) {
                return false
            }
        }
        return true
    }

    private fun getHigherPriorityHardConstraint(priority: Int, constraint: Any): MutableList<Any> {
        val map = mutableListOf<Any>()
        for (constrain in data.constrains) {
            if( constrain.isHard) {
                if (constrain.priority >= priority && constrain.id != constraint) {
                    map.add(constrain)
                }
            }
        }

        for (constrain in data.patternConstrains) {
            if (constrain.isHard) {
                if (constrain.priority >= priority && constrain.id != constraint) {
                    map.add(constrain)
                }
            }
        }

        for (cover in data.coverages) {
            if (cover.type.contains("hard") && cover.id != constraint) {
                if(cover.priority >= priority && cover.id != constraint) {
                    map.add(cover)
                }
            }
        }

        for (horizontalCover in data.horizontalCoverages) {
            if (horizontalCover.type.contains("hard") && horizontalCover.id != constraint) {
                if(horizontalCover.priority >= priority && horizontalCover.id != constraint) {
                    map.add(horizontalCover)
                }
            }
        }
        return map
    }

    private fun isViolationHigherConstraint(listConstraint: MutableList<Any>, schedules: MutableMap<String, MutableMap<Int, String>>): Boolean{
        for(constrain in listConstraint){
            if (!RuleViolation(data).checkAllViolation(constrain, schedules)){
                return false
            }
        }
        return true
    }

    private fun adjustScheduleToConstrain(schedule: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>> {
        var newSchedule = deepCopySolution(schedule)

        val constrainsWithPriority: MutableList<Pair<Any, Int>> = mutableListOf()

        for (item in data.constrains
            .filter {
                it.isHard
            })
        {
            constrainsWithPriority.add(Pair(item, item.priority))
        }

        for (item in data.patternConstrains
            //.filter { it.isHard }
        ) {
            constrainsWithPriority.add(Pair(item, item.priority))
        }
        for (item in data.coverages
            .filter {
                it.type.contains("hard")
            })
        {
            constrainsWithPriority.add(Pair(item, item.priority))
        }

        for (item in data.horizontalCoverages
            .filter {
                it.type.contains("hard")
            })
        {
            constrainsWithPriority.add(Pair(item, item.priority))
        }

        for (i in constrainsWithPriority.indices) {
            for (j in 0 until constrainsWithPriority.size - 1 - i) {
                if (constrainsWithPriority[j].second < constrainsWithPriority[j + 1].second) {
                    // Swap
                    val temp = constrainsWithPriority[j]
                    constrainsWithPriority[j] = constrainsWithPriority[j + 1]
                    constrainsWithPriority[j + 1] = temp
                }
            }
        }

        for ((constrain, _) in constrainsWithPriority) {
            when (constrain) {
                is Constraint -> {
                    var maxFixIteration = 0
                    val listUpper = getHigherPriorityHardConstraint(constrain.priority, constrain.id)
                    val listUpperIncludeCurrent =  (listUpper + constrain).toMutableList()
                    while (maxFixIteration < 500 && !isViolationHigherConstraint(listUpperIncludeCurrent, newSchedule)) {

                        when (constrain.id) {
                            "exactly-staff-working-time" -> {
                                for (week in 1..data.schedulePeriod) {
                                    val input: MutableMap<String, Double> = mutableMapOf()
                                    for (staff in data.staffs) {
                                        var staffWorkingTime = 0.0
                                        for (day in 1..7) {
                                            staffWorkingTime += data.shifts.find { it.id == newSchedule[staff.id]?.get(day + 7 * (week - 1)) }?.duration!!
                                        }
                                        input[staff.id] = staffWorkingTime
                                    }
                                    for ((key, value) in input) {
                                        if (value.toInt() != 44) {
                                            if (key == "Staff_1" || key == "Staff_3" || key == "Staff_6") {
                                                val countDuration: MutableMap<Int, Int> = mutableMapOf()
                                                countDuration[8] = 0
                                                countDuration[7] = 0
                                                countDuration[4] = 0
                                                countDuration[0] = 0
                                                for (day in 1..7) {
                                                    if (data.shifts.find { it.id == newSchedule[key]?.get(day + 7 * (week - 1)) }?.duration!! == 7) {
                                                        if (newSchedule[key]?.get(day + 7 * (week - 1))!! in data.shiftGroups.find { it.id == "AF" }?.shifts!! ) {
                                                            newSchedule[key]?.set(day + 7 * (week - 1), "A1")
                                                        } else if (newSchedule[key]?.get(day + 7 * (week - 1))!! in data.shiftGroups.find { it.id == "MO" }?.shifts!!) {
                                                            newSchedule[key]?.set(day + 7 * (week - 1), "M1")
                                                        }
                                                    }
                                                    countDuration[data.shifts.find { it.id == newSchedule[key]?.get(day + 7 * (week - 1)) }?.duration!!] =
                                                        countDuration[data.shifts.find {
                                                            it.id == newSchedule[key]?.get(
                                                                day + 7 * (week - 1)
                                                            )
                                                        }?.duration!!]!! + 1
                                                }
                                                if (countDuration[4] == 0) {
                                                    newSchedule = greedySwapAHalfShiftWithBestScore(newSchedule, key, week).maxByOrNull { it.value }?.key!!
                                                } else if(countDuration[4]!! > 1 ){
                                                    for(index in 1..< countDuration[4]!!-1){
                                                        newSchedule = greedyDestroyAHalfShiftWithBestScore(
                                                            newSchedule,
                                                            key,
                                                            week
                                                        ).maxByOrNull { it.value }?.key!!
                                                        //for (finalSchedule in tempList.keys){
                                                        //if (isViolationHigherConstraint(listUpper, finalSchedule)){
                                                        //newSchedule = finalSchedule
                                                        //}
                                                        //}
                                                    }
                                                }
                                            } else {
                                                val countDuration: MutableMap<Int, Int> = mutableMapOf()
                                                countDuration[8] = 0
                                                countDuration[7] = 0
                                                countDuration[4] = 0
                                                countDuration[0] = 0
                                                for (day in 1..7) {
                                                    countDuration[data.shifts.find { it.id == newSchedule[key]?.get(day + 7 * (week - 1)) }?.duration!!] =
                                                        countDuration[data.shifts.find { it.id == newSchedule[key]?.get(day + 7 * (week - 1)) }?.duration!!]!! + 1
                                                }

                                                if (countDuration[4]!! > 0){
                                                    for  (index in 1.. countDuration[4]!!) {
                                                        val tempList = greedyDestroyAHalfShiftWithBestScore(
                                                            newSchedule,
                                                            key,
                                                            week
                                                        )
                                                        if (isViolationHigherConstraint(listUpper, tempList.maxByOrNull { it.value }?.key!!)){
                                                            newSchedule =  tempList.maxByOrNull { it.value }?.key!!
                                                        } else {
                                                            for (finalSchedule in tempList.keys) {
                                                                if (isViolationHigherConstraint(listUpper, finalSchedule)) {
                                                                    newSchedule = finalSchedule
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                if (countDuration[8]!! < 2) {
                                                    for (index in 1..2 - countDuration[8]!!) {
                                                        val tempList = greedySwapToMaxDurationShiftWithBestScore(
                                                            newSchedule,
                                                            key,
                                                            week
                                                        )
                                                        if (isViolationHigherConstraint(listUpper, tempList.maxByOrNull { it.value }?.key!!)){
                                                            newSchedule =  tempList.maxByOrNull { it.value }?.key!!
                                                        } else {
                                                            for (finalSchedule in tempList.keys) {
                                                                if (isViolationHigherConstraint(listUpper, finalSchedule)) {
                                                                    newSchedule = finalSchedule
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else if (countDuration[8]!! > 2) {
                                                    for (index in 1..countDuration[8]!! - 2) {
                                                        val tempList = greedySwapToSevenHoursDurationShiftWithBestScore(
                                                            newSchedule,
                                                            key,
                                                            week
                                                        )
                                                        if (isViolationHigherConstraint(listUpper, tempList.maxByOrNull { it.value }?.key!!)){
                                                            newSchedule =  tempList.maxByOrNull { it.value }?.key!!
                                                        } else {
                                                            for (finalSchedule in tempList.keys) {
                                                                if (isViolationHigherConstraint(listUpper, finalSchedule)) {
                                                                    newSchedule = finalSchedule
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            "archive-0.5-day" -> {
                                for (week in 1..data.schedulePeriod) {
                                    val numberOfHalfShift: MutableMap<String, Int> = mutableMapOf()
                                    for (staff in constrain.staffGroup) {
                                        numberOfHalfShift[staff] = 0
                                        for (day in 1..7) {
                                            if (data.shifts.find { it.id == newSchedule[staff]?.get(day + 7 * (week - 1)) }?.duration!! == 4) {
                                                numberOfHalfShift[staff] = numberOfHalfShift[staff]!! + 1
                                            }
                                        }
                                    }
                                    for ((key, value) in numberOfHalfShift) {
                                        if (value < 1) {
                                            val tempList = greedySwapAHalfShiftWithBestScore(newSchedule, key, week)
                                            if (isViolationHigherConstraint(listUpper, tempList.maxByOrNull { it.value }?.key!!)){
                                                newSchedule =  tempList.maxByOrNull { it.value }?.key!!
                                            } else {
                                                for (finalSchedule in tempList.keys) {
                                                    if (isViolationHigherConstraint(listUpper, finalSchedule)) {
                                                        newSchedule = finalSchedule
                                                    }
                                                }
                                            }
                                        } else if (value > 1) {
                                            for (index in 1..<value) {
                                                val tempList = greedyDestroyAHalfShiftWithBestScore(newSchedule, key, week)
                                                if (isViolationHigherConstraint(listUpper, tempList.maxByOrNull { it.value }?.key!!)){
                                                    newSchedule =  tempList.maxByOrNull { it.value }?.key!!
                                                } else {
                                                    for (finalSchedule in tempList.keys) {
                                                        if (isViolationHigherConstraint(listUpper, finalSchedule)) {
                                                            newSchedule = finalSchedule
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            "un-archive-0.5-day" -> {
                                for (week in 1..data.schedulePeriod) {
                                    val numberOfHalfShift: MutableMap<String, Int> = mutableMapOf()
                                    for (staff in constrain.staffGroup) {
                                        numberOfHalfShift[staff] = 0
                                        for (day in 1..7) {
                                            if (data.shifts.find { it.id == newSchedule[staff]?.get(day + 7 * (week - 1)) }?.duration!! == 4) {
                                                numberOfHalfShift[staff] = numberOfHalfShift[staff]!! + 1
                                            }
                                        }
                                    }
                                    for ((key, value) in numberOfHalfShift) {
                                        if (value > 0) {
                                            for (index in 1..value) {
                                                val tempList = greedyDestroyAHalfShiftWithBestScore(newSchedule, key, week)
                                                if (isViolationHigherConstraint(listUpper, tempList.maxByOrNull { it.value }?.key!!)){
                                                    newSchedule =  tempList.maxByOrNull { it.value }?.key!!
                                                } else {
                                                    for (finalSchedule in tempList.keys) {
                                                        if (isViolationHigherConstraint(listUpper, finalSchedule)) {
                                                            newSchedule = finalSchedule
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        maxFixIteration += 1
                    }
                }

                is PatternConstrain -> {
                    var maxFixIteration = 0
                    val listUpper = getHigherPriorityHardConstraint(constrain.priority, constrain.id)
                    val listUpperIncludeCurrent = (listUpper + constrain).toMutableList()
                    constrain.parsingPattern()
                    while (maxFixIteration < 100 && !isViolationHigherConstraint(listUpperIncludeCurrent, newSchedule)){
                        for (week in 1..data.schedulePeriod) {
                            for (day in 1..7 - 3) {
                                for (staff in constrain.staffGroup) {
                                    for (index in 1..constrain.patternLists.values.maxOf { it.size }) {
                                        if (rule.checkPatternConstrainViolation(
                                                constrain,
                                                newSchedule,
                                                week,
                                                day,
                                                staff
                                            )
                                        ) {
                                            for (shift in data.shifts.filterNot { it.id == "PH" }) {
                                                val temp = deepCopySolution(newSchedule)
                                                if (temp[staff]?.get(day + 7 * (week - 1) + index) != "PH"){
                                                    temp[staff]?.set(day + 7 * (week - 1) + index, shift.id)
                                                    if (!rule.checkPatternConstrainViolation(
                                                            constrain,
                                                            temp,
                                                            week,
                                                            day,
                                                            staff
                                                        ) && isViolationHigherConstraint(listUpper, newSchedule)
                                                    ) {
                                                        newSchedule = temp
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        maxFixIteration += 1
                    }
                }

                is HorizontalCoverage -> {
                    var maxFixIteration = 0
                    val listUpper = getHigherPriorityHardConstraint(constrain.priority, constrain.id)
                    val listUpperIncludeCurrent =  (listUpper + constrain).toMutableList()
                    while (maxFixIteration < 100 && !isViolationHigherConstraint(listUpperIncludeCurrent, newSchedule)){
                        var temp = deepCopySolution(newSchedule)
                        temp = greedyCoverageHorizontalEnhancement(temp)
                        if (isViolationHigherConstraint(listUpper, temp)){
                            newSchedule = temp
                        }
                        maxFixIteration +=1
                    }
                }
            }
        }
        return newSchedule
    }

    private fun adjustPublicHolidays(schedule: MutableMap<String, MutableMap<Int, String>>) :MutableMap<String, MutableMap<Int, String>> {
        val schedules = deepCopySolution(schedule)
        for (staff in data.staffGroups.find{ it.id == "OPH" }!!.staffList) {
            var currentMonth = data.startDate.month
            var currentDay = data.startDate.day
            val totalDays = 7 * data.schedulePeriod
            var daysProcessed = 0
            while (daysProcessed < totalDays) {
                val daysInMonth = when (currentMonth) {
                    2 -> if (isLeapYear(data.startDate.year)) 29 else 28
                    4, 6, 9, 11 -> 30
                    else -> 31
                }

                if (currentDay > daysInMonth) {
                    currentDay = 1
                    currentMonth += 1

                    if (currentMonth > 12) currentMonth = 1
                }

                val isHoliday = data.publicHolidays.any { it.day == currentDay && it.month == currentMonth }
                if (isHoliday) {
                    schedules[staff]?.set(daysProcessed + 1, "PH")
                }
                currentDay += 1
                daysProcessed += 1
            }
        }
        return schedules
    }

    private fun shiftPatternEnhancement(schedule: MutableMap<String, MutableMap<Int, String>>) :MutableMap<String, MutableMap<Int, String>> {
        var nextScheduled = deepCopySolution(schedule)
        for (week in 1..data.schedulePeriod) {
            for (staff in data.staffs) {
                for (day in 1..7) {
                    if (checkPatternViolation(nextScheduled, week, day, staff.id)) {
                        for (constraint in data.patternConstrains) {
                            var maxFixIteration = 0
                            val listUpper = getHigherPriorityHardConstraint(constraint.priority, constraint.id)
                            var listUpperIncludeCurrent = (listUpper + constraint).toMutableList()
                            constraint.parsingPattern()
                            for (index in 1..constraint.patternLists.values.maxOf { it.size }) {
                                if (rule.checkPatternConstrainViolation(
                                        constraint,
                                        nextScheduled,
                                        week,
                                        day,
                                        staff.id
                                    )
                                ) {
                                    for (shift in data.shifts.filterNot { it.id == "PH" }) {
                                        val temp = deepCopySolution(nextScheduled)
                                        temp[staff.id]?.set(day + 7 * (week - 1) + index, shift.id)
                                        if (!rule.checkPatternConstrainViolation(
                                                constraint,
                                                temp,
                                                week,
                                                day,
                                                staff.id
                                            )
                                        ) {
                                            if (calculate.patternConstrainScore(temp) >= calculate.patternConstrainScore(
                                                    nextScheduled
                                                )
                                                && !checkPatternViolation(temp, week, day, staff.id)
                                                && isViolationHigherConstraint(listUpper, temp)
                                            ) {
                                                nextScheduled = temp
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return nextScheduled
    }

    private fun shakeHandEnhancement(schedule: MutableMap<String, MutableMap<Int, String>>) :MutableMap<String, MutableMap<Int, String>> {
        var nextScheduled = deepCopySolution(schedule)
        for (week in 1..data.schedulePeriod) {
            for (staff in data.staffs) {
                for (day in 1..7) {

                }
            }
        }
        return nextScheduled
    }

    fun runIteration(){
        var currentSolution = adjustPublicHolidays(initialSolution())
        this.bestSolution = deepCopySolution(currentSolution)
        createScoreOperator()
        createOperatorTimes()

        for (index in 1..this.numberIterations) {
            val operatorIndex = routeWheel(index)
            val nextSolution = shakeAndRepair(currentSolution, operatorIndex)
            currentSolution = calculateSimulatedAnnealing(currentSolution, nextSolution)
            if (calculate.totalScore(currentSolution) > calculate.totalScore(this.bestSolution)){
                this.bestSolution = deepCopySolution(currentSolution)
            }
        }
        this.bestSolution = adjustScheduleToConstrain(this.bestSolution)
        scoring()
    }
}