package com.budgetapp.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DryCleaning
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.Liquor
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PedalBike
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SmokingRooms
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Stroller
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class EnvelopeIconOption(
    val token: String,
    val label: String,
    val imageVector: ImageVector,
)

object EnvelopeIconCatalog {
    const val DefaultToken = "wallet"

    val options =
        listOf(
            EnvelopeIconOption(DefaultToken, "Wallet", Icons.Outlined.AccountBalanceWallet),
            EnvelopeIconOption("groceries", "Groceries", Icons.Outlined.ShoppingCart),
            EnvelopeIconOption("home", "Home", Icons.Outlined.Home),
            EnvelopeIconOption("transport", "Transport", Icons.Outlined.DirectionsCar),
            EnvelopeIconOption("dining", "Dining", Icons.Outlined.Restaurant),
            EnvelopeIconOption("bills", "Bills", Icons.Outlined.ReceiptLong),
            EnvelopeIconOption("health", "Health", Icons.Outlined.MedicalServices),
            EnvelopeIconOption("gift", "Gifts", Icons.Outlined.CardGiftcard),
            EnvelopeIconOption("travel", "Travel", Icons.Outlined.Flight),
            EnvelopeIconOption("goal", "Goal", Icons.Outlined.Flag),
            EnvelopeIconOption("savings", "Savings", Icons.Outlined.Savings),
            EnvelopeIconOption("entertainment", "Entertainment", Icons.Outlined.Movie),
            EnvelopeIconOption("gaming", "Gaming", Icons.Outlined.SportsEsports),
            EnvelopeIconOption("education", "Education", Icons.Outlined.School),
            EnvelopeIconOption("children", "Children", Icons.Outlined.ChildCare),
            EnvelopeIconOption("fitness", "Fitness", Icons.Outlined.FitnessCenter),
            EnvelopeIconOption("coffee", "Coffee", Icons.Outlined.LocalCafe),
            EnvelopeIconOption("phone", "Phone", Icons.Outlined.PhoneAndroid),
            EnvelopeIconOption("internet", "Internet", Icons.Outlined.Wifi),
            EnvelopeIconOption("insurance", "Insurance", Icons.Outlined.Security),
            EnvelopeIconOption("clothing", "Clothing", Icons.Outlined.Checkroom),
            EnvelopeIconOption("work", "Work", Icons.Outlined.Work),
            EnvelopeIconOption("subscriptions", "Subscriptions", Icons.Outlined.Subscriptions),
            EnvelopeIconOption("fuel", "Fuel", Icons.Outlined.LocalGasStation),
            EnvelopeIconOption("repairs", "Repairs", Icons.Outlined.Build),
            EnvelopeIconOption("pets", "Pets", Icons.Outlined.Pets),
            EnvelopeIconOption("sweets", "Sweets", Icons.Outlined.Cake),
            EnvelopeIconOption("ice_cream", "Ice cream", Icons.Outlined.Icecream),
            EnvelopeIconOption("bakery", "Bakery", Icons.Outlined.BakeryDining),
            EnvelopeIconOption("junk_food", "Junk food", Icons.Outlined.Fastfood),
            EnvelopeIconOption("drinks", "Drinks", Icons.Outlined.LocalBar),
            EnvelopeIconOption("alcohol", "Alcohol", Icons.Outlined.Liquor),
            EnvelopeIconOption("tobacco", "Tobacco", Icons.Outlined.SmokingRooms),
            EnvelopeIconOption("rent", "Rent", Icons.Outlined.Apartment),
            EnvelopeIconOption("furniture", "Furniture", Icons.Outlined.Chair),
            EnvelopeIconOption("appliances", "Appliances", Icons.Outlined.Kitchen),
            EnvelopeIconOption("cleaning", "Cleaning", Icons.Outlined.CleaningServices),
            EnvelopeIconOption("laundry", "Laundry", Icons.Outlined.LocalLaundryService),
            EnvelopeIconOption("dry_cleaning", "Dry cleaning", Icons.Outlined.DryCleaning),
            EnvelopeIconOption("electricity", "Electricity", Icons.Outlined.Bolt),
            EnvelopeIconOption("water", "Water", Icons.Outlined.WaterDrop),
            EnvelopeIconOption("heating", "Heating", Icons.Outlined.Thermostat),
            EnvelopeIconOption("lighting", "Lighting", Icons.Outlined.Lightbulb),
            EnvelopeIconOption("bus", "Bus", Icons.Outlined.DirectionsBus),
            EnvelopeIconOption("train", "Train", Icons.Outlined.Train),
            EnvelopeIconOption("taxi", "Taxi", Icons.Outlined.LocalTaxi),
            EnvelopeIconOption("motorcycle", "Motorcycle", Icons.Outlined.TwoWheeler),
            EnvelopeIconOption("bicycle", "Bicycle", Icons.Outlined.PedalBike),
            EnvelopeIconOption("parking", "Parking", Icons.Outlined.LocalParking),
            EnvelopeIconOption("hotel", "Hotels", Icons.Outlined.Hotel),
            EnvelopeIconOption("pharmacy", "Pharmacy", Icons.Outlined.LocalPharmacy),
            EnvelopeIconOption("beauty", "Beauty", Icons.Outlined.Spa),
            EnvelopeIconOption("haircuts", "Haircuts", Icons.Outlined.ContentCut),
            EnvelopeIconOption("baby", "Baby", Icons.Outlined.Stroller),
            EnvelopeIconOption("books", "Books", Icons.Outlined.MenuBook),
            EnvelopeIconOption("music", "Music", Icons.Outlined.MusicNote),
            EnvelopeIconOption("art", "Art", Icons.Outlined.Palette),
            EnvelopeIconOption("photography", "Photography", Icons.Outlined.PhotoCamera),
            EnvelopeIconOption("football", "Football", Icons.Outlined.SportsSoccer),
            EnvelopeIconOption("basketball", "Basketball", Icons.Outlined.SportsBasketball),
            EnvelopeIconOption("tennis", "Tennis", Icons.Outlined.SportsTennis),
            EnvelopeIconOption("electronics", "Electronics", Icons.Outlined.Devices),
            EnvelopeIconOption("garden", "Garden", Icons.Outlined.Yard),
            EnvelopeIconOption("shopping", "Shopping", Icons.Outlined.Storefront),
            EnvelopeIconOption("wedding", "Wedding", Icons.Outlined.Favorite),
            EnvelopeIconOption("celebrations", "Celebrations", Icons.Outlined.Celebration),
            EnvelopeIconOption("charity", "Charity", Icons.Outlined.VolunteerActivism),
            EnvelopeIconOption("emergency", "Emergency", Icons.Outlined.WarningAmber),
            EnvelopeIconOption("taxes", "Taxes", Icons.Outlined.AccountBalance),
            EnvelopeIconOption("debt", "Debt", Icons.Outlined.CreditCard),
            EnvelopeIconOption("cash", "Cash", Icons.Outlined.Payments),
            EnvelopeIconOption("invoices", "Invoices", Icons.Outlined.RequestQuote),
            EnvelopeIconOption("investing", "Investing", Icons.Outlined.TrendingUp),
        )

    fun normalized(token: String): String =
        options.firstOrNull { it.token == token }?.token ?: DefaultToken

    fun option(token: String): EnvelopeIconOption =
        options.firstOrNull { it.token == token } ?: options.first()
}

@Composable
fun EnvelopeCategoryIcon(
    token: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val option = EnvelopeIconCatalog.option(token)
    Icon(
        imageVector = option.imageVector,
        contentDescription = null,
        tint = tint,
        modifier = modifier,
    )
}
