package pixlefury.shoppingapp.api

import org.json.JSONObject
import pixlefury.shoppingapp.R
import java.lang.Exception
import java.math.BigDecimal

data class ShoppingList(val id: Int, var name: String, var total_cost: BigDecimal, var permissions: Permission, var status: Status) {
	val items = mutableMapOf<Product.Barcode, Item>()

//	@JvmInline
//	value class Id(val id: Int) {
//		init {
//			//require(id.isNotEmpty())
//		}
//	}

	enum class Status(val id: String, val resource: Int) {
		Private("private", R.string.status_private),
		Shared("shared", R.string.status_shared),
		ReadyToShop("ready_to_shop", R.string.status_ready_to_shop);

		companion object {
			fun fromId(id: String) = when (id) {
				"private" -> Private
				"shared" -> Shared
				"ready_to_shop" -> ReadyToShop
				else -> throw Exception("No list status with id: $id")
			}
		}
	}

	enum class Permission(val id: String, val resource: Int) {
		Owner("owner", R.string.app_name), Shopper("shopper", R.string.app_name), Editor("editor", R.string.app_name);

		companion object {
			fun fromId(id: String) = when (id) {
				"owner" -> Owner
				"shopper" -> Shopper
				"editor" -> Editor
				else -> throw Exception("No list permission with id: $id")
			}
		}
	}

	data class Item(var quantity: UInt, var shop: Shop, var ticked: Boolean) {
		val subs = mutableListOf<Substitute>()
	}

	data class Substitute(var barcode: Product.Barcode, var amount: UInt, var ticked: Boolean)

	companion object {
		fun fromJson(json: JSONObject): ShoppingList {
			val id = json.getInt("id")
			val name = json.getString("name")
			val total_cost = BigDecimal(json.getString("total_cost"))
			val status = Status.fromId(json.getString("status"))
			val permissions = Permission.fromId(json.getString("permission"))

			return ShoppingList(id, name, total_cost, permissions, status)
		}
	}
}
