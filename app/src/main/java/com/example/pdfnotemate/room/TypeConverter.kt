package com.example.pdfnotemate.room

import androidx.room.TypeConverter
import com.example.pdfnotemate.tools.pdf.viewer.model.Coordinates
import com.google.gson.Gson

object TypeConverter {
    @TypeConverter
    fun toPdfCoordinates(json: String?): Coordinates? {
        if (json.isNullOrEmpty()) return null
        return Gson().fromJson(json, Coordinates::class.java)
    }

    @TypeConverter
    fun fromPdfCoordinates(topic: Coordinates?): String? {
        if (topic == null) return null
        return Gson().toJson(topic)
    }
}