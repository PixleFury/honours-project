package pixlefury.shoppingapp.api

import pixlefury.shoppingapp.R

enum class Shop(val id: String, val resource: Int) {
	TESCO("tesco", R.string.shop_tesco),
	M_AND_S("marks_and_spencer", R.string.shop_ms),
	ASDA("asda", R.string.shop_asda);

	companion object {
		fun fromId(id: String) = when (id) {
			"tesco" -> TESCO
			"marks_and_spencer" -> M_AND_S
			"asda" -> ASDA
			else -> TESCO
		}
	}
}