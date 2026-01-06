package si.um.feri.kaput.classes

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import si.um.feri.kaput.MyApplication
import si.um.feri.kaput.models.Location

data class SimulationToggles(
    var family: Boolean = false,
    var allowNegativeAmount: Boolean = false,
    var fastTest: Boolean = false
)

data class TimeRanges(
    val normal: Pair<Int, Int> = 1 to 12, val fast: Pair<Int, Int> = 1 to 60
) {
    fun current(isFast: Boolean): Pair<Int, Int> = if (isFast) fast else normal
}

class SimulationParameters(private val app: MyApplication) {

    private val toggles = SimulationToggles()
    private val timeRanges = TimeRanges()

    val FastTestToggle: Boolean
        get() = toggles.fastTest

    val negativeAmountSwitch: Boolean
        get() = toggles.allowNegativeAmount

    val familyToggle: Boolean
        get() = toggles.family

    var selectedLocations: JSONArray = JSONArray()
        private set

    var locationOptions: JSONObject = JSONObject()
        private set

    var priceRange: Pair<Int, Int> = 1 to 1000
        set(value) {
            field = value
            currentPrice = currentPrice.coerceIn(value.first, value.second)
        }

    var currentPrice: Int = priceRange.first
        private set

    var numberToGenerate: Int = 0

    val currentTimePeriod: Pair<Int, Int>
        get() = timeRanges.current(toggles.fastTest)

    // endregion

    // region toggle helpers (backwards compatible)

    fun switchFamilyToggle(value: Boolean) {
        toggles.family = value
    }

    fun switchTestToggle(value: Boolean) {
        toggles.fastTest = value
    }

    fun switchAmountToggle(value: Boolean) {
        toggles.allowNegativeAmount = value
    }

    fun setToggle(
        family: Boolean? = null, allowNegative: Boolean? = null, fastTest: Boolean? = null
    ) {
        family?.let { toggles.family = it }
        allowNegative?.let { toggles.allowNegativeAmount = it }
        fastTest?.let { toggles.fastTest = it }
    }


    fun setLocationOptions() {
        val locationsArray: JSONArray = if (toggles.family) {
            app.familyDataSet.optJSONObject("statistics")?.optJSONArray("locations") ?: JSONArray()
        } else {
            app.UserDataSet.optJSONObject("user")?.optJSONArray("locations") ?: JSONArray()
        }

        locationOptions = JSONObject().apply {
            put("locations", locationsArray)
        }

        Log.d(
            "SimulationParameters", "Selected locations updated: ${locationsArray.length()} items"
        )
    }

    fun updateNumberToGenerate(number: Int) {
        numberToGenerate = number
    }

    fun numberToGenerate(): Int = numberToGenerate

    fun updateCurrentPrice(value: Int) {
        currentPrice = value.coerceIn(priceRange.first, priceRange.second)
    }

    fun updateSelectedLocations(locations: List<Location>, checked: BooleanArray) {
        val array = JSONArray()
        locations.forEachIndexed { index, item ->
            if (index < checked.size && checked[index]) {
                val obj = JSONObject().apply {
                    put("id", item._id)
                    put("name", item.name)
                    item.lat?.let { put("lat", it) }
                    item.lng?.let { put("lng", it) }
                }
                array.put(obj)
            }
        }
        selectedLocations = array
        Log.d(
            "SimulationParameters", "Selected locations updated: $selectedLocations"
        )
    }

    fun isLocationSelected(id: String): Boolean {
        for (i in 0 until selectedLocations.length()) {
            val obj = selectedLocations.optJSONObject(i)
            if (obj?.optString("id") == id) return true
        }
        return false
    }
}
