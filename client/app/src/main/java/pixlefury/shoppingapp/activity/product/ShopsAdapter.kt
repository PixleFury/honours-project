package pixlefury.shoppingapp.activity.product

import android.icu.number.NumberFormatter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pixlefury.shoppingapp.R
import pixlefury.shoppingapp.api.Shop
import pixlefury.shoppingapp.api.User
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

class ShopsAdapter(val data: List<Pair<Shop, BigDecimal>>, val child_clicked: ((Shop) -> Unit)?) : RecyclerView.Adapter<ShopsAdapter.ShopViewHolder>() {
	class ShopViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val tvShop: TextView = view.findViewById(R.id.tv_shop)
		val tvPrice: TextView = view.findViewById(R.id.tv_shop_price)
		lateinit var adapter: ShopsAdapter

		init {
			view.setOnClickListener {
				adapter.child_clicked?.invoke(adapter.data[adapterPosition].first)
			}
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
		return ShopViewHolder(
			LayoutInflater.from(parent.context).inflate(R.layout.rv_item_shop_and_price, parent, false)
		)
	}

	override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
		holder.adapter = this
		holder.tvShop.setText(data[position].first.resource)
		holder.tvPrice.text = NumberFormat.getCurrencyInstance(Locale.UK).format(data[position].second)
	}

	override fun getItemCount() = data.size
}