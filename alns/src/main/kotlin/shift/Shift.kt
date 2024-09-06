package com.ft.aio.template.adapter.output.web.scrippt.shift

class Shift {

    var id: Int = 0
    var name: String = ""
    var duration: Int = 0

    constructor ( id: Int,
                  name: String,
                  duration: Int) {

        this.id = id
        this.name = name
        this.duration = duration
    }
}