package com.example.eatitserver.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.eatitserver.R
import com.example.eatitserver.model.CategoryModel
import com.example.eatitserver.model.FoodModel
import com.example.eatitserver.model.ServerUserModel
import com.example.eatitserver.model.TokenModel
import com.google.firebase.database.FirebaseDatabase
import java.util.*

object Common {
    val ORDER_REF = "Order"
    var foodSelected: FoodModel?=null
    val FULL_WITDTH_COLUMN: Int = 1
    val DEFAULT_COLUMN_COUNT: Int = 0
    var categorySelected: CategoryModel? = null
    val CATEGORY_REF = "Category"
    val SERVER_REF= "Server"
    var currenServerUser: ServerUserModel?=null
    val TOKEN_REF = "Tokens"
    val NOTI_TITLE = "title"
    val NOTI_CONTENT = "content"
    var SESSION_TOKEN: String = ""

    fun setSpanString(welcome: String, name: String?, txtUser: TextView?) {
        val buider = SpannableStringBuilder()
        buider.append(welcome)
        val txtSpannable = SpannableString(name)
        val boldSpan = StyleSpan(Typeface.BOLD)
        txtSpannable.setSpan(boldSpan, 0, name!!.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        buider.append(txtSpannable)
        txtUser!!.setText(buider, TextView.BufferType.SPANNABLE)
    }

    fun setSpanStringColor(welcome: String, name: String?, txtUser: TextView?, color: Int) {
        val buider = SpannableStringBuilder()
        buider.append(welcome)
        val txtSpannable = SpannableString(name)
        val boldSpan = StyleSpan(Typeface.BOLD)
        txtSpannable.setSpan(boldSpan, 0, name!!.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        txtSpannable.setSpan(
            ForegroundColorSpan(color),
            0,
            name!!.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        buider.append(txtSpannable)
        txtUser!!.setText(buider, TextView.BufferType.SPANNABLE)
    }

    fun convertStatusToString(orderStatus: Int): String? =
        when (orderStatus) {
            0 -> "Placed"
            1 -> "Shipping"
            2 -> "Shipped"
            -1 -> "Cancelled"
            else -> "Error"
        }

    fun updateToken() {
        FirebaseDatabase.getInstance()
            .getReference(Common.TOKEN_REF)
            .child(currenServerUser!!.uid!!)
            .setValue(TokenModel(currenServerUser!!.uid!!, Common.SESSION_TOKEN))
            .addOnFailureListener { e ->
                Log.d("Token Error", e.message)
            }
    }

    fun getNewOrderTopic(): String {
        return java.lang.StringBuilder("/topics/new_order").toString()
    }

    fun createOrderNumber(): String {
        return java.lang.StringBuilder()
            .append(System.currentTimeMillis())
            .append(Math.abs(Random().nextInt()))
            .toString()
    }

    fun showNotification(
        context: Context,
        id: Int,
        title: String?,
        content: String?,
        intent: Intent?
    ) {
        var pendingIntent: PendingIntent? = null
        if (intent != null)
            pendingIntent =
                PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val NOTIFICATION_CHANEL_ID = "com.example.eatitclient"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANEL_ID,
                "Eat IT v2", NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.description = "Eat It v2"
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
            notificationChannel.lightColor = (Color.RED)
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)

            notificationManager.createNotificationChannel(notificationChannel)
        }
        val buider = NotificationCompat.Builder(context, NOTIFICATION_CHANEL_ID)

        buider.setContentTitle(title!!).setContentText(content!!).setAutoCancel(true)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_restaurant_menu_black_24dp
                )
            )

        if (pendingIntent != null)
            buider.setContentIntent(pendingIntent)
        val notification = buider.build()

        notificationManager.notify(id, notification)
    }
}