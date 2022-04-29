package pixlefury.shoppingapp.activity.list

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ContentFrameLayout
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pixlefury.shoppingapp.R
import pixlefury.shoppingapp.api.API

class ListsActivity : AppCompatActivity() {
	override fun onRestart() {
		super.onRestart()
		startActivity(intent)
		finish()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_lists)

		val root = findViewById<ContentFrameLayout>(android.R.id.content)
		val loadingSpinner = layoutInflater.inflate(R.layout.loading_spinner, root, false)
		loadingSpinner.visibility = View.GONE
		root.addView(loadingSpinner)

		val ownedLists = API.getOwnedShoppingLists()
		val sharedLists = API.getSharedShoppingLists()

		val sections = mutableListOf<Pair<Int, Int>>()
		// Only add header if there is items in section
		var offset = 0
		if (ownedLists.isNotEmpty()) {
			sections.add(Pair(R.string.my_lists, 0))
			offset = 1
		}
		if (sharedLists.isNotEmpty()) sections.add(Pair(R.string.shared_lists, ownedLists.size + offset))

		val myListsRecycler = findViewById<RecyclerView>(R.id.rv_shopping_lists)
		myListsRecycler.adapter = ShoppingListAdapter(ownedLists + sharedLists, sections)
		myListsRecycler.layoutManager = LinearLayoutManager(this)

		findViewById<Button>(R.id.btn_create_list).setOnClickListener {
			val views = layoutInflater.inflate(R.layout.dialog_create_list, null)

			val etListName = views.findViewById<EditText>(R.id.et_list_name)

			val dialog =  AlertDialog.Builder(this)
				.setTitle(R.string.create_list)
				.setView(views)
				.setPositiveButton(R.string.create_list, DialogInterface.OnClickListener { dialog, id ->
					loadingSpinner.visibility = View.VISIBLE
					val listName = etListName.text.toString()
					API.createShoppingList(listName) {
						startActivity(intent)
						finish()
					}
				})
				.create()

			etListName.addTextChangedListener {it
				dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = it!!.length >= 3
			}

			dialog.show()
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
		}
	}
}