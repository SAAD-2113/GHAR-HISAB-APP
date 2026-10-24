package com.sindantechnologies.gharhisab.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps icon key strings (stored in the database) to Material Icons.
 */
object IconMapper {
    fun getIcon(iconKey: String): ImageVector {
        return when (iconKey) {
            "shopping_cart" -> Icons.Filled.ShoppingCart
            "bolt" -> Icons.Filled.Home // Using Home as fallback for bolt
            "directions_car" -> Icons.Filled.DirectionsCar
            "school" -> Icons.Filled.School
            "medical_services" -> Icons.Filled.MedicalServices
            "home" -> Icons.Filled.Home
            "restaurant" -> Icons.Filled.Restaurant
            "local_mall" -> Icons.Filled.LocalMall
            "person" -> Icons.Filled.Person
            "more_horiz" -> Icons.Filled.MoreHoriz
            "payments" -> Icons.Filled.Payments
            "store" -> Icons.Filled.Store
            "real_estate_agent" -> Icons.Filled.AccountBalance
            "work" -> Icons.Filled.Work
            "card_giftcard" -> Icons.Filled.CardGiftcard
            "fastfood" -> Icons.Filled.Fastfood
            "credit_card" -> Icons.Filled.CreditCard
            else -> Icons.Filled.MoreHoriz
        }
    }
}
