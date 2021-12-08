package com.vtrixdigital.chatr.models

import android.app.PendingIntent
import android.app.PendingIntent.CanceledException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import java.util.*

class Action : Parcelable {
    val text: String?
    private val packageName: String?
    private val p: PendingIntent?
    private val isQuickReply: Boolean
    private val remoteInputs = ArrayList<RemoteInputParcel>()

    constructor(`in`: Parcel) {
        text = `in`.readString()
        packageName = `in`.readString()
        p = `in`.readParcelable(PendingIntent::class.java.classLoader)
        isQuickReply = `in`.readByte().toInt() != 0
        `in`.readTypedList(remoteInputs as List<Any>, RemoteInputParcel.CREATOR)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(text)
        dest.writeString(packageName)
        dest.writeParcelable(p, flags)
        dest.writeByte((if (isQuickReply) 1 else 0).toByte())
        dest.writeTypedList(remoteInputs)
    }

    constructor(action: NotificationCompat.Action, packageName: String?, isQuickReply: Boolean) {
        text = action.title.toString()
        this.packageName = packageName
        p = action.actionIntent
        if (action.remoteInputs != null) {
            val size = action.remoteInputs!!.size
            for (i in 0 until size) remoteInputs.add(RemoteInputParcel(action.remoteInputs!![i]))
        }
        this.isQuickReply = isQuickReply
    }

    @Throws(CanceledException::class)
    fun sendReply(context: Context?, msg: String?) {
        val intent = Intent()
        val bundle = Bundle()
        val actualInputs = ArrayList<RemoteInput>()
        for (input in remoteInputs) {
            bundle.putCharSequence(input.resultKey, msg)
            val builder = RemoteInput.Builder(input.resultKey)
            builder.setLabel(input.label)
            builder.setChoices(input.choices)
            builder.setAllowFreeFormInput(input.isAllowFreeFormInput)
            builder.addExtras(input.extras)
            actualInputs.add(builder.build())
        }
        val inputs = actualInputs.toTypedArray()
        RemoteInput.addResultsToIntent(inputs, intent, bundle)
        p!!.send(context, 0, intent)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Action> {
        override fun createFromParcel(parcel: Parcel): Action {
            return Action(parcel)
        }

        override fun newArray(size: Int): Array<Action?> {
            return arrayOfNulls(size)
        }
    }
}