package khoa.nv.applocker

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import khoa.nv.applocker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.sw.isChecked = PermissionChecker.checkUsageAccessPermission(this)
    }

    fun startService(view: android.view.View) {
        startService(Intent(this, AppLockerService::class.java))
    }

    fun checkUAP(view: android.view.View) {
        startActivityForResult(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), 123)
    }

    fun stopService(view: android.view.View) {
        stopService(Intent(this, AppLockerService::class.java))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123) {
            binding.sw.isChecked = PermissionChecker.checkUsageAccessPermission(this)
        }
    }
}