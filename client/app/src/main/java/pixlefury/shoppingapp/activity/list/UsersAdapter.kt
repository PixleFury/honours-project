package pixlefury.shoppingapp.activity.list

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pixlefury.shoppingapp.R
import pixlefury.shoppingapp.api.User

class UsersAdapter(val data: MutableList<User>, val child_clicked: ((User) -> Unit)?) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {
	class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val tvUsername: TextView = view.findViewById(R.id.tv_user_username)
		lateinit var adapter: UsersAdapter

		init {
			view.setOnClickListener {
				adapter.child_clicked?.invoke(adapter.data[adapterPosition])
			}
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
		return UserViewHolder(
			LayoutInflater.from(parent.context).inflate(R.layout.rv_item_user, parent, false)
		)
	}

	override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
		holder.adapter = this
		val user = data[position]
		holder.tvUsername.text = user.username
	}

	override fun getItemCount() = data.size
}