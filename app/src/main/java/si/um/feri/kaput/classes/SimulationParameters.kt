package si.um.feri.kaput.classes

import android.util.Log
import si.um.feri.kaput.MyApplication
import si.um.feri.kaput.models.Location

data class SimulationToggles(
    var family: Boolean = false,
    var allowNegativeAmount: Boolean = false,
    var fastTest: Boolean = false
)

data class TimeRanges(
    val normal: Pair<Int, Int> = 1 to 12,
    val fast: Pair<Int, Int> = 1 to 60
) {
    fun current(isFast: Boolean): Pair<Int, Int> = if (isFast) fast else normal
}

class SimulationParameters(private val app: MyApplication) {

    private val toggles = SimulationToggles()
    private val timeRanges = TimeRanges()

    val fastTestToggle: Boolean
        get() = toggles.fastTest

    val negativeAmountSwitch: Boolean
        get() = toggles.allowNegativeAmount

    val familyToggle: Boolean
        get() = toggles.family

    var selectedLocations: MutableList<Location> = mutableListOf()
        private set

    var locationOptions: List<Location> = emptyList()
        private set

    var priceRange: Pair<Int, Int> = 1 to 1000
        set(value) {
            field = value
            currentPrice = currentPrice.coerceIn(value.first, value.second)
        }

    var currentPrice: Int = 1
        private set

    var numberToGenerate: Int = 0

    val currentTimePeriod: Pair<Int, Int>
        get() = timeRanges.current(toggles.fastTest)

    data class FamilyMember(val id: String, val username: String)

    val familyMembers: List<FamilyMember>
        get() {
            val dataManager = app.dataManager
            return if (toggles.family) {
                dataManager.familyMembers.map { pair ->
                    FamilyMember(pair.first ?: "", pair.second ?: "")
                }
            } else {
                val userId = dataManager.userId ?: ""
                val username = dataManager.username
                listOf(FamilyMember(userId, username))
            }
        }

    val userLocations: List<Location>
        get() = app.dataManager.userLocations

    fun switchFamilyToggle(value: Boolean) {
        toggles.family = value
    }

    fun switchTestToggle(value: Boolean) {
        toggles.fastTest = value
    }

    fun switchAmountToggle(value: Boolean) {
        toggles.allowNegativeAmount = value
    }

    fun setLocationOptions() {
        val dataManager = app.dataManager
        locationOptions = if (toggles.family) {
            dataManager.familyLocations
        } else {
            dataManager.userLocations
        }

        locationOptions.forEach { loc ->
            Log.d(
                "SimulationParameters",
                "Location ${loc.identifier} has address: ${loc.address ?: "NO ADDRESS"}"
            )
        }

        Log.d(
            "SimulationParameters",
            "Location options updated: ${locationOptions.size} locations"
        )
    }

    fun updateNumberToGenerate(number: Int) {
        numberToGenerate = number
    }

    fun updateCurrentPrice(value: Int) {
        currentPrice = value.coerceIn(priceRange.first, priceRange.second)
    }

    fun updateSelectedLocations(locations: List<Location>, checked: BooleanArray) {
        selectedLocations.clear()
        locations.forEachIndexed { index, item ->
            if (index < checked.size && checked[index]) {
                selectedLocations.add(item)
            }
        }
        Log.d(
            "SimulationParameters",
            "Selected locations updated: ${selectedLocations.map { it.identifier }}"
        )
    }

    fun isLocationSelected(id: String): Boolean {
        return selectedLocations.any { it._id == id }
    }
}
