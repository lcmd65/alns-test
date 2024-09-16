package com.ft.aio.template.adapter.output.web.scrippt.engine

import com.ft.aio.template.adapter.output.web.scrippt.input.InputData

class CreateConObjs (val data: InputData) {

    fun createConstrainScore(schedule: MutableMap<String, MutableMap<Int, String>>){
        for (constrain in data.constrains){
            when(constrain.id) {
                "exactly-staff-working-time" -> {

                    var input : MutableMap<String, Double> = mutableMapOf()
                    for (week in 1..  data.schedulePeriod) {

                        for (staff in data.staffs) {
                            var staffWokringTime : Double = 0.0
                            for (day in 1 .. 7){
                                staffWokringTime += data.shifts.find { it.id == schedule[staff.id]?.get(day + 7*(week - 1))}?.duration!!
                            }
                            input.set(staff.id + week.toString(), staffWokringTime)
                        }
                    }
                    constrain.caculateScore(input)
                }

                "archive-0.5-day" -> {
                    var input : MutableMap<String, Double> = mutableMapOf()
                    for (week in 1..  data.schedulePeriod) {

                        for (staff in data.staffs) {
                            var staffWorkingTime : Double = 0.0
                            for (day in 1 .. 7){
                                var temp = data.shifts.find { it.id == schedule[staff.id]?.get(day + 7*(week - 1))}?.duration!!
                                if (temp != 4){
                                    staffWorkingTime += 1
                                }
                                else {
                                    staffWorkingTime += 0.5
                                }
                            }
                            input.set(staff.id + week.toString(), staffWorkingTime)
                        }
                    }
                    constrain.caculateScore(input)
                }

                "un-archive-0.5-day" -> {
                    var input : MutableMap<String, Double> = mutableMapOf()
                    for (week in 1..  data.schedulePeriod) {

                        for (staff in data.staffs) {
                            var staffWorkingTime : Double = 0.0
                            for (day in 1 .. 7){
                                var temp = data.shifts.find { it.id == schedule[staff.id]?.get(day + 7*(week - 1))}?.duration!!
                                if (temp != 4){
                                    staffWorkingTime += 1
                                }
                                else {
                                    staffWorkingTime += 0.5
                                }
                            }
                            input.set(staff.id + week.toString(), staffWorkingTime)
                        }
                    }
                    constrain.caculateScore(input)
                }
            }
        }
    }

    fun createPatternConstrainScore(){

    }
}