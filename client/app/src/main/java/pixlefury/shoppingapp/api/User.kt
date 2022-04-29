package pixlefury.shoppingapp.api

import org.json.JSONObject

data class User(val id: Int, val username: String) {
	companion object {
		fun fromJson(json: JSONObject): User {
			val id = json.getInt("id")
			val username = json.getString("username")

			return User(id, username)
		}
	}
}
