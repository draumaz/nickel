package io.github.draumaz.nickel.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CurrencyBitcoin
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SmokingRooms
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.graphics.shapes.RoundedPolygon
import java.io.Serializable

enum class Kind { EXPENSE, INCOME }

/** Which tonal family a category's badge is drawn in. */
enum class Tone { PRIMARY, SECONDARY, TERTIARY }

/**
 * A transaction category. Built-in categories use the stable ids of the original enum
 * (e.g. `GROCERIES`) so older ledgers keep loading; custom ones are created by the user
 * or by importing a CSV. Icons and shapes are stored as registry keys so the category
 * stays plain data (and saveable across configuration changes).
 */
data class Category(
    val id: String,
    val label: String,
    val kind: Kind,
    val iconKey: String,
    val shapeKey: String,
    val tone: Tone,
    val builtIn: Boolean = false,
) : Serializable {
    val icon: ImageVector get() = CategoryIcons.get(iconKey)
    val shape: RoundedPolygon get() = CategoryShapes.get(shapeKey)
}

object CategoryIcons {
    private val icons: Map<String, ImageVector> by lazy {
        mapOf(
            "payments" to Icons.Rounded.Payments,
            "work" to Icons.Rounded.Work,
            "bolt" to Icons.Rounded.Bolt,
            "savings" to Icons.Rounded.Savings,
            "gift" to Icons.Rounded.CardGiftcard,
            "undo" to Icons.AutoMirrored.Rounded.Undo,
            "bank" to Icons.Rounded.AccountBalance,
            "cart" to Icons.Rounded.ShoppingCart,
            "restaurant" to Icons.Rounded.Restaurant,
            "home" to Icons.Rounded.Home,
            "car" to Icons.Rounded.DirectionsCar,
            "hospital" to Icons.Rounded.LocalHospital,
            "bag" to Icons.Rounded.ShoppingBag,
            "movie" to Icons.Rounded.Movie,
            "subscriptions" to Icons.Rounded.Subscriptions,
            "flight" to Icons.Rounded.Flight,
            "school" to Icons.Rounded.School,
            "pets" to Icons.Rounded.Pets,
            "more" to Icons.Rounded.MoreHoriz,
            "category" to Icons.Rounded.Category,
            "bitcoin" to Icons.Rounded.CurrencyBitcoin,
            "fastfood" to Icons.Rounded.Fastfood,
            "health" to Icons.Rounded.HealthAndSafety,
            "cafe" to Icons.Rounded.LocalCafe,
            "fitness" to Icons.Rounded.FitnessCenter,
            "games" to Icons.Rounded.SportsEsports,
            "book" to Icons.AutoMirrored.Rounded.MenuBook,
            "phone" to Icons.Rounded.PhoneIphone,
            "shield" to Icons.Rounded.Shield,
            "receipt" to Icons.AutoMirrored.Rounded.ReceiptLong,
            "chart" to Icons.AutoMirrored.Rounded.ShowChart,
            "bar" to Icons.Rounded.LocalBar,
            "clothes" to Icons.Rounded.Checkroom,
            "spa" to Icons.Rounded.Spa,
            "party" to Icons.Rounded.Celebration,
            "baby" to Icons.Rounded.ChildCare,
            "charity" to Icons.Rounded.VolunteerActivism,
            "gas" to Icons.Rounded.LocalGasStation,
            "wifi" to Icons.Rounded.Wifi,
            "water" to Icons.Rounded.WaterDrop,
            "outdoors" to Icons.Rounded.Park,
            "art" to Icons.Rounded.Brush,
            "music" to Icons.Rounded.MusicNote,
            "smoking" to Icons.Rounded.SmokingRooms,
        )
    }

    fun get(key: String): ImageVector = icons[key] ?: Icons.Rounded.Category

