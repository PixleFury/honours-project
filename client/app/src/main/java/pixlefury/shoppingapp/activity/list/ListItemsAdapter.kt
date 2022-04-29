package pixlefury.shoppingapp.activity.list
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import pixlefury.shoppingapp.R
import pixlefury.shoppingapp.activity.product.ProductInfoActivity
import pixlefury.shoppingapp.api.Product
import pixlefury.shoppingapp.api.ShoppingList
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

class ListItemsAdapter(val data: MutableList<Pair<Product, ShoppingList.Item>>, val list: ShoppingList) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	val edited_items = mutableMapOf<Int, Boolean>()

	companion object {
		const val VIEW_TYPE_HEADER = 1
	}

	class ListItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val tvItemName: TextView = view.findViewById(R.id.tv_list_item_name)
		val tvItemTotalPrice: TextView = view.findViewById(R.id.tv_list_item_total_price)
		val etItemAmount: EditText = view.findViewById(R.id.et_n_amount)
		lateinit var adapter: ListItemsAdapter

		init {
			view.setOnClickListener {
				val intent = Intent(view.context, ProductInfoActivity::class.java)
				intent.putExtra(ProductInfoActivity.PRODUCT, adapter.data[adapterPosition].first.barcode.toString())
				intent.putExtra(ProductInfoActivity.LIST, adapter.list.id)
				view.context.startActivity(intent)
			}

			etItemAmount.addTextChangedListener { text ->
				val comboItem = adapter.data[adapterPosition]
				val product = comboItem.first
				val listItem = comboItem.second

				val countString = text.toString()
				if (countString.isNotEmpty()) {
					listItem.quantity = countString.toUInt()

					if (listItem.quantity == 0u) {
						tvItemTotalPrice.setText(R.string.removed)
					} else {
						val totalCost = product.prices[listItem.shop]?.times(BigDecimal(listItem.quantity.toInt()))
						tvItemTotalPrice.text = NumberFormat.getCurrencyInstance(Locale.UK).format(totalCost)
					}

					adapter.edited_items[adapterPosition] = true
				}
			}

			view.findViewById<Button>(R.id.btn_more).setOnClickListener {
				val comboItem = adapter.data[adapterPosition]
				val product = comboItem.first
				val listItem = comboItem.second

				// Only increase if below cap
				if (listItem.quantity < 99u) {
					listItem.quantity += 1u

					// Update views
					etItemAmount.setText(listItem.quantity.toString())
					val totalCost = product.prices[listItem.shop]?.times(BigDecimal(listItem.quantity.toInt()))
					tvItemTotalPrice.text = NumberFormat.getCurrencyInstance(Locale.UK).format(totalCost)

					adapter.edited_items[adapterPosition] = true
				}
			}

			view.findViewById<Button>(R.id.btn_less).setOnClickListener {
				val comboItem = adapter.data[adapterPosition]
				val product = comboItem.first
				val listItem = comboItem.second

				// Only reduce if there is enough left
				if (listItem.quantity > 0u) {
					listItem.quantity -= 1u

					// Update views
					etItemAmount.setText(listItem.quantity.toString())

					if (listItem.quantity == 0u) {
						tvItemTotalPrice.setText(R.string.removed)
					} else {
						val totalCost = product.prices[listItem.shop]?.times(BigDecimal(listItem.quantity.toInt()))
						tvItemTotalPrice.text = NumberFormat.getCurrencyInstance(Locale.UK).format(totalCost)
					}

					adapter.edited_items[adapterPosition] = true
				}
			}
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return ListItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.rv_item_list_item, parent, false))
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		holder as ListItemViewHolder

		holder.adapter = this

		val comboItem = data[position]
		val product = comboItem.first
		val listItem = comboItem.second

		holder.tvItemName.text = product.name
		holder.tvItemTotalPrice.text

		val totalCost = product.prices[listItem.shop]?.times(BigDecimal(listItem.quantity.toInt()))
		holder.tvItemTotalPrice.text = NumberFormat.getCurrencyInstance(Locale.UK).format(totalCost)

		holder.etItemAmount.setText(listItem.quantity.toString())
	}

	override fun getItemCount() = data.size
}