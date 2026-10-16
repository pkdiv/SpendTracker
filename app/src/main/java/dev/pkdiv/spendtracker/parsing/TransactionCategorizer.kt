package dev.pkdiv.spendtracker.parsing

import javax.inject.Inject

/**
 * Deterministic, on-device default categorization for parsed merchants.
 *
 * The repository consults user overrides in `merchant_categories` first, then
 * falls back to these ordered rules. Matching is case-insensitive substring
 * matching, so the list is ordered from most specific to most generic.
 */
class TransactionCategorizer @Inject constructor() {

    fun categorize(merchant: String): TransactionCategory {
        val normalized = merchant.trim()
        for ((keyword, category) in RULES) {
            if (normalized.contains(keyword, ignoreCase = true)) return category
        }
        return TransactionCategory.OTHER
    }

    companion object {
        private val RULES: List<Pair<String, TransactionCategory>> = listOf(
            "SELF TRANSFER" to TransactionCategory.SELF_TRANSFER,
            "OWN ACCOUNT" to TransactionCategory.SELF_TRANSFER,

            // Entertainment before generic shopping so "Amazon Prime" stays
            // entertainment rather than matching "Amazon".
            "PRIME VIDEO" to TransactionCategory.ENTERTAINMENT,
            "AMAZON PRIME" to TransactionCategory.ENTERTAINMENT,
            "HOTSTAR" to TransactionCategory.ENTERTAINMENT,
            "DISNEY" to TransactionCategory.ENTERTAINMENT,
            "NETFLIX" to TransactionCategory.ENTERTAINMENT,
            "SPOTIFY" to TransactionCategory.ENTERTAINMENT,
            "YOUTUBE" to TransactionCategory.ENTERTAINMENT,
            "BOOKMYSHOW" to TransactionCategory.ENTERTAINMENT,
            "PVR" to TransactionCategory.ENTERTAINMENT,
            "INOX" to TransactionCategory.ENTERTAINMENT,
            "PLAYSTATION" to TransactionCategory.ENTERTAINMENT,
            "STEAM" to TransactionCategory.ENTERTAINMENT,

            "APOLLO" to TransactionCategory.HEALTH,
            "PHARM" to TransactionCategory.HEALTH,
            "MEDICAL" to TransactionCategory.HEALTH,
            "HOSPITAL" to TransactionCategory.HEALTH,
            "CLINIC" to TransactionCategory.HEALTH,
            "DENTAL" to TransactionCategory.HEALTH,
            "MEDICINE" to TransactionCategory.HEALTH,
            "DIAGNOSTIC" to TransactionCategory.HEALTH,
            "NETMEDS" to TransactionCategory.HEALTH,
            "PHARMEASY" to TransactionCategory.HEALTH,
            "1MG" to TransactionCategory.HEALTH,
            "GYM" to TransactionCategory.HEALTH,

            "UBER" to TransactionCategory.TRANSPORT,
            "OLA" to TransactionCategory.TRANSPORT,
            "IRCTC" to TransactionCategory.TRANSPORT,
            "RAILWAY" to TransactionCategory.TRANSPORT,
            "METRO" to TransactionCategory.TRANSPORT,
            "PETROL" to TransactionCategory.TRANSPORT,
            "INDIANOIL" to TransactionCategory.TRANSPORT,
            "BPCL" to TransactionCategory.TRANSPORT,
            "HPCL" to TransactionCategory.TRANSPORT,
            "FUEL" to TransactionCategory.TRANSPORT,
            "TOLL" to TransactionCategory.TRANSPORT,
            "PARKING" to TransactionCategory.TRANSPORT,
            "CAB" to TransactionCategory.TRANSPORT,
            "TAXI" to TransactionCategory.TRANSPORT,

            // Food & groceries before generic shopping so "Instamart"/"BigBasket"
            // aren't caught by "mart"/"basket".
            "SWIGGY" to TransactionCategory.FOOD,
            "ZOMATO" to TransactionCategory.FOOD,
            "DOMINO" to TransactionCategory.FOOD,
            "PIZZA" to TransactionCategory.FOOD,
            "KFC" to TransactionCategory.FOOD,
            "MCDONALD" to TransactionCategory.FOOD,
            "BURGER" to TransactionCategory.FOOD,
            "CAFE" to TransactionCategory.FOOD,
            "RESTAURANT" to TransactionCategory.FOOD,
            "BAKERY" to TransactionCategory.FOOD,
            "BIRYANI" to TransactionCategory.FOOD,
            "GROCERY" to TransactionCategory.FOOD,
            "BIGBASKET" to TransactionCategory.FOOD,
            "BLINKIT" to TransactionCategory.FOOD,
            "INSTAMART" to TransactionCategory.FOOD,
            "ZEPTO" to TransactionCategory.FOOD,
            "GROFERS" to TransactionCategory.FOOD,
            "FOOD" to TransactionCategory.FOOD,

            "AMAZON" to TransactionCategory.SHOPPING,
            "FLIPKART" to TransactionCategory.SHOPPING,
            "MYNTRA" to TransactionCategory.SHOPPING,
            "SNAPDEAL" to TransactionCategory.SHOPPING,
            "MEESHO" to TransactionCategory.SHOPPING,
            "AJIO" to TransactionCategory.SHOPPING,
            "CROMA" to TransactionCategory.SHOPPING,
            "RELIANCE DIGITAL" to TransactionCategory.SHOPPING,
            "SHOP" to TransactionCategory.SHOPPING,
            "MART" to TransactionCategory.SHOPPING,
            "BAZAAR" to TransactionCategory.SHOPPING,
            "RETAIL" to TransactionCategory.SHOPPING,
            "STORE" to TransactionCategory.SHOPPING,

            "ELECTRICITY" to TransactionCategory.BILLS,
            "WATER BILL" to TransactionCategory.BILLS,
            "BROADBAND" to TransactionCategory.BILLS,
            "RECHARGE" to TransactionCategory.BILLS,
            "AIRTEL" to TransactionCategory.BILLS,
            "JIO" to TransactionCategory.BILLS,
            "VODAFONE" to TransactionCategory.BILLS,
            "BSNL" to TransactionCategory.BILLS,
            "DTH" to TransactionCategory.BILLS,
            "INSURANCE" to TransactionCategory.BILLS,
            "LOAN" to TransactionCategory.BILLS,
            "EMI" to TransactionCategory.BILLS,
            "ELECTRIC" to TransactionCategory.BILLS,
            "WIFI" to TransactionCategory.BILLS,
            "BILL" to TransactionCategory.BILLS,
        )
    }
}
