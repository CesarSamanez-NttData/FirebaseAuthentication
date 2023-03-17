package com.csamanez.firebaseauthentication

interface OnProductListener {
    fun onClick(product: Product)
    fun onLongClick(product: Product) // Remove a product
}