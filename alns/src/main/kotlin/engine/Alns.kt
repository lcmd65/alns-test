package com.ft.aio.template.adapter.output.web.scrippt.engine

import com.ft.aio.template.adapter.output.web.scrippt.constrain.PatternConstrain
import com.ft.aio.template.adapter.output.web.scrippt.staff.Staff
import com.ft.aio.template.adapter.output.web.scrippt.input.InputData
import com.ft.aio.template.adapter.output.web.scrippt.rule.RuleViolation

// Adaptive large neighborhood search
// **@author: Dat Le

import kotlin.random.Random
import kotlin.math.exp

open class Alns(var data: InputData) {
    var numberIterations: Int = 1000
    var temperature: Double = 100.0
    var alpha: Double = 0.9
    var limit: Double = 1e-3
    var deltaE: Double = 0.0
    var score: Double = 0.0
    var penalty: Double = Int.MAX_VALUE.toDouble()
    var probabilitiesOfOperator: MutableMap<Int, Double> = mutableMapOf()
    var operatorScore: MutableMap<Int, Double> = mutableMapOf()
    var operatorWeight: MutableMap<Int, Double> = mutableMapOf()
    var operatorTimes: MutableMap<Int, Double> = mutableMapOf()
    var bestSolution: MutableMap<String, MutableMap<Int, String>> = mutableMapOf()
    var coverageFullFill: MutableMap<String, MutableMap<Int, String>> = mutableMapOf()
    var horizontalCoverageFullFill: MutableMap<String, MutableMap<Int, MutableMap<String, Int>>> = mutableMapOf()
    var calculate = CommonCaculate(data)
    var rule = RuleViolation(data)

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
            if (this.operatorWeight.get(index) != null){
                this.operatorWeight.set(
                    index,
                    this.operatorWeight.get(index)!! * 0.2 + 0.8 * this.operatorScore?.get(index)!! / this.operatorTimes?.get(index)!!)
            }
            else{
                this.operatorWeight.set(
                    index,
                    0.2 + 0.8 * this.operatorScore?.get(index)!! / this.operatorTimes?.get(index)!!)
            }
        }
    }

    private fun resetWeightOperators(){
        for(index in 0..4) {
            this.operatorWeight.set(
                index,
                0.2 + 0.8 * this.operatorScore?.get(index)!! / this.operatorTimes?.get(index)!!
            )
        }
    }

    private fun createScoreOperator() {
        this.operatorScore.set(0, 0.2)
        this.operatorScore.set(1, 0.1)
        this.operatorScore.set(2, 0.1)
        this.operatorScore.set(3, 0.1)
        this.operatorScore.set(4, 0.5)
    }

    private fun createOperatorTimes() {
        for (index in 0..4) {
            this.operatorTimes.set(index, 1.0)
        }
    }

    private fun calculateSimulatedAnealing(
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

    private fun caculateCoverageFullfillment(
        schedules: MutableMap<String, MutableMap<Int, String>>,
        coverageId: String,
        dayId: Int,
        week: Int
    ): Int {
        var coverage = data.coverages.find { it.id == coverageId && it.day == dayId }
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

    private fun caculateHorizontalCoverageFullfillment(
        schedules: MutableMap<String, MutableMap<Int, String>>,
        coverageId: Int
    ): MutableMap<Int, MutableMap<String, Int>>{
        var horizontalMap : MutableMap<Int, MutableMap<String, Int>> = mutableMapOf()
        for (week in 1 .. data.schedulePeriod){
            var temp: MutableMap<String, Int> = mutableMapOf()
            val coverage = data.horizontalCoverages.find { it.id == coverageId }
            for (staff in data.staffs){
                temp.set(staff.id, 0)
                if (coverage != null) {
                    for (day in coverage.days) {
                        if (coverage != null) {
                            if (schedules[staff.id]?.get(day + 7*(week-1))!! in coverage.shifts && day in coverage.days) {
                                temp.set(staff.id, temp.get(staff.id)!! + 1)
                            }
                        }
                    }
                }
            }
            horizontalMap.set(week, temp)
        }
        return horizontalMap
    }

    private fun initialSolution(): MutableMap<String, MutableMap<Int, String>> {
        var schedule : MutableMap<String, MutableMap<Int, String>>
        schedule = mutableMapOf()

        // create blank schedule for caculating
        for (staff in data.staffs) {
            schedule[staff.id] = mutableMapOf()
            for (day in 1..7 * data.schedulePeriod){
                schedule[staff.id]?.set(day, "")
            }
        }

        for (week in 1..data.schedulePeriod) {
            for (coverage in data.coverages) {
                for (staff in data.staffs) {
                    if (caculateCoverageFullfillment(schedule, coverage.id, coverage.day, week) < coverage.desireValue &&
                        checkIfStaffInStaffGroup(staff, coverage.staffGroups) &&
                        checkIfStaffInStaffGroup(staff, coverage.staffGroups) &&
                        schedule[staff.id]?.get(coverage.day + 7*(week - 1)) == ""
                    ) {
                        schedule[staff.id]?.set(coverage.day + 7*(week - 1), coverage.shift.random())
                    }
                }
            }
            for (coverage in data.coverages) {
                for (staff in data.staffs) {
                    if (schedule[staff.id]?.get(coverage.day + +7*(week - 1)) == "") {
                        schedule[staff.id]?.set(coverage.day + 7*(week - 1), data.shifts.filterNot { it.id == "PH" }.random().id)
                    }
                }
            }
        }
        return schedule
    }

    private fun randomDestroySolution(schedules: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>> {
        val mutableSchedule = deepCopySolution(schedules)
        if (mutableSchedule.isNotEmpty()) {

            val randomScheduleStaff = mutableSchedule.keys.random()
            val randomScheduleDay = mutableSchedule[randomScheduleStaff]?.keys?.random()
            if (randomScheduleDay != null) {
                mutableSchedule[randomScheduleStaff]?.set(randomScheduleDay.toInt(), "")
            }
        }
        return mutableSchedule
    }

    private fun repairSolution(schedules: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>> {
        var repairedSchedule = deepCopySolution(schedules)

        for (staffId in repairedSchedule.keys) {
            for (dayId in repairedSchedule[staffId]!!.keys) {
                if (repairedSchedule[staffId]?.get(dayId) == ""){
                    repairedSchedule[staffId]?.set(dayId, data.shifts.filterNot { it.id == "PH" }.random().id)
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
        var bestSchedule = deepCopySolution(schedules)
        var scoreTemp : MutableMap<MutableMap<String, MutableMap<Int, String>>, Double> = mutableMapOf()
        for (week in 1.. data.schedulePeriod) {
            data.coverages.forEach { coverage ->
                if (coverage.type.contains("equal to")){
                    val currentFulfillment = caculateCoverageFullfillment(schedules, coverage.id, coverage.day, week)
                    if (currentFulfillment < coverage.desireValue) {
                        data.staffs.forEach { staff ->
                            if (checkIfStaffInStaffGroup(staff, coverage.staffGroups)) {
                                coverage.shift.filterNot { it == "PH" }.forEach { shift ->
                                    val tempSolution =
                                        schedules.mapValues { (_, shifts) -> shifts.toMutableMap() }.toMutableMap()
                                    tempSolution[staff.id]?.set(coverage.day + 7 * (week - 1), shift)
                                    scoreTemp.set(tempSolution, calculate.coverageScore(tempSolution))
                                    if (calculate.coverageScore(schedules) < calculate.coverageScore(tempSolution)) {
                                        return tempSolution
                                    }
                                }
                            }
                        }
                    }
                    else if (currentFulfillment > coverage.desireValue) {
                        data.staffs.forEach { staff ->
                            if (checkIfStaffInStaffGroup(staff, coverage.staffGroups)) {
                                if (schedules[staff.id]?.get(coverage.day + 7 * (week - 1)) in coverage.shift) {
                                    var tempSolution = deepCopySolution(schedules)
                                    for (shiftFill in data.shifts){
                                        if(shiftFill.id !in coverage.shift && shiftFill.id != "PH"){
                                            tempSolution[staff.id]?.set(coverage.day +7*(week - 1), shiftFill.id)
                                            scoreTemp.set(tempSolution, calculate.coverageScore(tempSolution))
                                            if(calculate.coverageScore(schedules) < calculate.coverageScore(tempSolution)) {
                                                return tempSolution
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else if(coverage.type.contains("at least")){
                    val currentFulfillment = caculateCoverageFullfillment(schedules, coverage.id, coverage.day, week)
                    if (currentFulfillment < coverage.desireValue) {
                        var scoreTemp : MutableMap<MutableMap<String, MutableMap<Int, String>>, Double> = mutableMapOf()
                        data.staffs.forEach { staff ->
                            if (checkIfStaffInStaffGroup(staff, coverage.staffGroups)) {
                                coverage.shift.filterNot { it == "PH"}.forEach { shift ->
                                    var tempSolution = deepCopySolution(schedules)
                                    tempSolution[staff.id]?.set(coverage.day + 7 * (week - 1), shift)
                                    scoreTemp.set(tempSolution, calculate.coverageScore(tempSolution))
                                    if (calculate.coverageScore(schedules) < calculate.coverageScore(tempSolution)) {
                                        return tempSolution
                                    }
                                }
                            }
                        }
                    }
                }
                else if(coverage.type.contains("at most")){
                    val currentFulfillment = caculateCoverageFullfillment(schedules, coverage.id, coverage.day, week)
                    if (currentFulfillment > coverage.desireValue) {
                        data.staffs.forEach { staff ->
                            if (checkIfStaffInStaffGroup(staff, coverage.staffGroups)) {
                                coverage.shift.filterNot { it == "PH"}.forEach { shift ->
                                    var tempSolution = deepCopySolution(schedules)
                                    tempSolution[staff.id]?.set(coverage.day + 7 * (week - 1), shift)
                                    scoreTemp.set(tempSolution, calculate.coverageScore(tempSolution))
                                    if (calculate.coverageScore(schedules) < calculate.coverageScore(tempSolution)) {
                                        return tempSolution
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
        var newSchedule = deepCopySolution(schedules)
        var shiftSwap = ""
        if (newSchedule.isNotEmpty()) {
            data.staffs.forEach{ staff ->
                for (week in 1 .. data.schedulePeriod){
                    for(index1 in 1..7){
                        shiftSwap = newSchedule[staff.id]?.get(index1 + 7* (week - 1)).toString()

                        for (index2 in 1.. 7){
                            var tempSchedule = deepCopySolution(newSchedule)
                            tempSchedule[staff.id]?.set(index1 + 7* (week - 1), tempSchedule[staff.id]?.get(index2 + 7* (week - 1))!!)
                            tempSchedule[staff.id]?.set(index2 + 7* (week - 1), shiftSwap)
                            if (CommonCaculate(data).totalScore(tempSchedule) > CommonCaculate(data).totalScore(newSchedule)){
                                return tempSchedule
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
            val fullHorizontalCoverFullFill = caculateHorizontalCoverageFullfillment(schedules, horizontalCover.id)
            for ((week, horizontalCoverFullFill) in fullHorizontalCoverFullFill){
                for (item in horizontalCoverFullFill) {
                    if (horizontalCover.type.contains("at least")){
                        if (item.value < horizontalCover.desireValue) {
                            for (shift in horizontalCover.shifts) {
                                 horizontalCover.days.forEach { day ->
                                    val tempSolution = schedules.mapValues { (_, shifts) -> shifts.toMutableMap() }.toMutableMap()
                                    tempSolution[item.key]?.set(day + 7*(week - 1), shift)
                                    if (calculate.coverageScore(schedules) < calculate.coverageScore(tempSolution)) {
                                        return tempSolution
                                    }
                                }
                            }
                        }
                    }
                    else if (horizontalCover.type.contains("equal to")){
                        if(item.value < horizontalCover.desireValue){
                            for (shift in horizontalCover.shifts){
                                for (day in horizontalCover.days) {
                                    val tempSolution = schedules.mapValues { (_, shifts) -> shifts.toMutableMap() }.toMutableMap()
                                    tempSolution[item.key]?.set(day + 7*(week - 1), shift)
                                    if (calculate.coverageScore(schedules) < calculate.coverageScore(tempSolution)) {
                                        return tempSolution
                                    }
                                }
                            }
                        }
                        else if(item.value > horizontalCover.desireValue){
                            for (shift in horizontalCover.shifts){
                                for (day in horizontalCover.days) {
                                    if (schedules[item.key]?.get(day + 7*(week - 1)) in horizontalCover.shifts){
                                        for (shiftFill in data.shifts){
                                            if(shiftFill.id !in horizontalCover.shifts && shiftFill.id != "PH"){
                                                val tempSolution = schedules.mapValues { (_, shifts) -> shifts.toMutableMap() }.toMutableMap()
                                                tempSolution[item.key]?.set(day +7*(week - 1), shiftFill.id)
                                                if(calculate.coverageScore(schedules) < calculate.coverageScore(tempSolution)) {
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
                                        for (shiftFill in data.shifts){
                                            if(shiftFill.id !in horizontalCover.shifts && shiftFill.id != "PH"){
                                                val tempSolution = schedules.mapValues { (_, shifts) -> shifts.toMutableMap() }.toMutableMap()
                                                tempSolution[item.key]?.set(day +7*(week - 1), shiftFill.id)
                                                if(calculate.coverageScore(schedules) < calculate.coverageScore(tempSolution)) {
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

    fun adjustScheduleToConstrain(schedule: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>> {
        var newSchedule = deepCopySolution(schedule)
        for (constrain in data.constrains) {
            when (constrain.id) {
                "exactly-staff-working-time" -> {
                    for (week in 1..  data.schedulePeriod) {
                        var input : MutableMap<String, Double> = mutableMapOf()

                        for (staff in data.staffs) {
                            var staffWokringTime : Double = 0.0
                            for (day in 1 .. 7){
                                staffWokringTime += data.shifts.find { it.id == newSchedule[staff.id]?.get(day + 7*(week - 1))}?.duration!!
                            }
                            input.set(staff.id, staffWokringTime)
                        }
                        for ((key, value) in input){
                            if (value.toInt() != 44){
                                if(key == "Staff_1" || key == "Staff_3" || key == "Staff_6"){
                                    var countDuration : MutableMap<Int, Int> = mutableMapOf()
                                    countDuration.set(8, 0)
                                    countDuration.set(7, 0)
                                    countDuration.set(4, 0)
                                    countDuration.set(0, 0)
                                    for (day in 1 .. 7){
                                        if (data.shifts.find { it.id == newSchedule[key]?.get(day + 7*(week - 1))}?.duration!! == 7){
                                            if(newSchedule[key]?.get(day + 7*(week - 1))!! in data.shiftGroups.find{it.id == "AF"}?.shifts!!){
                                                newSchedule[key]?.set(day + 7*(week - 1), "A1")
                                            }
                                            else if (newSchedule[key]?.get(day + 7*(week - 1))!! in data.shiftGroups.find{it.id == "MO"}?.shifts!!){
                                                newSchedule[key]?.set(day + 7*(week - 1), "M1")
                                            }
                                        }
                                        countDuration.set(data.shifts.find { it.id == newSchedule[key]?.get(day + 7*(week - 1))}?.duration!!,
                                            countDuration.get(data.shifts.find { it.id == newSchedule[key]?.get(day + 7*(week - 1))}?.duration!!)!! + 1)
                                    }
                                    if (countDuration.get(4) == 0){
                                        newSchedule = greedySwapAHalfShiftWithBestScore(newSchedule, key, week)
                                    }
                                }
                                else {
                                    var countDuration : MutableMap<Int, Int> = mutableMapOf()
                                    countDuration.set(8, 0)
                                    countDuration.set(7, 0)
                                    countDuration.set(4, 0)
                                    countDuration.set(0, 0)
                                    for (day in 1 .. 7){
                                        countDuration.set(data.shifts.find { it.id == newSchedule[key]?.get(day + 7*(week - 1))}?.duration!!,
                                            countDuration.get(data.shifts.find { it.id == newSchedule[key]?.get(day + 7*(week - 1))}?.duration!!)!! + 1)
                                    }
                                    if (countDuration.get(8)!! < 2){
                                        for (index in 1..2 - countDuration.get(8)!!){
                                            newSchedule = greedySwapToMaxDurationShiftWithBestScore(newSchedule, key, week)
                                        }
                                    }
                                    else if (countDuration.get(8)!! < 2){
                                        for (index in 1..countDuration.get(8)!! - 2){
                                            newSchedule = greedySwapToSevenHoursDurationShiftWithBestScore(newSchedule, key, week)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "archive-0.5-day" -> {
                    for (week in 1..  data.schedulePeriod) {
                        var numberOfHalfShift : MutableMap<String, Int> = mutableMapOf()
                        for (staff in data.staffs) {
                            numberOfHalfShift.set(staff.id, 0)
                            for (day in 1 .. 7){
                                if(data.shifts.find { it.id == newSchedule[staff.id]?.get(day + 7*(week - 1))}?.duration!! == 4){
                                    numberOfHalfShift.set(staff.id, numberOfHalfShift.get(staff.id)!!+1)
                                }
                            }
                        }
                        for ((key, value) in numberOfHalfShift){
                            if (value < 1) {
                                newSchedule = greedySwapAHalfShiftWithBestScore(newSchedule, key, week)
                            }
                            else if (value > 1){
                                for (index in 1 .. value - 1){
                                    newSchedule = greedyDestroyAHalfShiftWithBestScore(newSchedule, key, week)
                                }
                            }
                        }
                    }
                }

                "un-archive-0.5-day" -> {
                    for (week in 1..  data.schedulePeriod) {
                        var numberOfHalfShift : MutableMap<String, Int> = mutableMapOf()
                        for (staff in data.staffs) {
                            numberOfHalfShift.set(staff.id, 0)
                            for (day in 1 .. 7){
                                if(data.shifts.find { it.id == newSchedule[staff.id]?.get(day + 7*(week - 1))}?.duration!! == 4){
                                    numberOfHalfShift.set(staff.id, numberOfHalfShift.get(staff.id)!!+1)
                                }
                            }
                        }
                        for ((key, value) in numberOfHalfShift){
                            if (value > 0){
                                for (index in 1 .. value - 1){
                                    newSchedule = greedyDestroyAHalfShiftWithBestScore(newSchedule, key, week)
                                }
                            }
                        }
                    }
                }
            }
        }
        return newSchedule
    }

    private fun greedySwapAHalfShiftWithBestScore(schedule: MutableMap<String, MutableMap<Int, String>>, key: String, week: Int):  MutableMap<String, MutableMap<Int, String>>{
        var listSchedule : MutableMap<MutableMap<String, MutableMap<Int, String>>, Double> = mutableMapOf()
        for (day in 1 .. data.schedulePeriod){
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
                var temp = deepCopySolution(schedule)
                temp[key]?.set(day + 7 * (week - 1), shiftInfo)
                listSchedule.set(temp, calculate.constrainScore(temp))
            }
        }
        val bestSchedule = listSchedule.maxByOrNull { it.value }?.key
        return bestSchedule ?: schedule
    }

    private fun greedyDestroyAHalfShiftWithBestScore(schedule: MutableMap<String, MutableMap<Int, String>>, key: String, week: Int):  MutableMap<String, MutableMap<Int, String>>{
        var listSchedule : MutableMap<MutableMap<String, MutableMap<Int, String>>, Double> = mutableMapOf()
        for (day in 1 .. data.schedulePeriod){
            var shiftInfo = schedule[key]?.get(day + 7*(week - 1))!!
            if (data.shifts.find { it.id == shiftInfo }!!.duration == 4) {
                for (shift in data.shifts){
                    if (shift.id != "DO" && shift.id != "PH" && shift.duration != 4){
                        var temp = deepCopySolution(schedule)
                        temp[key]?.set(day + 7*(week - 1), shift.id)
                        listSchedule.set(temp, calculate.constrainScore( temp))
                    }
                }
            }
        }
        val bestSchedule = listSchedule.maxByOrNull { it.value }?.key
        return bestSchedule ?: schedule
    }

    private fun greedySwapToMaxDurationShiftWithBestScore(schedule: MutableMap<String, MutableMap<Int, String>>, key: String, week: Int):  MutableMap<String, MutableMap<Int, String>>{
        var listSchedule : MutableMap<MutableMap<String, MutableMap<Int, String>>, Double> = mutableMapOf()
        for (day in 1 .. data.schedulePeriod){
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
                var temp = deepCopySolution(schedule)
                temp[key]?.set(day + 7 * (week - 1), shiftInfo)
                listSchedule.set(temp, calculate.constrainScore(temp))
            }

        }
        val bestSchedule = listSchedule.maxByOrNull { it.value }?.key
        return bestSchedule ?: schedule
    }

    private fun greedySwapToSevenHoursDurationShiftWithBestScore(schedule: MutableMap<String, MutableMap<Int, String>>, key: String, week: Int):  MutableMap<String, MutableMap<Int, String>>{
        var listSchedule : MutableMap<MutableMap<String, MutableMap<Int, String>>, Double> = mutableMapOf()
        for (day in 1 .. data.schedulePeriod){
            var shiftInfo = schedule[key]?.get(day + 7*(week - 1))!!
            if (shiftInfo == "DO" || shiftInfo == "PH"){
                continue
            }
            else{
                if (shiftInfo in data.shiftGroups.find{it.id == "AF"}?.shifts!! && shiftInfo != "A2" && shiftInfo != "DO" && shiftInfo != "PH"){
                    shiftInfo = "A2"
                }
                else if (shiftInfo in data.shiftGroups.find{it.id == "MO"}?.shifts!! && shiftInfo != "M2" && shiftInfo != "DO" && shiftInfo != "PH"){
                    shiftInfo = "M2"
                }
                var temp = deepCopySolution(schedule)
                temp[key]?.set(day + 7*(week - 1), shiftInfo)
                listSchedule.set(temp, calculate.constrainScore(temp))
            }
        }
        val bestSchedule = listSchedule.maxByOrNull { it.value }?.key
        return bestSchedule ?: schedule
    }

    private fun routeWheel(index: Int): Int{
        if (index % 400 == 0){
            resetWeightOperators()
        }
        else {
            createWeightOperators()
        }

        var rand = Random.nextDouble()
        var S = 0.0

        for (index in 0 until this.operatorWeight.size){
            S += this.operatorWeight.get(index)!!
        }
        this.probabilitiesOfOperator.set(0 , this.operatorWeight.get(0)!!/S)
        for (index in 1 until this.operatorWeight.size){
            this.probabilitiesOfOperator.set(index, this.probabilitiesOfOperator.get(index - 1)!! + this.operatorWeight.get(index)!!/S)
        }

        var choseValue = 0

        if (rand <= this.probabilitiesOfOperator.get(0)!!){
            choseValue = 0
        }
        else{
            for(index in 1 until this.operatorScore.size ){
                if (rand > this.probabilitiesOfOperator.get(index - 1)!! && rand <= this.probabilitiesOfOperator.get(index)!!){
                    choseValue = index
                }
            }
        }
        this.operatorTimes.set(choseValue, this.operatorTimes.get(choseValue)!! + 1)
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
        var newScheduled = deepCopySolution(schedules)
        for (week in 1 .. data.schedulePeriod){
            for (staff in data.staffs){
                for(day in 1 .. 7){
                    if (checkPatternViolation(newScheduled, week, day ,staff.id) == true){
                        return greedyFixedShiftPatternViolations(newScheduled, week, day, staff.id)
                    }
                }
            }
        }

        for (week in 1 .. data.schedulePeriod){
            for (staff in data.staffs){
                for(day in 1 .. 7){
                    if (checkConstrainViolation(newScheduled, week ,staff.id) == true){
                        return greedyFixedConstraintViolations(newScheduled, week, staff.id)
                    }
                }
            }
        }

        return newScheduled
    }

    private fun greedyFixedShiftPatternViolations(schedules: MutableMap<String, MutableMap<Int, String>>, week: Int, day: Int, staff: String):MutableMap<String, MutableMap<Int, String>>{
        var nextScheduled = deepCopySolution(schedules)
        for (constrain in data.patternConstrains) {
            for (index in 1..constrain.patternLists.values.maxOf { it.size }) {
                if (rule.checkPatternConstrainViolation(constrain, nextScheduled, week, day, staff) == true) {
                    for (shift in data.shifts.filterNot { it.id == "PH" }) {
                        var temp = deepCopySolution(nextScheduled)
                        temp[staff]?.set(day + 7 * (week - 1) + index, shift.id)
                        if (rule.checkPatternConstrainViolation(constrain, temp, week, day, staff) == false) {
                            if (calculate.constrainScore(temp) >= calculate.constrainScore(nextScheduled)
                                && calculate.patternConstrainScore(temp) >= calculate.patternConstrainScore(
                                    nextScheduled
                                )
                                && checkPatternViolation(temp, week, day, staff) == false
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
                            var staffWokringTime: Double = 0.0
                            for (day in 1..7) {
                                staffWokringTime += data.shifts.find { it.id == newSchedule[staff]?.get(day + 7 * (week - 1)) }?.duration!!
                            }
                            if (staffWokringTime.toInt() != 44) {
                                if (staff == "Staff_1" || staff == "Staff_3" || staff == "Staff_6") {
                                    var countDuration: MutableMap<Int, Int> = mutableMapOf()
                                    countDuration.set(8, 0)
                                    countDuration.set(7, 0)
                                    countDuration.set(4, 0)
                                    countDuration.set(0, 0)
                                    for (day in 1..7) {
                                        if (data.shifts.find { it.id == newSchedule[staff]?.get(day + 7 * (week - 1)) }?.duration!! == 7) {
                                            if (newSchedule[staff]?.get(day + 7 * (week - 1))!! in data.shiftGroups.find { it.id == "AF" }?.shifts!!) {
                                                newSchedule[staff]?.set(day + 7 * (week - 1), "A1")
                                            } else if (newSchedule[staff]?.get(day + 7 * (week - 1))!! in data.shiftGroups.find { it.id == "MO" }?.shifts!!) {
                                                newSchedule[staff]?.set(day + 7 * (week - 1), "M1")
                                            }
                                        }
                                        countDuration.set(
                                            data.shifts.find { it.id == newSchedule[staff]?.get(day + 7 * (week - 1)) }?.duration!!,
                                            countDuration.get(data.shifts.find {
                                                it.id == newSchedule[staff]?.get(
                                                    day + 7 * (week - 1)
                                                )
                                            }?.duration!!)!! + 1
                                        )
                                    }

                                    if (countDuration.get(4) == 0) {
                                        newSchedule = greedySwapAHalfShiftWithBestScore(newSchedule, staff, week)
                                    }
                                }
                                else {
                                    var countDuration: MutableMap<Int, Int> = mutableMapOf()
                                    countDuration.set(8, 0)
                                    countDuration.set(7, 0)
                                    countDuration.set(4, 0)
                                    countDuration.set(0, 0)
                                    for (day in 1..7) {
                                        countDuration.set(
                                            data.shifts.find { it.id == newSchedule[staff]?.get(day + 7 * (week - 1)) }?.duration!!,
                                            countDuration.get(data.shifts.find {
                                                it.id == newSchedule[staff]?.get(
                                                    day + 7 * (week - 1)
                                                )
                                            }?.duration!!)!! + 1
                                        )
                                    }
                                    if (countDuration.get(8)!! < 2) {
                                        for (index in 1..2 - countDuration.get(8)!!) {
                                            newSchedule = greedySwapToMaxDurationShiftWithBestScore(
                                                newSchedule,
                                                staff,
                                                week
                                            )
                                        }
                                    } else if (countDuration.get(8)!! > 2) {
                                        for (index in 1..countDuration.get(8)!! - 2) {
                                            newSchedule = greedySwapToSevenHoursDurationShiftWithBestScore(
                                                newSchedule,
                                                staff,
                                                week
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        "archive-0.5-day" -> {
                            var numberOfHalfShift: MutableMap<String, Int> = mutableMapOf()
                            numberOfHalfShift.set(staff, 0)
                            for (day in 1..7) {
                                if (data.shifts.find { it.id == newSchedule[staff]?.get(day + 7 * (week - 1)) }?.duration!! == 4) {
                                    numberOfHalfShift.set(staff, numberOfHalfShift.get(staff)!! + 1)
                                }
                            }
                            for ((key, value) in numberOfHalfShift) {
                                if (value < 1) {
                                    newSchedule = greedySwapAHalfShiftWithBestScore(newSchedule, key, week)
                                } else if (value > 1) {
                                    for (index in 1..value - 1) {
                                        newSchedule = greedyDestroyAHalfShiftWithBestScore(newSchedule, key, week)
                                    }
                                }
                            }
                            return newSchedule
                        }

                        "un-archive-0.5-day" -> {
                            var numberOfHalfShift: MutableMap<String, Int> = mutableMapOf()
                            numberOfHalfShift.set(staff, 0)
                            for (day in 1..7) {
                                if (data.shifts.find { it.id == newSchedule[staff]?.get(day + 7 * (week - 1)) }?.duration!! == 4) {
                                    numberOfHalfShift.set(staff, numberOfHalfShift.get(staff)!! + 1)
                                }
                            }
                            for ((key, value) in numberOfHalfShift) {
                                if (value > 0) {
                                    for (index in 1..value - 1) {
                                        newSchedule = greedyDestroyAHalfShiftWithBestScore(newSchedule, key, week)
                                    }
                                }
                            }
                            return newSchedule
                        }
                    }
                }
            }
            else {
                //TODO
            }
        }
        return newSchedule
    }

    private fun checkPatternViolation(schedules: MutableMap<String, MutableMap<Int, String>>, week: Int, day: Int, staff: String): Boolean{
        for (constrain in data.patternConstrains){
            if (rule.checkPatternConstrainViolation(constrain, schedules, week, day, staff) == true) {
                return true
            }
        }
        return false
    }

    private fun checkConstrainViolation(schedules: MutableMap<String, MutableMap<Int, String>>, week: Int, staff: String): Boolean{
        for (constrain in data.constrains){
            if (rule.checkConstrainViolation(constrain, schedules, week, staff) == true) {
                return false
            }
        }
        return true
    }

    fun runIteration(){
        var currentSolution = initialSolution()
        this.bestSolution = deepCopySolution(currentSolution)
        createScoreOperator()
        createOperatorTimes()

        for (index in 1..this.numberIterations) {
            val operatorIndex = routeWheel(index)
            var nextSolution = shakeAndRepair(currentSolution, operatorIndex)
            currentSolution = calculateSimulatedAnealing(currentSolution, nextSolution)
            if (calculate.totalScore(currentSolution) > calculate.totalScore(this.bestSolution)){
                this.bestSolution = deepCopySolution(currentSolution)
            }
        }
    }
}