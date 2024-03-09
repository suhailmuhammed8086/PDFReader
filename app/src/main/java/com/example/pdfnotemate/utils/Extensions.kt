package com.example.pdfnotemate.utils

import android.os.Build
import android.os.Bundle
import android.os.Parcelable

fun <T : Parcelable?> Bundle.getParcelableArrayListVs(
    key: String,
    java: Class<out T>
): java.util.ArrayList<T>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayList(key, java)
    } else {
        getParcelableArrayList(key)
    }
}