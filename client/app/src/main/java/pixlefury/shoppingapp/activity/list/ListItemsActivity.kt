package pixlefury.shoppingapp.activity.list

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pixlefury.shoppingapp.R
import pixlefury.shoppingapp.activity.product.ProductSearchActivity
import pixlefury.shoppingapp.api.API
import pixlefury.shoppingapp.api.Product
import pixlefury.shoppingapp.api.ShoppingList

class ListItemsActivity : AppCompatActivity() {
	companion object {
		const val SHOPPING_LIST = "shopping_list"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_list_items)

		val listId = intent.extras?.getInt(SHOPPING_LIST) ?: 0
		val shoppingList = API.getShoppingList(listId) ?: throw Exception("Could not find list: $listId")

		val items = mutableListOf<Pair<Product, ShoppingList.Item>>()

		for (barcode in shoppingList.items.keys) {
			items.add(Pair(API.getProduct(barcode), shoppingList.items[barcode]) as Pair<Product, ShoppingList.Item>)
		}

		val listItemsRecycler = findViewById<RecyclerView>(R.id.rv_list_items)
		listItemsRecycler.adapter = ListItemsAdapter(items, shoppingList)
		listItemsRecycler.layoutManager = LinearLayoutManager(this)

		findViewById<Button>(R.id.btn_save_list_items).setOnClickListener {
			val adapter = listItemsRecycler.adapter as ListItemsAdapter
			for (itemIndex in adapter.edited_items.keys) {
				val combo = adapter.data[itemIndex]
				val product = combo.first
				val listItem = combo.second
				API.modifyListItem(shoppingList.id, product.barcode, listItem.shop, listItem.quantity)
			}
			finish()
		}

		findViewById<Button>(R.id.btn_add_products).setOnClickListener {
			val intent = Intent(this, ProductSearchActivity::class.java)
			intent.putExtra(ProductSearchActivity.SHOPPING_LIST, shoppingList.id)
			startActivity(intent)
			finish()
		}
	}
}