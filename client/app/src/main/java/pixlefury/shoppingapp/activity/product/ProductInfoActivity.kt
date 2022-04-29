package pixlefury.shoppingapp.activity.product

import android.R.attr.left
import android.R.attr.right
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.NetworkImageView
import pixlefury.shoppingapp.R
import pixlefury.shoppingapp.api.API
import pixlefury.shoppingapp.api.Product
import java.text.NumberFormat
import java.util.*


class ProductInfoActivity : AppCompatActivity() {
	companion object {
		const val PRODUCT = "product"
		const val LIST = "list"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_product_info)

		val barcode = Product.Barcode(intent.extras?.getString(PRODUCT) ?: "")
		val product = API.getProduct(barcode) ?: throw Exception("Product cannot be found")

		val listId = intent.extras?.getInt(LIST) ?: 0
		val shoppingList = API.getShoppingList(listId) ?: throw Exception("List cannot be found")

		findViewById<TextView>(R.id.tv_product_name).text = product.name
		findViewById<TextView>(R.id.tv_product_desc).text = product.description

		findViewById<NetworkImageView>(R.id.img_main_image).setImageUrl(API.API_URL + "/images/products/${product.barcode}.png", API.imageLoader)

		for (shop in product.prices.keys) {
			val badge = TextView(this)
			badge.setText(shop.resource)
			val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
			params.setMargins(0, 0, 24, 0)
			badge.setLayoutParams(params)
			findViewById<LinearLayout>(R.id.shop_tags_contianer).addView(badge)
		}

		if (product.prices.isEmpty()) {
			findViewById<TextView>(R.id.tv_product_price_range).text = getString(R.string.product_unavailable)
			findViewById<Button>(R.id.btn_add_to_list).isEnabled = false
		} else {
			val maxPrice = product.prices.maxByOrNull { it.value }?.value
			val minPrice = product.prices.minByOrNull { it.value }?.value
			val formatter = NumberFormat.getCurrencyInstance(Locale.UK)

			findViewById<TextView>(R.id.tv_product_price_range).text =
				if (maxPrice == minPrice) {
					formatter.format(maxPrice)
				} else {
					formatter.format(minPrice) + " - " + formatter.format(maxPrice)
				}

			findViewById<Button>(R.id.btn_add_to_list).setOnClickListener {
				if (product.prices.keys.size == 1) {
					// Only one shop so just add it from there
					API.modifyListItem(shoppingList.id, product.barcode, product.prices.keys.first(), 1u)
				} else {
					val views = View.inflate(this, R.layout.dialog_add_item_to_list, null)
					val selectedListSpinner = views.findViewById<Spinner>(R.id.selected_list_spinner)
					val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf(shoppingList.name))
					selectedListSpinner.adapter = adapter

					val dialog = AlertDialog.Builder(this)
						.setView(views)
						.setTitle(product.name)
						.create()

					val shopsRecycler = views.findViewById<RecyclerView>(R.id.rv_shops_for_product)
					shopsRecycler.adapter = ShopsAdapter(product.prices.toList()) { shop ->
						API.modifyListItem(shoppingList.id, product.barcode, shop, 1u)
						dialog.cancel()
					}
					shopsRecycler.layoutManager = LinearLayoutManager(this)

					dialog.show()
				}
			}
		}
	}
}