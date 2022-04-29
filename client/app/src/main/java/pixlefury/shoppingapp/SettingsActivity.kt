package pixlefury.shoppingapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import pixlefury.shoppingapp.activity.MainActivity
import pixlefury.shoppingapp.activity.user.LoginActivity
import pixlefury.shoppingapp.api.API
import kotlin.system.exitProcess

class SettingsActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_settings)

		findViewById<Button>(R.id.btn_logout).setOnClickListener {
			val prefs = getSharedPreferences(AppConsts.PREFS_FILE, 0)
			prefs.edit().remove("username").remove("password").commit()
			API.logout()
			startActivity(Intent(this, MainActivity::class.java))
			finish()
		}
	}
}