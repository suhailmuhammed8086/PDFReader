package com.example.pdfnotemate.tools.pdf.viewer.model

import android.os.Parcel
import android.os.Parcelable


open class PdfAnnotationModel(var type: Type, var paginationPageIndex: Int) : Parcelable {
    open var charDrawSegments = arrayListOf<CharDrawSegments>()

    constructor(parcel: Parcel) : this(
        Type.valueOf(parcel.readString() ?: Type.Note.name),
        parcel.readInt(),
    )

    enum class Type {
        Note, Highlight
    }

    fun asNote(): CommentModel? {
        if (this is CommentModel) {
            return this
        }
        return null
    }

    fun asHighlight(): HighlightModel? {
        if (this is HighlightModel) {
            return this
        }
        return null
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type.name)
        parcel.writeInt(paginationPageIndex)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PdfAnnotationModel> {
        override fun createFromParcel(parcel: Parcel): PdfAnnotationModel {
            return PdfAnnotationModel(parcel)
        }

        override fun newArray(size: Int): Array<PdfAnnotationModel?> {
            return arrayOfNulls(size)
        }
    }
}
