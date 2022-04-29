package pixlefury.shoppingapp.activity.user

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.ContentFrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import pixlefury.shoppingapp.AppConsts
import pixlefury.shoppingapp.R
import pixlefury.shoppingapp.activity.MainActivity
import pixlefury.shoppingapp.api.API

class LoginActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_login)

		val prefs = getSharedPreferences(AppConsts.PREFS_FILE, 0)

		val root = findViewById<ContentFrameLayout>(android.R.id.content)
		val loadingSpinner = layoutInflater.inflate(R.layout.loading_spinner, root, false)
		loadingSpinner.visibility = View.GONE
		root.addView(loadingSpinner)

		findViewById<Button>(R.id.btn_login)
			.setOnClickListener {
				val username = findViewById<EditText>(R.id.et_username).text.toString()
				val password = findViewById<EditText>(R.id.et_password).text.toString()

				loadingSpinner.visibility = View.VISIBLE

				API.login(username, password) {
					loadingSpinner.visibility = View.GONE
					if (it) {
						val editor = prefs.edit()
						editor.putString("username", username)
						editor.putString("password", password)
						editor.apply()

						startActivity(Intent(this, MainActivity::class.java))
						finish()
					} else {
						val editor = prefs.edit()
						editor.remove("username")
						editor.remove("password")
						editor.apply()
					}
				}
			}

		findViewById<TextView>(R.id.tv_no_account)
			.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }

		findViewById<TextView>(R.id.tv_reset_password)
			.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
	}
}