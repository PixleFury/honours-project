package pixlefury.shoppingapp.api

import org.json.JSONObject
import java.math.BigDecimal

data class Product(
	val barcode: Barcode,
	val name: String,
	val description: String,
) {
	val prices: MutableMap<Shop, BigDecimal> = mutableMapOf()

	@JvmInline
	value class Barcode(private val barcode: String) {
		init {
			require(barcode.isNotEmpty())
			// TODO: also check for valid characters
		}

		override fun toString() = barcode
	}

	companion object {
		fun fromJson(json: JSONObject): Product {
			val barcode = Barcode(json.getString("barcode"))
			val name = json.getString("name")
			val description = json.getString("description")

			val product = Product(barcode, name, description)

			for (shop in Shop.values()) {
				val soldAt = json.optBoolean("sold_at_${shop.id}", false)
				if (soldAt) {
					val priceString = json.getString("price_${shop.id}")
					val priceDecimal = priceString.toBigDecimalOrNull()
					priceDecimal?.let {
						product.prices.put(shop, priceDecimal)
					}
				}
			}

			return product
		}
	}
}