package io.fatkhuranonym.nuchainnode.data

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson

data class SessionResponse(
	val result: String? = null,
	val id: Int? = null,
	val jsonrpc: String? = null
) {
	class Deserializer : ResponseDeserializable<SessionResponse> {
		override fun deserialize(content: String) = Gson().fromJson(content, SessionResponse::class.java)
	}
}

