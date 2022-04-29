package pixlefury.shoppingapp.activity.list
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import pixlefury.shoppingapp.R
import pixlefury.shoppingapp.activity.product.ProductInfoActivity
import pixlefury.shoppingapp.api.API
import pixlefury.shoppingapp.api.Product
import pixlefury.shoppingapp.api.ShoppingList
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

class TickListItemsAdapter(val data: MutableList<Pair<Product, ShoppingList.Item>>, val list: ShoppingList) : RecyclerView.Adapter<TickListItemsAdapter.TickListItemViewHolder>() {
	class TickListItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val tvItemName: TextView = view.findViewById(R.id.tv_item_name)
		val tvAmount: TextView = view.findViewById(R.id.tv_item_amount)
		val cbTicked: CheckBox = view.findViewById(R.id.cb_item_ticked)
		lateinit var adapter: TickListItemsAdapter

		init {
			cbTicked.setOnClickListener {
				API.tickListItem(adapter.list.id, adapter.data[adapterPosition].first.barcode, (it as CheckBox).isChecked)
			}
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TickListItemViewHolder {
		return TickListItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.rv_item_shop_for_list_item, parent, false))
	}

	override fun onBindViewHolder(holder: TickListItemViewHolder, position: Int) {
		holder.adapter = this

		val comboItem = data[position]
		val product = comboItem.first
		val listItem = comboItem.second

		holder.tvItemName.text = product.name
		holder.tvAmount.text = holder.itemView.context.getString(R.string.item_amount, listItem.quantity.toInt())
		holder.cbTicked.isChecked = listItem.ticked
	}

	override fun getItemCount() = data.size
}