    // Keyword hints for naming icons of imported categories. First match wins.
    private val hints = listOf(
        listOf("crypto", "bitcoin", "btc", "eth") to "bitcoin",
        listOf("saving", "invest", "retire", "401k", "ira") to "savings",
        listOf("stock", "dividend", "trading") to "chart",
        listOf("grocer", "supermarket") to "cart",
        listOf("coffee", "cafe", "café") to "cafe",
        listOf("restaurant", "dining", "dinner", "lunch") to "restaurant",
        listOf("food", "snack", "candy", "junk", "takeout", "eat") to "fastfood",
        listOf("bar", "alcohol", "beer", "wine", "drink") to "bar",
        listOf("nicotine", "vape", "vaping", "smok", "cigar", "tobacco", "zyn") to "smoking",
        listOf("health", "medical", "doctor", "pharm", "dental", "therapy") to "health",
        listOf("hospital") to "hospital",
        listOf("energy", "electric", "power", "utilit") to "bolt",
        listOf("water") to "water",
        listOf("internet", "wifi") to "wifi",
        listOf("phone", "mobile", "cell") to "phone",
        listOf("rent", "mortgage", "housing", "home") to "home",
        listOf("gas", "fuel") to "gas",
        listOf("transport", "transit", "car", "uber", "lyft", "bus", "train", "parking") to "car",
        listOf("travel", "flight", "vacation", "trip", "hotel") to "flight",
        listOf("subscri", "stream", "netflix", "spotify") to "subscriptions",
        listOf("shop", "amazon") to "bag",
        listOf("cloth", "fashion", "apparel") to "clothes",
        listOf("gym", "fitness", "sport") to "fitness",
        listOf("game", "gaming") to "games",
        listOf("movie", "cinema", "entertain", "fun") to "movie",
        listOf("music", "concert") to "music",
        listOf("book", "education", "school", "tuition", "course") to "school",
        listOf("pet", "dog", "cat", "vet") to "pets",
        listOf("gift", "present") to "gift",
        listOf("charity", "donat") to "charity",
        listOf("insur") to "shield",
        listOf("tax", "bill", "fee", "fine", "consequence") to "receipt",
        listOf("beauty", "spa", "hair", "self care", "selfcare") to "spa",
        listOf("kid", "baby", "child") to "baby",
        listOf("party", "celebrat") to "party",
        listOf("art", "craft", "hobby") to "art",
        listOf("garden", "outdoor", "park") to "outdoors",
        listOf("paycheck", "salary", "wage", "payroll") to "payments",
        listOf("freelance", "side", "gig", "work", "job") to "work",
        listOf("refund", "return", "reimburse") to "undo",
        listOf("interest", "bank") to "bank",
    )

    fun guess(label: String): String {
        // Match hints against word starts so "Refund" isn't "fun" and "Parent" isn't "rent".
        val words = label.lowercase().split(Regex("[^\\p{L}0-9]+")).filter { it.isNotEmpty() }
        return hints.firstOrNull { (prefixes, _) ->
            prefixes.any { p -> words.any { it.startsWith(p) } }
        }?.second ?: "category"
    }
}

object CategoryShapes {
    private val shapes: Map<String, RoundedPolygon> by lazy {
        mapOf(
            "cookie4" to MaterialShapes.Cookie4Sided,
            "cookie6" to MaterialShapes.Cookie6Sided,
            "cookie7" to MaterialShapes.Cookie7Sided,
            "cookie9" to MaterialShapes.Cookie9Sided,
            "cookie12" to MaterialShapes.Cookie12Sided,
            "clover4" to MaterialShapes.Clover4Leaf,
            "clover8" to MaterialShapes.Clover8Leaf,
            "sunny" to MaterialShapes.Sunny,
            "flower" to MaterialShapes.Flower,
            "pill" to MaterialShapes.Pill,
            "puffy" to MaterialShapes.Puffy,
            "arch" to MaterialShapes.Arch,
            "burst" to MaterialShapes.SoftBurst,
            "gem" to MaterialShapes.Gem,
            "slanted" to MaterialShapes.Slanted,
            "ghostish" to MaterialShapes.Ghostish,
            "pentagon" to MaterialShapes.Pentagon,
            "bun" to MaterialShapes.Bun,
            "heart" to MaterialShapes.Heart,
            "puffyDiamond" to MaterialShapes.PuffyDiamond,
        )
    }

    /** Shapes handed out to imported categories, in rotation. */
    val keys = listOf(
        "cookie7", "clover4", "sunny", "flower", "puffyDiamond", "cookie12", "gem",
        "bun", "clover8", "pentagon", "cookie6", "heart", "puffy", "slanted",
    )

    fun get(key: String): RoundedPolygon = shapes[key] ?: MaterialShapes.Cookie9Sided
}

object Categories {
    private fun builtIn(id: String, label: String, kind: Kind, icon: String, tone: Tone, shape: String) =
        Category(id, label, kind, icon, shape, tone, builtIn = true)

