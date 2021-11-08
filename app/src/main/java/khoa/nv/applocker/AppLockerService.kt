package khoa.nv.applocker

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.widget.Toast
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

class AppLockerService : Service() {

    private var foregroundAppDisposable: Disposable? = null
    private val allDisposables = CompositeDisposable()

    private var lastForegroundAppPackage: String? = null
    private val appForegroundObservable = AppForegroundObservable(this)

    private var screenOnOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> observeForegroundApplication()
                Intent.ACTION_SCREEN_OFF -> stopForegroundApplicationObserver()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        observeForegroundApplication()
        registerScreenReceiver()
    }

    override fun onDestroy() {
        stopForegroundApplicationObserver()
        unregisterScreenReceiver()
        if (allDisposables.isDisposed.not()) {
            allDisposables.dispose()
        }
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun observeForegroundApplication() {
        if (foregroundAppDisposable != null && foregroundAppDisposable?.isDisposed?.not() == true) {
            return
        }

        foregroundAppDisposable = appForegroundObservable
            .get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { foregroundAppPackage -> onAppForeground(foregroundAppPackage) },
                { error -> Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show() })
        allDisposables.add(foregroundAppDisposable!!)
    }

    private fun stopForegroundApplicationObserver() {
        if (foregroundAppDisposable != null && foregroundAppDisposable?.isDisposed?.not() == true) {
            foregroundAppDisposable?.dispose()
        }
    }

    private fun onAppForeground(foregroundAppPackage: String) {
        val intent = Intent(applicationContext, OverlayValidationActivity::class.java)
            .putExtra("KEY_PACKAGE_NAME", foregroundAppPackage)
        if (lastForegroundAppPackage == applicationContext.packageName) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        } else {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)

        lastForegroundAppPackage = foregroundAppPackage
    }

    private fun registerScreenReceiver() {
        val screenFilter = IntentFilter()
        screenFilter.addAction(Intent.ACTION_SCREEN_ON)
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOnOffReceiver, screenFilter)
    }

    private fun unregisterScreenReceiver() {
        unregisterReceiver(screenOnOffReceiver)
    }
}

