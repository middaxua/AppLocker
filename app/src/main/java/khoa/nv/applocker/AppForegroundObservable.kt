package khoa.nv.applocker

import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import io.reactivex.rxjava3.core.Flowable
import java.util.concurrent.TimeUnit

class AppForegroundObservable(private val context: Context) {

    fun get(): Flowable<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> getForegroundObservableHigherLollipop()
            else -> getForegroundObservableLowerLollipop()
        }

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun getForegroundObservableHigherLollipop(): Flowable<String> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        return Flowable.interval(100, TimeUnit.MILLISECONDS)
            .filter { PermissionChecker.checkUsageAccessPermission(context) }
            .map {
                val time = System.currentTimeMillis()
                val usageEvents = usm.queryEvents(time - 3600 * 1000, time)
                var usageEvent: UsageEvents.Event? = null
                val event = UsageEvents.Event()
                while (usageEvents.hasNextEvent()) {
                    usageEvents.getNextEvent(event)
                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        usageEvent = event
                    }
                }
                UsageEventWrapper(usageEvent)
            }
            .filter { it.usageEvent != null }
            .map { it.usageEvent!! }
            .filter { it.className != null && it.className.contains(OverlayValidationActivity::class.java.simpleName).not() }
            .map { it.packageName }
            .distinctUntilChanged()
    }

    private fun getForegroundObservableLowerLollipop(): Flowable<String> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return Flowable.interval(100, TimeUnit.MILLISECONDS)
            .map {
                am.getRunningTasks(1)[0].topActivity!!
            }
            .filter { it.className.contains(OverlayValidationActivity::class.java.simpleName).not() }
            .map { it.packageName }
            .distinctUntilChanged()
    }
}