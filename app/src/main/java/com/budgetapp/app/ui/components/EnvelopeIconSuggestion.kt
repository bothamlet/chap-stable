package com.budgetapp.app.ui.components

import java.text.Normalizer
import java.util.Locale

private data class IconSuggestionRule(
    val token: String,
    val keywords: List<String>,
)

private val iconSuggestionRules =
    listOf(
        IconSuggestionRule("ice_cream", listOf("ice cream", "ledai")),
        IconSuggestionRule("junk_food", listOf("junk food", "fast food", "takeaway", "greitas maistas")),
        IconSuggestionRule("dry_cleaning", listOf("dry cleaning", "cheminis valymas")),
        IconSuggestionRule("groceries", listOf("groceries", "grocery", "food shop", "maistas", "produktai", "parduotuve")),
        IconSuggestionRule("sweets", listOf("sweets", "candy", "chocolate", "dessert", "saldumynai", "sokoladas")),
        IconSuggestionRule("bakery", listOf("bakery", "bread", "pastry", "kepykla", "duona")),
        IconSuggestionRule("dining", listOf("dining", "restaurant", "lunch", "dinner", "pietus", "vakariene", "restoranas")),
        IconSuggestionRule("coffee", listOf("coffee", "cafe", "cappuccino", "kava", "kavine")),
        IconSuggestionRule("drinks", listOf("drinks", "bar", "cocktails", "gerimai", "kokteiliai")),
        IconSuggestionRule("alcohol", listOf("alcohol", "beer", "wine", "alus", "vynas", "alkoholis")),
        IconSuggestionRule("tobacco", listOf("tobacco", "cigarettes", "smokes", "tabakas", "cigaretes")),
        IconSuggestionRule("rent", listOf("apartment rent", "rent", "mortgage", "nuoma", "busto paskola")),
        IconSuggestionRule("electricity", listOf("electricity", "power", "elektra")),
        IconSuggestionRule("water", listOf("water", "vanduo")),
        IconSuggestionRule("heating", listOf("heating", "gas bill", "sildymas", "dujos")),
        IconSuggestionRule("internet", listOf("internet", "wifi", "broadband", "internetas")),
        IconSuggestionRule("phone", listOf("phone", "mobile", "telefonas", "mobilus")),
        IconSuggestionRule("subscriptions", listOf("subscriptions", "subscription", "netflix", "spotify", "prenumeratos")),
        IconSuggestionRule("bills", listOf("bills", "utilities", "saskaitos", "mokesciai")),
        IconSuggestionRule("furniture", listOf("furniture", "furnishing", "baldai")),
        IconSuggestionRule("appliances", listOf("appliances", "kitchen", "technika", "virtuve")),
        IconSuggestionRule("cleaning", listOf("cleaning", "cleaner", "valymas")),
        IconSuggestionRule("laundry", listOf("laundry", "skalbykla", "skalbimas")),
        IconSuggestionRule("home", listOf("home", "house", "apartment", "bustas", "namai")),
        IconSuggestionRule("fuel", listOf("fuel", "petrol", "gasoline", "degalai", "benzinas")),
        IconSuggestionRule("parking", listOf("parking", "parkavimas")),
        IconSuggestionRule("taxi", listOf("taxi", "uber", "bolt ride", "taksi")),
        IconSuggestionRule("train", listOf("train", "rail", "traukinys")),
        IconSuggestionRule("bus", listOf("bus", "transit", "autobusas")),
        IconSuggestionRule("bicycle", listOf("bicycle", "bike", "dviratis")),
        IconSuggestionRule("motorcycle", listOf("motorcycle", "motorbike", "motociklas")),
        IconSuggestionRule("transport", listOf("transport", "car", "vehicle", "automobilis", "masina")),
        IconSuggestionRule("repairs", listOf("repairs", "maintenance", "service", "remontas", "taisymas")),
        IconSuggestionRule("travel", listOf("travel", "holiday", "vacation", "flight", "kelione", "atostogos", "skrydis")),
        IconSuggestionRule("hotel", listOf("hotel", "accommodation", "viesbutis", "nakvyne")),
        IconSuggestionRule("pharmacy", listOf("pharmacy", "medicine", "medication", "vaistine", "vaistai")),
        IconSuggestionRule("health", listOf("health", "doctor", "dentist", "medical", "sveikata", "gydytojas", "dantistas")),
        IconSuggestionRule("fitness", listOf("fitness", "gym", "workout", "sporto sale", "treniruotes")),
        IconSuggestionRule("beauty", listOf("beauty", "cosmetics", "spa", "grozis", "kosmetika")),
        IconSuggestionRule("haircuts", listOf("haircut", "hairdresser", "barber", "kirpykla", "kirpimas")),
        IconSuggestionRule("clothing", listOf("clothing", "clothes", "shoes", "drabuziai", "batai")),
        IconSuggestionRule("baby", listOf("baby", "newborn", "kudikis")),
        IconSuggestionRule("children", listOf("children", "kids", "childcare", "vaikai")),
        IconSuggestionRule("pets", listOf("pets", "pet", "dog", "cat", "augintiniai", "suo", "kate")),
        IconSuggestionRule("education", listOf("education", "school", "tuition", "mokslai", "mokykla")),
        IconSuggestionRule("books", listOf("books", "book", "reading", "knygos", "knyga")),
        IconSuggestionRule("gaming", listOf("gaming", "games", "playstation", "xbox", "zaidimai")),
        IconSuggestionRule("entertainment", listOf("entertainment", "cinema", "movies", "pramogos", "kinas")),
        IconSuggestionRule("music", listOf("music", "concert", "muzika", "koncertas")),
        IconSuggestionRule("art", listOf("art", "crafts", "painting", "menas", "rankdarbiai")),
        IconSuggestionRule("photography", listOf("photography", "camera", "photos", "fotografija")),
        IconSuggestionRule("football", listOf("football", "soccer", "futbolas")),
        IconSuggestionRule("basketball", listOf("basketball", "krepsinis")),
        IconSuggestionRule("tennis", listOf("tennis", "tenisas")),
        IconSuggestionRule("electronics", listOf("electronics", "computer", "laptop", "gadgets", "elektronika", "kompiuteris")),
        IconSuggestionRule("garden", listOf("garden", "plants", "yard", "sodas", "augalai")),
        IconSuggestionRule("shopping", listOf("shopping", "purchases", "pirkiniai", "apsipirkimas")),
        IconSuggestionRule("gift", listOf("gift", "gifts", "presents", "dovana", "dovanos")),
        IconSuggestionRule("wedding", listOf("wedding", "vestuves")),
        IconSuggestionRule("celebrations", listOf("birthday", "party", "celebration", "gimtadienis", "svente")),
        IconSuggestionRule("charity", listOf("charity", "donation", "donations", "labdara", "auka")),
        IconSuggestionRule("emergency", listOf("emergency", "rainy day", "nenumatytos", "nelaime")),
        IconSuggestionRule("taxes", listOf("taxes", "tax", "mokestine", "mokesciai valstybei")),
        IconSuggestionRule("debt", listOf("debt", "loan", "credit card", "skola", "paskola")),
        IconSuggestionRule("invoices", listOf("invoice", "invoices", "saskaita faktura")),
        IconSuggestionRule("investing", listOf("investing", "investment", "stocks", "investicijos", "akcijos")),
        IconSuggestionRule("savings", listOf("savings", "save", "taupymas", "santaupos")),
        IconSuggestionRule("insurance", listOf("insurance", "draudimas")),
        IconSuggestionRule("work", listOf("work", "office", "darbas", "biuras")),
        IconSuggestionRule("cash", listOf("cash", "grynieji")),
        IconSuggestionRule("goal", listOf("goal", "target", "tikslas")),
    )

fun suggestEnvelopeIconToken(name: String): String? {
    val normalizedName = normalizeSuggestionText(name)
    if (normalizedName.isBlank()) {
        return null
    }
    val searchableName = " $normalizedName "
    return iconSuggestionRules
        .mapNotNull { rule ->
            val matchLength =
                rule.keywords
                    .map(::normalizeSuggestionText)
                    .filter { keyword -> searchableName.contains(" $keyword ") }
                    .maxOfOrNull(String::length)
            matchLength?.let { rule.token to it }
        }
        .maxByOrNull { (_, matchLength) -> matchLength }
        ?.first
}

private fun normalizeSuggestionText(value: String): String =
    Normalizer
        .normalize(value, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase(Locale.ROOT)
        .replace("[^a-z0-9]+".toRegex(), " ")
        .trim()
