package pixlefury.shoppingapp.api

import android.content.Context
import android.graphics.Bitmap
import android.os.AsyncTask
import android.util.Log
import android.util.LruCache
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.*
import org.json.JSONObject
import java.math.BigDecimal
import java.net.*

object API {
	private const val LOGGER_TAG = "Shopping API"
	private lateinit var requestQueue: RequestQueue
	public const val API_URL = "https://tranquil-garden-88100.herokuapp.com/"

	private var signedIn = false

	// Local cache
	private val shopping_lists = mutableMapOf<Int, ShoppingList>()
	private val products = mutableMapOf<Product.Barcode, Product>()
	private val known_users = mutableMapOf<Int, User>()

	public lateinit var imageLoader: ImageLoader // This is for product images

	fun init(context: Context) {
		CookieHandler.setDefault(CookieManager()) // Enable (session) cookies
		requestQueue = Volley.newRequestQueue(context)

		imageLoader = ImageLoader(requestQueue, object: ImageLoader.ImageCache {
			val cache = LruCache<String, Bitmap>(20)

			override fun getBitmap(url: String): Bitmap? {
				return cache.get(url)
			}

			override fun putBitmap(url: String, bitmap: Bitmap) {
				cache.put(url, bitmap)
			}
		})
	}


	private fun loadUserData() {
		updateShoppingLists() {
			getShoppingListProducts()
		}
		// TODO: Get know users from list permission
	}


	fun login(username: String, password: String, callback: (res: Boolean) -> Unit) {
		val json = JSONObject()
		json.put("username", username)
		json.put("password", password)

		val request = JsonObjectRequest(Request.Method.POST, API_URL + "auth", json,
			{
				Log.d(LOGGER_TAG, it.toString())
				signedIn = true
				CookieHandler.getDefault()
				loadUserData()
				callback.invoke(true)
			},
			{
				Log.e(LOGGER_TAG, it.toString())
				callback.invoke(false)
			}
		)
		requestQueue.add(request)
	}


	fun logout() {
		signedIn = false
		shopping_lists.clear()
		known_users.clear()
	}


	fun register(username: String, password: String, callback: (res: Boolean) -> Unit) {
		val json = JSONObject()
		json.put("username", username)
		json.put("password", password)

		val request = JsonObjectRequest(Request.Method.POST, API_URL + "register", json,
			{
				Log.d(LOGGER_TAG, it.toString())
				loadUserData()
				callback.invoke(true)
			},
			{
				Log.e(LOGGER_TAG, it.toString())
				callback.invoke(false)
			}
		)
		requestQueue.add(request)
	}


	fun isSignedIn(): Boolean {
		return signedIn;
	}


	fun getKnownUsers(): List<User> {
		return known_users.values.toList()
	}


	fun getKnownUser(userId: Int): User? {
		return known_users[userId]
	}


	fun getShoppingList(listId: Int): ShoppingList? {
		return shopping_lists[listId]
	}


	fun getShoppingLists(): List<ShoppingList> {
		return shopping_lists.values.toList()
	}


	fun getOwnedShoppingLists(): List<ShoppingList> {
		return shopping_lists.filter { it.value.permissions == ShoppingList.Permission.Owner }.values.toList()
	}


	fun getSharedShoppingLists(): List<ShoppingList> {
		return shopping_lists.filter { it.value.permissions != ShoppingList.Permission.Owner }.values.toList()
	}


	fun deleteShoppingList(listId: Int, callback: () -> Unit) {
		val request = JsonObjectRequest(Request.Method.DELETE, API_URL + "lists/" + listId, null,
			{
				shopping_lists.remove(listId)
				callback()
			},
			{
				Log.e(LOGGER_TAG, it.toString())
			})
		requestQueue.add(request)
	}


	private fun updateShoppingLists(callback: () -> Unit) {
		val request = JsonArrayRequest(Request.Method.GET, API_URL + "lists", null,
			{ it ->
				for (i in 0 until it.length()) {
					val list = ShoppingList.fromJson(it.getJSONObject(i))
					shopping_lists[list.id] = list // Update internal cache

					val request2 = JsonObjectRequest(Request.Method.GET, API_URL + "lists/" + list.id, null,
						{
							Log.d(LOGGER_TAG, it.toString())

							val items = mutableMapOf<Product.Barcode, ShoppingList.Item>()
							val jsonItems = it.optJSONArray("items")
							if (jsonItems != null) {
								for (i in 0 until jsonItems.length()) {
									val jsonEntry = jsonItems.getJSONObject(i)
									val barcode = Product.Barcode(jsonEntry.getString("barcode"))
									val quantity = jsonEntry.getInt("amount").toUInt()
									val shop = Shop.fromId(jsonEntry.getString("shop"))
									val ticked = jsonEntry.getBoolean("ticked")
									items[barcode] = ShoppingList.Item(quantity, shop, ticked)
								}
							}
							shopping_lists[list.id]?.items?.putAll(items)
							callback()
						},
						{
							Log.e(LOGGER_TAG, it.toString())
						}
					)
					requestQueue.add(request2)
				}
			},
			{
				Log.e(LOGGER_TAG, it.toString())
			}
		)
		requestQueue.add(request)
	}


