package com.stopsmoke.kekkek.presentation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.flowWithLifecycle
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.snackbar.Snackbar
import com.stopsmoke.kekkek.R
import com.stopsmoke.kekkek.domain.model.DateTimeUnit
import com.stopsmoke.kekkek.domain.model.ElapsedDateTime
import com.stopsmoke.kekkek.domain.model.PostCategory
import com.stopsmoke.kekkek.domain.model.User
import com.stopsmoke.kekkek.domain.model.ProfileImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal fun ElapsedDateTime.toResourceId(context: Context): String =
    when (elapsedDateTime) {
        DateTimeUnit.YEAR -> "${number}${context.getString(R.string.notification_elapsed_year)}"
        DateTimeUnit.MONTH -> "${number}${context.getString(R.string.notification_elapsed_month)}"
        DateTimeUnit.WEEK -> "${number}${context.getString(R.string.notification_elapsed_week)}"
        DateTimeUnit.DAY -> "${number}${context.getString(R.string.notification_elapsed_day)}"
        DateTimeUnit.HOUR -> "${number}${context.getString(R.string.notification_elapsed_hour)}"
        DateTimeUnit.MINUTE -> "${number}${context.getString(R.string.notification_elapsed_minute)}"
        DateTimeUnit.SECOND -> "${number}${context.getString(R.string.notification_elapsed_second)}"
    }


internal fun<T> Flow<T>.collectLatest(
    lifecycleScope: LifecycleCoroutineScope,
    action: (value: T) -> Unit
): Job {
    return lifecycleScope.launch {
        this@collectLatest.collectLatest(action)
    }
}

internal fun<T> Flow<T>.collectLatestWithLifecycle(
    lifecycle: Lifecycle,
    action: suspend (value: T) -> Unit
): Job {
    return lifecycle.coroutineScope.launch {
        this@collectLatestWithLifecycle.flowWithLifecycle(lifecycle).collectLatest(action)
    }
}

internal fun<T> Bundle.getParcelableAndroidVersionSupport(key: String, clazz: Class<T>) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, clazz)
    } else {
        getParcelable(key)
    }

internal fun PostCategory.getResourceString(context: Context) = when(this) {
    PostCategory.NOTICE -> context.getString(R.string.community_category_notice)
    PostCategory.QUIT_SMOKING_SUPPORT -> context.getString(R.string.community_category_quit_smoking_support)
    PostCategory.POPULAR -> context.getString(R.string.community_category_popular)
    PostCategory.QUIT_SMOKING_AIDS_REVIEWS -> context.getString(R.string.community_category_quit_smoking_aids_reviews)
    PostCategory.SUCCESS_STORIES -> context.getString(R.string.community_category_success_stories)
    PostCategory.GENERAL_DISCUSSION -> context.getString(R.string.community_category_general_discussion)
    PostCategory.FAILURE_STORIES -> context.getString(R.string.community_category_failure_stories)
    PostCategory.RESOLUTIONS -> context.getString(R.string.community_category_resolutions)
    PostCategory.UNKNOWN -> context.getString(R.string.community_category_unknown)
    PostCategory.ALL -> context.getString(R.string.community_category_all)
}

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = connectivityManager.activeNetworkInfo
    return networkInfo != null && networkInfo.isConnected
}

internal fun View.snackbarLongShow(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_LONG).show()
}

internal fun ImageView.setDefaultProfileImage(profileImage: ProfileImage) {
    when(profileImage) {
        ProfileImage.Default -> setImageResource(R.drawable.ic_user_profile_test)
        is ProfileImage.Web -> load(profileImage.url)
    }
}


class CustomItemDecoration(private val color: Int, private val height: Int) : RecyclerView.ItemDecoration() {
    private val paint = Paint()

    init {
        paint.color = color
        paint.strokeWidth = height.toFloat()
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight

        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)

            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + height

            c.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
        }
    }
}


fun (User.Registered).getTotalDay(): Long{
    var totalDay:Long = 0
    this.history.historyTimeList.forEach {
        if(it.quitSmokingStartDateTime !=null){
            if(it.quitSmokingStopDateTime != null)  totalDay += ChronoUnit.DAYS.between(it.quitSmokingStartDateTime, it.quitSmokingStopDateTime)
            else if(it.quitSmokingStopDateTime == null) totalDay += ChronoUnit.DAYS.between(it.quitSmokingStartDateTime, LocalDateTime.now())
        }
    }
    return totalDay
}

internal fun View.hideSoftKeyboard() {
    val inputMethodManager = ContextCompat.getSystemService(context, InputMethodManager::class.java)
    inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
}