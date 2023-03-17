package com.csamanez.firebaseauthentication

data class Product(
    var id: String?,
    var name: String?,
    val description: String?,
    val imgUrl: String?,
    val quantity: Int = 0,
    val price: Double = 0.0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Product

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}
