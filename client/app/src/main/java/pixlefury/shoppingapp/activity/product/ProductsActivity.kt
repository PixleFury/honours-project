package pixlefury.shoppingapp.activity.product

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pixlefury.shoppingapp.R
import pixlefury.shoppingapp.api.API

class ProductsActivity : AppCompatActivity() {
	companion object {
		const val QUERY = "query"
		const val LIST = "shopping_list"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_products)

		val query = intent.extras?.getString(QUERY) ?: ""
		val listName = intent.extras?.getString(LIST) ?: ""
		val list = API.getShoppingLists().find {it.name == listName}
		Log.d("TEST", "Editing: " + listName)

		if (list == null) {
			finish()
			return
		}

		var hasMorePagesToLoad = false
		var isLoadingRequest = false
		var page = 1u

		API.searchForProducts(query) { products, has_more ->
			if (products.isEmpty()) {
				// TODO: Display no results message
			} else {
				hasMorePagesToLoad = has_more

				val productsRecycler = findViewById<RecyclerView>(R.id.rv_products)
				productsRecycler.adapter = ProductsAdapter(products.toMutableList(), list.id)
				productsRecycler.layoutManager = LinearLayoutManager(this)

				// Pagination for many results
				productsRecycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {
					// Adapted from dominicoder - https://stackoverflow.com/a/45909112, converted to Kotlin and integrated into the project
					override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
						if (!hasMorePagesToLoad) return;
						if (isLoadingRequest) return;

						val layoutManager = rv.layoutManager as LinearLayoutManager
						val currentLastItem = layoutManager.findLastVisibleItemPosition()

						if (currentLastItem == layoutManager.itemCount - 1) {
							isLoadingRequest = true
							API.searchForProducts(query, page) { products, still_has_more ->
								isLoadingRequest = false // Stopped loading
								hasMorePagesToLoad = still_has_more
								if (products.isNotEmpty()) {
									(productsRecycler.adapter as ProductsAdapter).data.addAll(products)
									rv.adapter?.notifyDataSetChanged()
								}
							}
							page += 1u
						}
					}
				})
			}
		}
	}
}