	private fun getShoppingListProducts() {
		for (list in shopping_lists.values) {
			for (barcode in list.items.keys) {
				// If product info is unkown
				if (!products.containsKey(barcode)) {
					requestQueue.add(JsonObjectRequest(API_URL + "products/$barcode", null,
						{ json ->
							products[barcode] = Product.fromJson(json)
						},
						{
							Log.e(LOGGER_TAG, it.toString())
						}
					))
				}
			}
		}
	}


	fun modifyListItem(listId: Int, barcode: Product.Barcode, shop: Shop, quantity: UInt = 1u) {
		val json = JSONObject()
		json.put("barcode", barcode)
		json.put("shop", shop.id)
		json.put("amount", quantity)

		requestQueue.add(JsonObjectRequest(Request.Method.PUT, API_URL + "lists/$listId/items", json,
			{
				Log.d(LOGGER_TAG, it.toString())
				if (quantity == 0u) {
					shopping_lists[listId]?.items?.remove(barcode) // Delete if none left
				} else {
					shopping_lists[listId]?.items?.set(barcode, ShoppingList.Item(quantity, shop, false)) // Update local list with new amount
				}
				var total = BigDecimal(0)
				for (kv in shopping_lists[listId]!!.items.entries) {
					total += getProduct(kv.key)?.prices?.get(kv.value.shop)?.times(kv.value.quantity.toInt().toBigDecimal())!!
				}
				shopping_lists[listId]?.total_cost = total
			},
			{
				Log.e(LOGGER_TAG, it.toString())
			}
		))
	}

	fun tickListItem(listId: Int, barcode: Product.Barcode, ticked: Boolean) {
		val json = JSONObject()
		json.put("barcode", barcode)
		json.put("ticked", ticked)

		requestQueue.add(JsonObjectRequest(Request.Method.PUT, API_URL + "lists/$listId/ticked", json,
			{
				Log.d(LOGGER_TAG, it.toString())
				shopping_lists[listId]?.items?.get(barcode)?.ticked = ticked // Update local list with new amount
			},
			{
				Log.e(LOGGER_TAG, it.toString())
			}
		))
	}


	fun searchForProducts(nameQuery: String, page: UInt = 0u, callback: (res: List<Product>, has_more: Boolean) -> Unit) {
		val request = JsonObjectRequest(Request.Method.GET, API_URL + "products?q=${nameQuery}&p=${page}", null,
			{ json ->
				Log.d(LOGGER_TAG, json.toString())

				val searchedProducts = mutableListOf<Product>()

				val jsonProducts = json.getJSONArray("products")
				for (i in 0 until jsonProducts.length()) {
					val product = Product.fromJson(jsonProducts.getJSONObject(i))
					searchedProducts.add(product)
					products[product.barcode] = product // Update internal cache
				}

				callback.invoke(searchedProducts, json.getBoolean("has_more"))
			},
			{
				Log.e(LOGGER_TAG, it.toString())
				callback.invoke(emptyList(), false)
			}
		)
		requestQueue.add(request)
	}


	fun getProduct(barcode: Product.Barcode): Product? {
		return products[barcode]
	}


	fun createShoppingList(name: String, callback: () -> Unit) {
		val req = JSONObject()
		req.put("name", name)

		requestQueue.add(JsonObjectRequest(Request.Method.POST, API_URL + "lists", req,
			{ json ->
				val list = ShoppingList.fromJson(json)
				shopping_lists[list.id] = list // Update internal cache
				Log.d("a", json.toString())
				callback.invoke()
			},
			{
				Log.e(LOGGER_TAG, it.toString())
				callback.invoke()
			}
		))
	}


	fun searchForUsers(nameQuery: String, callback: (res: List<User>) -> Unit) {
		if (nameQuery.length < 3) return

		requestQueue.add(JsonArrayRequest(API_URL + "users?q=${nameQuery}",
			{ json ->
				val searchedUsers = mutableListOf<User>()

				//val jsonUsers = json.getJSONArray("users")
				for (i in 0 until json.length()) {
					val user = User.fromJson(json.getJSONObject(i))
					searchedUsers.add(user)
				}

				callback(searchedUsers)
			},
			{
				Log.e(LOGGER_TAG, it.toString())
				callback(emptyList())
			}
		))
	}

	fun shareListWithUser(listId: Int, user: User, permission: ShoppingList.Permission) {
		val req = JSONObject()
		req.put("user_id", user.id)
		req.put("permission", permission.id)

		requestQueue.add(JsonObjectRequest(Request.Method.PUT, API_URL + "lists/$listId/users", req,
			{ json ->
				Log.e(LOGGER_TAG, json.toString())
				known_users[user.id] = user
				shopping_lists[listId]?.status = ShoppingList.Status.Shared
			},
			{
				Log.e(LOGGER_TAG, it.toString())
			}
		))
	}
}