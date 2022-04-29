package pixlefury.shoppingapp.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.widget.ContentFrameLayout
import pixlefury.shoppingapp.activity.product.ProductSearchActivity
import pixlefury.shoppingapp.AppConsts.PREFS_FILE
import pixlefury.shoppingapp.R
import pixlefury.shoppingapp.SettingsActivity
import pixlefury.shoppingapp.activity.list.ListsActivity
import pixlefury.shoppingapp.activity.list.ShopForListActivity
import pixlefury.shoppingapp.activity.user.LoginActivity
import pixlefury.shoppingapp.api.API
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		API.init(applicationContext)

		if (!API.isSignedIn()) {
			val root = findViewById<ContentFrameLayout>(android.R.id.content)
			val loadingSpinner = layoutInflater.inflate(R.layout.loading_spinner, root, false)
			root.addView(loadingSpinner)

			val prefs = getSharedPreferences(PREFS_FILE, 0)
			val username = prefs.getString("username", "") as String
			val password = prefs.getString("password", "") as String
			API.login(username, password) {
				if (!it) {
					startActivity(Intent(this, LoginActivity::class.java))
					finish() // Prevents user from closing the login activity with the back button
				} else {
					loadingSpinner.visibility = View.GONE
				}
			}
		}

		val btnFind = findViewById<Button>(R.id.btn_find)
		btnFind.setOnClickListener {
			if (API.getOwnedShoppingLists().isNotEmpty()) {
				startActivity(Intent(this, ProductSearchActivity::class.java))
			}
		}

		findViewById<Button>(R.id.btn_lists)
			.setOnClickListener { startActivity(Intent(this, ListsActivity::class.java)) }

		findViewById<Button>(R.id.btn_settings)
			.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
	}
}