package pixlefury.shoppingapp.activity.product

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import pixlefury.shoppingapp.R
import pixlefury.shoppingapp.api.API
import java.lang.Exception

class ProductSearchActivity : AppCompatActivity() {
	companion object {
		const val SHOPPING_LIST = "shopping_list"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_product_search)

		val searchView = findViewById<EditText>(R.id.search_view)

//		val shopGroup = findViewById<RadioGroup>(R.id.shop_group)
//		val shops = listOf(
//			R.string.shop_tesco,
//			R.string.shop_asda,
//			R.string.shop_sainsburys,
//			R.string.shop_ms,
//			R.string.shop_iceland
//		)
//		for (shop in shops) {
//			val rb = CheckBox(this)
//			rb.text = getText(shop)
//			shopGroup.addView(rb)
//		}
//
//		val sortGroup = findViewById<RadioGroup>(R.id.sort_group)
//		val sortModes = listOf(
//			R.string.sort_relevance,
//			R.string.sort_alpha,
//			R.string.sort_amount,
//			R.string.sort_price,
//			R.string.sort_rating
//		)
//		for (sort_mode in sortModes) {
//			val rb = RadioButton(this)
//			rb.text = getText(sort_mode)
//			sortGroup.addView(rb)
//		}

		val shoppingLists = API.getOwnedShoppingLists()
		val listNames = shoppingLists.map { it.name }

		val selectedListSpinner = findViewById<Spinner>(R.id.selected_list_spinner)
		val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listNames)
		selectedListSpinner.adapter = adapter

		val listId = intent.extras?.getInt(SHOPPING_LIST)
		if (listId != null) {
			val shoppingList = API.getShoppingList(listId) ?: throw Exception("Could not find list: $listId")
			selectedListSpinner.setSelection(listNames.indexOf(shoppingList.name))
		}

		findViewById<Button>(R.id.btn_search).setOnClickListener {
			val intent = Intent(this, ProductsActivity::class.java)
			intent.putExtra(ProductsActivity.QUERY, searchView.text.toString())
			intent.putExtra(ProductsActivity.LIST, selectedListSpinner.selectedItem.toString())
			startActivity(intent)
		}
	}
}