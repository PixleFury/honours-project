package pixlefury.shoppingapp.activity.user

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.widget.ContentFrameLayout
import pixlefury.shoppingapp.AppConsts
import pixlefury.shoppingapp.R
import pixlefury.shoppingapp.activity.MainActivity
import pixlefury.shoppingapp.api.API

class RegisterActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_register)

		val prefs = getSharedPreferences(AppConsts.PREFS_FILE, 0)

		val root = findViewById<ContentFrameLayout>(android.R.id.content)
		val loadingSpinner = layoutInflater.inflate(R.layout.loading_spinner, root, false)
		loadingSpinner.visibility = View.GONE
		root.addView(loadingSpinner)

		findViewById<Button>(R.id.btn_register)
			.setOnClickListener {
				val username = findViewById<EditText>(R.id.et_new_username).text.toString()
				val password = findViewById<EditText>(R.id.et_new_password).text.toString()
				val passwordConfirmed = findViewById<EditText>(R.id.et_confirm_new_password).text.toString()

				if (password == passwordConfirmed) {
					loadingSpinner.visibility = View.VISIBLE
					API.register(username, password) {
						loadingSpinner.visibility = View.GONE
						if (it) {
							val editor = prefs.edit()
							editor.putString("username", username)
							editor.putString("password", password)
							editor.apply()

							startActivity(Intent(this, MainActivity::class.java))
							finish()
						}
					}
				}
			}
	}
}