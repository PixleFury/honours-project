package pixlefury.shoppingapp.activity.product

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.NetworkImageView
import pixlefury.shoppingapp.R
import pixlefury.shoppingapp.activity.list.ListDetailsActivity
import pixlefury.shoppingapp.api.API
import pixlefury.shoppingapp.api.Product
import pixlefury.shoppingapp.api.Shop
import pixlefury.shoppingapp.api.ShoppingList
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

class ProductsAdapter(var data: MutableList<Product>, val listId: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val tvName: TextView = view.findViewById(R.id.tv_product_name)
		val tvPriceRange: TextView = view.findViewById(R.id.tv_product_price_range)
		val imgIcon: NetworkImageView = view.findViewById(R.id.img_product_icon)
		val shopBadges: LinearLayout = view.findViewById(R.id.shops_container)
		lateinit var adapter: ProductsAdapter

		init {
			view.setOnClickListener {
				val intent = Intent(view.context, ProductInfoActivity::class.java)
				intent.putExtra(ProductInfoActivity.PRODUCT, adapter.data[adapterPosition].barcode.toString())
				intent.putExtra(ProductInfoActivity.LIST, adapter.listId)
				view.context.startActivity(intent)
			}

			view.findViewById<Button>(R.id.btn_add_to_list).setOnClickListener {
				val product = adapter.data[adapterPosition]
				if (product.prices.keys.size == 1) {
					// Only one shop so just add it from there
					API.modifyListItem(adapter.listId, product.barcode, product.prices.keys.first(), 1u)
				} else {
					val views = View.inflate(view.context, R.layout.dialog_add_item_to_list, null)
					val selectedListSpinner = views.findViewById<Spinner>(R.id.selected_list_spinner)
					val adapter2 = ArrayAdapter(views.context, android.R.layout.simple_spinner_item, listOf(API.getShoppingList(adapter.listId)?.name))
					selectedListSpinner.adapter = adapter2

					val dialog = AlertDialog.Builder(view.context)
						.setView(views)
						.setTitle(product.name)
						.create()

					val shopsRecycler = views.findViewById<RecyclerView>(R.id.rv_shops_for_product)
					shopsRecycler.adapter = ShopsAdapter(product.prices.toList()) { shop ->
						API.modifyListItem(adapter.listId, product.barcode, shop, 1u)
						dialog.cancel()
					}
					shopsRecycler.layoutManager = LinearLayoutManager(view.context)

					dialog.show()
				}
			}
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return ProductViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.rv_item_product, parent, false))
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		holder as ProductViewHolder
		holder.adapter = this
		val product = data[position]

		val maxPrice = product.prices.maxByOrNull { it.value }?.value
		val minPrice = product.prices.minByOrNull { it.value }?.value

		val formatter = NumberFormat.getCurrencyInstance(Locale.UK)

		val priceString: String =
			if (maxPrice == null && minPrice == null) {
				holder.itemView.context.getString(R.string.product_unavailable)
			} else if (maxPrice == minPrice) {
				formatter.format(maxPrice)
			} else {
				formatter.format(minPrice) + " - " + formatter.format(maxPrice)
			}

		holder.tvName.text = product.name
		holder.tvPriceRange.text = priceString

		holder.imgIcon.setImageUrl(API.API_URL + "/images/products/${product.barcode}.png", API.imageLoader)

		holder.shopBadges.removeAllViews()
		for (shop in product.prices.keys) {
			val badge = TextView(holder.itemView.context)
			badge.setText(shop.resource)
			val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
			params.setMargins(0, 0, 24, 0)
			badge.setLayoutParams(params)
			holder.shopBadges.addView(badge)
		}
	}

	override fun getItemCount(): Int = data.size

}
