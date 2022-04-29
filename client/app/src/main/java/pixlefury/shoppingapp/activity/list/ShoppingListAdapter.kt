package pixlefury.shoppingapp.activity.list
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pixlefury.shoppingapp.R
import pixlefury.shoppingapp.api.ShoppingList
import java.text.NumberFormat
import java.util.*

class ShoppingListAdapter(var data: List<ShoppingList>, var sections: List<Pair<Int, Int>>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	companion object {
		const val VIEW_TYPE_HEADER = 1
	}

	class HeaderViewHolder(val tvHeader: TextView) : RecyclerView.ViewHolder(tvHeader)

	class ListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val tvName: TextView = view.findViewById(R.id.tv_list_name)
		val tvTotalCost: TextView = view.findViewById(R.id.tv_list_total_cost)
		val tvStatus: TextView = view.findViewById(R.id.tv_list_status)
		lateinit var adapter: ShoppingListAdapter

		init {
			view.setOnClickListener {
				val list = adapter.data[adapter.getDataIndex(adapterPosition)]
				Log.d("List", list.toString())
				if (list.permissions == ShoppingList.Permission.Shopper) {
					val intent = Intent(view.context, ShopForListActivity::class.java)
					intent.putExtra(ShopForListActivity.SHOPPING_LIST, list.id)
					view.context.startActivity(intent)
				} else {
					val intent = Intent(view.context, ListDetailsActivity::class.java)
					intent.putExtra(ListDetailsActivity.SHOPPING_LIST, list.id)
					view.context.startActivity(intent)
				}
			}
		}
	}

	override fun getItemViewType(position: Int): Int {
		for (section in sections) {
			if (position == section.second) return VIEW_TYPE_HEADER
		}
		return 0
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
		VIEW_TYPE_HEADER -> HeaderViewHolder(TextView(parent.context, null, 0, R.style.header))
		else -> ListViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.rv_item_shopping_list, parent, false))
	}

	fun getDataIndex(position: Int): Int {
		var index = position
		for (section in sections.reversed()) if (position >= section.second) index--
		return index
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		when (holder.itemViewType) {
			VIEW_TYPE_HEADER -> {
				holder as HeaderViewHolder
				val section = sections.find { it.second == position }
				holder.tvHeader.text = holder.itemView.context.getText(section?.first ?: R.string.app_name)
			}
			else -> {
				val item = data[getDataIndex(position)]
				holder as ListViewHolder
				holder.tvName.text = item.name
				holder.tvTotalCost.text = NumberFormat.getCurrencyInstance(Locale.UK).format(item.total_cost)
				holder.tvStatus.text = holder.itemView.context.getText(item.status.resource)
				holder.adapter = this
			}
		}
	}

	override fun getItemCount() = data.size + sections.size
}