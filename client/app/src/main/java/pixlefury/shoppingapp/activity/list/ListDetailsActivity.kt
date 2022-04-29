package pixlefury.shoppingapp.activity.list

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ContentFrameLayout
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pixlefury.shoppingapp.R
import pixlefury.shoppingapp.api.API
import pixlefury.shoppingapp.api.ShoppingList
import pixlefury.shoppingapp.api.User
import java.lang.Exception
import java.security.Permission

class ListDetailsActivity : AppCompatActivity() {
	companion object {
		const val SHOPPING_LIST = "shopping_list"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_list_details)

		val root = findViewById<ContentFrameLayout>(android.R.id.content)
		val loadingSpinner = layoutInflater.inflate(R.layout.loading_spinner, root, false)
		loadingSpinner.visibility = View.GONE
		root.addView(loadingSpinner)

		val listId = intent.extras?.getInt(SHOPPING_LIST) ?: -1
		val shoppingList = API.getShoppingList(listId) ?: throw Exception("Could not find list: $listId")

		findViewById<TextView>(R.id.tv_list_details_name).text = shoppingList.name

		findViewById<Button>(R.id.btn_edit_list_items).setOnClickListener {
			val intent = Intent(this, ListItemsActivity::class.java)
			intent.putExtra(ListItemsActivity.SHOPPING_LIST, shoppingList.id)
			startActivity(intent)
		}

		findViewById<Button>(R.id.btn_ready_list).setOnClickListener {
			val intent = Intent(this, ShopForListActivity::class.java)
			intent.putExtra(ShopForListActivity.SHOPPING_LIST, shoppingList.id)
			startActivity(intent)
		}

		findViewById<Button>(R.id.btn_delete_list).setOnClickListener {
			loadingSpinner.visibility = View.VISIBLE
			API.deleteShoppingList(listId) {
				finish()
			}
		}

		findViewById<Button>(R.id.btn_share_list).setOnClickListener {
			val views = layoutInflater.inflate(R.layout.dialog_share_list, null)

			val dialog =  AlertDialog.Builder(this)
				.setTitle(R.string.share_list)
				.setView(views)
				.create()
			dialog.show()

			val childClicked = {user: User ->
				API.shareListWithUser(shoppingList.id, user, ShoppingList.Permission.Shopper)
				dialog.cancel()
			}

			val users = API.getKnownUsers().toMutableList()
			val knownUsersRecycler = views.findViewById<RecyclerView>(R.id.rv_known_users)
			knownUsersRecycler.adapter = UsersAdapter(users, childClicked)
			knownUsersRecycler.layoutManager = LinearLayoutManager(this)

			val searchedUsersRecycler = views.findViewById<RecyclerView>(R.id.rv_searched_users)
			searchedUsersRecycler.adapter = UsersAdapter(mutableListOf(), childClicked)
			searchedUsersRecycler.layoutManager = LinearLayoutManager(this)

			views.findViewById<EditText>(R.id.et_search_user).addTextChangedListener { text ->
				if (text == null) return@addTextChangedListener
				if (text.toString().length < 3) return@addTextChangedListener

				API.searchForUsers(text.toString()) { users ->
					(searchedUsersRecycler.adapter as UsersAdapter).data.clear()
					(searchedUsersRecycler.adapter as UsersAdapter).data.addAll(users)
					searchedUsersRecycler.adapter?.notifyDataSetChanged()
				}
			}
		}
	}
}