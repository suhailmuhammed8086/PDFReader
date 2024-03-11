package com.example.pdfnotemate.utils

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log

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
fun <T : Parcelable?> Intent.getParcelableExtraVs(
    key: String,
    java: Class<out T>
): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key,java)
    } else {
        getParcelableExtra(key)
    }
}
fun <T : Parcelable?> Bundle.getParcelableVs(
    key: String,
    java: Class<out T>
): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key,java)
    } else {
        getParcelable(key)
    }
}

fun <T> T.log(message: ((value: T) -> String)? = null): T {
    Log.e("QuickLog", message?.invoke(this) ?: "value print : $this")
    return this
}
fun <T> T.log(name: String): T {
    Log.e("QuickLog", " $name : $this")
    return this
}
