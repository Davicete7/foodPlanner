package com.example.foodplanner.ui.nav

object Routes {
    const val Auth = "auth"
    const val Inventory = "inventory"
    const val Recipes = "recipes"
    const val Cart = "cart"
    const val ChatList = "chatList"
    fun chat(chatId: String) = "chat/$chatId"
    const val Settings = "settings"
}
