package si.um.feri.kaput.classes

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import si.um.feri.kaput.MyApplication
import si.um.feri.kaput.models.LocationItem

class SimulationParameters(private val app: MyApplication) {
    var familyToggle: Boolean = false
    var negativeAmountSwitch: Boolean = false
    var FastTestToggle: Boolean = false
    var timePeriod: Pair<Int, Int> = Pair(1, 12)
    var fastTestTimePeriod: Pair<Int, Int> = Pair(1, 60)
    var selectedLocations: JSONArray = JSONArray()
    var locationOptions: JSONObject = JSONObject()

    var priceRange: Pair<Int, Int> = Pair(1, 1000)
    var currentPrice: Int = priceRange.first
    var numberToGenerate: Int = 0

    val currentTimePeriod: Pair<Int, Int>
        get() = if (FastTestToggle) fastTestTimePeriod else timePeriod

    fun switchFamilyToggle(value: Boolean) {
        familyToggle = value
    }

    fun switchAmountToggle() {
        negativeAmountSwitch = !negativeAmountSwitch
    }

    fun switchTestToggle(value: Boolean) {
        FastTestToggle = value
    }

    fun setLocationOptions() {
        val locationsArray: JSONArray = if (familyToggle) {
            val dataSet = app.familyDataSet
            val statistics = dataSet.optJSONObject("statistics")
            statistics?.optJSONArray("locations") ?: JSONArray()
        } else {
            val dataSet = app.UserDataSet
            val user = dataSet.optJSONObject("user")
            user?.optJSONArray("locations") ?: JSONArray()
        }

        locationOptions = JSONObject().apply {
            put("locations", locationsArray)
        }

        Log.d(
            "SimulationParameters",
            "Selected locations updated: ${locationsArray.length()} items"
        )
    }


    fun updateNumberToGenerate(number: Int) {
        numberToGenerate = number
    }

    fun numberToGenerate(): Int {
        return numberToGenerate
    }

    fun updateCurrentPrice(value: Int) {
        val min = priceRange.first
        val max = priceRange.second
        currentPrice = value.coerceIn(min, max)
    }

    fun updateSelectedLocations(locations: List<LocationItem>, checked: BooleanArray) {
        val array = JSONArray()
        locations.forEachIndexed { index, item ->
            if (checked[index]) {
                val obj = JSONObject().apply {
                    put("id", item._id)
                    put("name", item.name)
                }
                array.put(obj)
            }
        }
        selectedLocations = array

    }

    fun isLocationSelected(id: String): Boolean {
        for (i in 0 until selectedLocations.length()) {
            val obj = selectedLocations.optJSONObject(i)
            if (obj?.optString("id") == id) return true
        }
        return false
    }
}
