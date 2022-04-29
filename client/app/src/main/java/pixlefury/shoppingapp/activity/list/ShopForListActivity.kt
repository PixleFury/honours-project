package pixlefury.shoppingapp.activity.list

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import pixlefury.shoppingapp.R
import pixlefury.shoppingapp.api.API
import pixlefury.shoppingapp.api.Product
import pixlefury.shoppingapp.api.Shop
import pixlefury.shoppingapp.api.ShoppingList
import java.lang.Exception

class ShopForListActivity : AppCompatActivity() {
	companion object {
		const val SHOPPING_LIST = "shopping_list"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_shop_for_list)

		val listId = intent.extras?.getInt(SHOPPING_LIST) ?: -1
		val shoppingList = API.getShoppingList(listId) ?: throw Exception("Could not find list: $listId")

		val tabLayout = findViewById<TabLayout>(R.id.shop_for_list_tabs)

		// Generate shop map - {Shop => (Product, Item)}
		val shops = mutableMapOf<TabLayout.Tab, MutableList<Pair<Product, ShoppingList.Item>>>()

		val itemsRecycler = findViewById<RecyclerView>(R.id.rv_shop_for_list)
		itemsRecycler.adapter = TickListItemsAdapter(mutableListOf(), shoppingList)
		itemsRecycler.layoutManager = LinearLayoutManager(this)

		tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
			override fun onTabSelected(tab: TabLayout.Tab) {
				(itemsRecycler.adapter as TickListItemsAdapter).data.clear()
				(itemsRecycler.adapter as TickListItemsAdapter).data.addAll(shops[tab]!!)
				Log.d("", shops[tab].toString())
				Log.d("", ((itemsRecycler.adapter as TickListItemsAdapter).data).toString())
				(itemsRecycler.adapter as TickListItemsAdapter).notifyDataSetChanged()
			}

			override fun onTabUnselected(tab: TabLayout.Tab?) {}
			override fun onTabReselected(tab: TabLayout.Tab?) {}
		})

		for (shop in Shop.values()) {
			// Create layout tab
			val tab = tabLayout.newTab().setText(shop.resource)

			shops[tab] = mutableListOf()

			// Generate list of items to display for tab
			for (barcode in shoppingList.items.keys) {
				if (shoppingList.items[barcode]?.shop == shop) {
					shops[tab]?.add(Pair(API.getProduct(barcode), shoppingList.items[barcode]) as Pair<Product, ShoppingList.Item>)
				}
			}

			if (shops[tab]!!.isNotEmpty()) tabLayout.addTab(tab)
		}
	}
}