    val builtIns: List<Category> = listOf(
        // Income
        builtIn("PAYCHECK", "Paycheck", Kind.INCOME, "payments", Tone.PRIMARY, "cookie9"),
        builtIn("SALARY", "Salary", Kind.INCOME, "work", Tone.TERTIARY, "clover4"),
        builtIn("FREELANCE", "Freelance", Kind.INCOME, "bolt", Tone.SECONDARY, "sunny"),
        builtIn("INTEREST", "Interest", Kind.INCOME, "savings", Tone.PRIMARY, "cookie6"),
        builtIn("GIFT", "Gift", Kind.INCOME, "gift", Tone.TERTIARY, "flower"),
        builtIn("REFUND", "Refund", Kind.INCOME, "undo", Tone.SECONDARY, "pill"),
        builtIn("OTHER_INCOME", "Other", Kind.INCOME, "bank", Tone.PRIMARY, "puffy"),
        // Expenses
        builtIn("GROCERIES", "Groceries", Kind.EXPENSE, "cart", Tone.PRIMARY, "cookie9"),
        builtIn("DINING", "Dining", Kind.EXPENSE, "restaurant", Tone.TERTIARY, "flower"),
        builtIn("RENT", "Rent", Kind.EXPENSE, "home", Tone.SECONDARY, "arch"),
        builtIn("UTILITIES", "Utilities", Kind.EXPENSE, "bolt", Tone.PRIMARY, "burst"),
        builtIn("TRANSPORT", "Transport", Kind.EXPENSE, "car", Tone.SECONDARY, "pill"),
        builtIn("MEDICAL", "Medical", Kind.EXPENSE, "hospital", Tone.TERTIARY, "cookie4"),
        builtIn("SHOPPING", "Shopping", Kind.EXPENSE, "bag", Tone.PRIMARY, "gem"),
        builtIn("ENTERTAINMENT", "Fun", Kind.EXPENSE, "movie", Tone.TERTIARY, "sunny"),
        builtIn("SUBSCRIPTIONS", "Subscriptions", Kind.EXPENSE, "subscriptions", Tone.SECONDARY, "cookie12"),
        builtIn("TRAVEL", "Travel", Kind.EXPENSE, "flight", Tone.PRIMARY, "clover8"),
        builtIn("EDUCATION", "Education", Kind.EXPENSE, "school", Tone.SECONDARY, "slanted"),
        builtIn("PETS", "Pets", Kind.EXPENSE, "pets", Tone.TERTIARY, "ghostish"),
        builtIn("NICOTINE", "Nicotine", Kind.EXPENSE, "smoking", Tone.SECONDARY, "bun"),
        builtIn("INVESTMENTS", "Investments", Kind.EXPENSE, "chart", Tone.PRIMARY, "pentagon"),
        builtIn("OTHER_EXPENSE", "Other", Kind.EXPENSE, "more", Tone.SECONDARY, "puffy"),
    )

    fun fallback(kind: Kind): Category =
        builtIns.first { it.id == if (kind == Kind.INCOME) "OTHER_INCOME" else "OTHER_EXPENSE" }

    /**
     * Builds a custom category for a label from an import. Shape and tone are picked
     * deterministically from the name so the same label always looks the same.
     */
    fun custom(label: String, kind: Kind): Category {
        val slug = label.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "category" }
        val hash = label.lowercase().hashCode().let { if (it < 0) -it else it }
        return Category(
            id = "custom:${kind.name.lowercase()}:$slug",
            label = label,
            kind = kind,
            iconKey = CategoryIcons.guess(label),
            shapeKey = CategoryShapes.keys[hash % CategoryShapes.keys.size],
            tone = Tone.entries[hash % Tone.entries.size],
        )
    }

    /** Preselected for a new entry: the first built-in of its kind (Paycheck, Groceries). */
    fun default(kind: Kind): Category = builtIns.first { it.kind == kind }

    /** Built-in and custom categories together, alphabetically, with "Other" last. */
    fun forKind(kind: Kind, custom: List<Category>): List<Category> =
        (builtIns.filter { it.kind == kind && !it.id.startsWith("OTHER") } + custom.filter { it.kind == kind })
            .sortedBy { it.label.lowercase() } +
            fallback(kind)
}

data class Transaction(
    val id: Long,
    /** Always positive; the sign comes from [kind]. */
    val amountCents: Long,
    val kind: Kind,
    val category: Category,
    val note: String,
    val timestamp: Long,
) {
    val signedCents: Long get() = if (kind == Kind.INCOME) amountCents else -amountCents
}
