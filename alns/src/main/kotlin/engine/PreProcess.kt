package com.ft.aio.template.adapter.output.web.scrippt.engine
import com.ft.aio.template.adapter.output.web.scrippt.utils.ReadJSON
import com.ft.aio.template.adapter.output.web.scrippt.input.InputData
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


open class PreProcess {
    fun dataPreprocessing():InputData{
        var jsonString = ReadJSON().readJson("input/data_dummy.json")

        val data = Gson().fromJson(jsonString, InputData:: class.java)
        return data
    }
}