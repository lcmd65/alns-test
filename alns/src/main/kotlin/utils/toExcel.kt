package com.ft.aio.template.adapter.output.web.scrippt.utils

import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileOutputStream

class ToExcel {
    fun exportToExcel(solution: MutableMap<String, MutableMap<Int, String>>, filePath: String) {
        val workbook: Workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Solution")

        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Staff")
        for (i in 1..28) {
            headerRow.createCell(i).setCellValue("Day $i")
        }

        var rowIndex = 1
        for ((staff, schedule) in solution) {
            val row = sheet.createRow(rowIndex++)
            row.createCell(0).setCellValue(staff)

            for ((day, shift) in schedule) {
                row.createCell(day).setCellValue(shift)
            }
        }

        FileOutputStream(filePath).use { outputStream ->
            workbook.write(outputStream)
        }
        workbook.close()
    }
}
