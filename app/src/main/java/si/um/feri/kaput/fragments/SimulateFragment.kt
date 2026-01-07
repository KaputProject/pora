package si.um.feri.kaput.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONArray
import org.json.JSONObject
import si.um.feri.kaput.MyApplication
import si.um.feri.kaput.databinding.FragmentSimulateBinding
import si.um.feri.kaput.utils.MqttUtil
import si.um.feri.kaput.classes.SimulationParameters
import si.um.feri.kaput.models.Location
import si.um.feri.kaput.models.LocationResponse
import si.um.feri.kaput.models.Transaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.compareTo
import kotlin.text.format
import kotlin.toString

class SimulateFragment : Fragment() {
    private var _binding: FragmentSimulateBinding? = null
    private val binding get() = _binding!!
    private lateinit var app: MyApplication
    private lateinit var simulationParameters: SimulationParameters
    private var timeRange: Int = 1
    private var changeAmount: Int = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSimulateBinding.inflate(inflater, container, false)
        app = requireActivity().application as MyApplication
        simulationParameters = SimulationParameters(app)
        Log.d("SimulateFragment", app.databaseUtil.JWTtoken)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initButtons()
    }

    private fun initButtons() {
        initTimePeriodBar()
        updateTimePeriodLabel(simulationParameters.currentTimePeriod.first)

        binding.familySwitch.setOnCheckedChangeListener { _, isChecked ->
            simulationParameters.switchFamilyToggle(isChecked)
        }

        binding.testToggle.setOnCheckedChangeListener { _, isChecked ->
            simulationParameters.switchTestToggle(isChecked)
            initTimePeriodBar()
            updateTimePeriodLabel(simulationParameters.currentTimePeriod.first)
        }

        binding.timePeriodBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val periodRange = simulationParameters.currentTimePeriod
                val min = periodRange.first
                val value = progress + min
                updateTimePeriodLabel(value)
                timeRange = value

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.submitButton.setOnClickListener {
            val jsonData = StartSimulation()
            Log.d("SimulateFragment", "Generated JSON data: $jsonData")
            MqttUtil.publish(
                app.mqttClient, MqttUtil.SIMULATION_TOPIC, jsonData
            )
        }

        binding.priceRangeBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val min = simulationParameters.priceRange.first
                val value = min + progress
                simulationParameters.updateCurrentPrice(value)
                updatePriceLabel(value)
                changeAmount = value
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.locationInputText.setOnClickListener {
            simulationParameters.setLocationOptions()
            showLocationMultiSelectDialog()
        }

        binding.transationSwitch.setOnCheckedChangeListener { _, isChecked ->
            simulationParameters.switchAmountToggle(isChecked)
        }

        binding.generateCountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                val number = s?.toString()?.toIntOrNull() ?: 0
                simulationParameters.updateNumberToGenerate(number)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

    }
    // region helpers

    // initialize time period bar based on simulation parameters
    private fun initTimePeriodBar() {
        val periodRange = simulationParameters.currentTimePeriod
        val min = periodRange.first
        val max = periodRange.second
        binding.timePeriodBar.max = max - min
        binding.timePeriodBar.progress = 0
    }

    //posodobi label za časovno obdobje (mesece ali minute)
    private fun updateTimePeriodLabel(value: Int) {
        val label = if (simulationParameters.FastTestToggle) {
            "$value min"
        } else {
            "$value month"
        }
        binding.timepiriodNumber.text = label
    }

    //posodobi label za znesek
    private fun updatePriceLabel(value: Int) {
        binding.priceRangeNumber.text = "${value}€"
    }

    // prikaže dialog za večkratno izbiro lokacij
    private fun showLocationMultiSelectDialog() {
        // vse lokacije iz simulationParameters
        val jsonObject: JSONObject = simulationParameters.locationOptions
        val jsonString = jsonObject.toString()
        // parse JSON using Moshi (neka knjiznica za JSON parsing recommended by copilot)
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(LocationResponse::class.java)

        val locations = try {
            // parse locations from JSON
            adapter.fromJson(jsonString)?.locations ?: emptyList<Location>()
        } catch (e: Exception) {
            Log.e("SimulateFragment", "Error parsing locations JSON", e)
            emptyList<Location>()
        }
        // če ni lokacij, ne prikaži dialoga oz če je kaj šlo narobe pri parsiranju
        if (locations.isEmpty()) return
        // pripravi imena lokacij za prikaz v dialogu
        val names = locations.map { it.identifier }.toTypedArray()

        // Prefill from previously selectedLocations stored in simulationParameters
        val checkedItems = BooleanArray(names.size) { index ->
            val id = locations[index]._id
            simulationParameters.isLocationSelected(id)
        }

        // shranimo trenutno (prejšnje) stanje, da ga lahko povrnemo ob preklicu
        val previousCheckedItems = checkedItems.copyOf()

        val builder = AlertDialog.Builder(requireContext()).setTitle("Izberi lokacije")
            // multi choice items za lokacije
            .setMultiChoiceItems(names, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
                simulationParameters.updateSelectedLocations(locations, checkedItems)

                val selected = locations.filterIndexed { index, _ -> checkedItems[index] }
                val selectedNames = if (selected.size > 3) {
                    selected.take(3).joinToString(", ") { it.identifier.toString() } + " ..."
                } else {
                    selected.joinToString(", ") { it.identifier.toString() }
                }
                binding.locationInputText.text = selectedNames
            } // gumb za OK v dialogu shrani izbiro v simulationParameters
            .setPositiveButton("OK") { dialog, _ ->
                simulationParameters.updateSelectedLocations(locations, checkedItems)
                val selected = locations.filterIndexed { index, _ -> checkedItems[index] }
                val selectedNames = if (selected.size > 3) {
                    selected.take(3).joinToString(", ") { it.identifier.toString() } + " ..."
                } else {
                    selected.joinToString(", ") { it.identifier.toString() }
                }
                binding.locationInputText.text = selectedNames
                dialog.dismiss()
            } // gumb za preklic dialoga povrne izbiro na prejšnjo
            .setNegativeButton("Prekliči") { dialog, _ ->
                // povrnemo prejšnje stanje v simulationParameters
                simulationParameters.updateSelectedLocations(locations, previousCheckedItems)

                val selected = locations.filterIndexed { index, _ -> previousCheckedItems[index] }
                val selectedNames = if (selected.size > 3) {
                    selected.take(3).joinToString(", ") { it.identifier.toString() } + " ..."
                } else {
                    selected.joinToString(", ") { it.identifier.toString() }
                }
                binding.locationInputText.text = selectedNames

                dialog.dismiss()
            } // gumb za izbiro vseh lokacij
            .setNeutralButton("Select all") { dialog, _ ->
                for (i in checkedItems.indices) {
                    checkedItems[i] = true
                    val listView = (dialog as AlertDialog).listView
                    listView.setItemChecked(i, true)
                }
                simulationParameters.updateSelectedLocations(locations, checkedItems)
                val selectedNames = if (locations.size > 3) {
                    locations.take(3).joinToString(", ") { it.identifier.toString() } + " ..."
                } else {
                    locations.joinToString(", ") { it.identifier.toString() }
                }
                binding.locationInputText.text = selectedNames
            }
        builder.show()
    }

    fun StartSimulation(): String {
        val transactions = mutableListOf<JSONObject>()
        val now = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

        // Pridobi družinske člane ali samo trenutnega uporabnika
        val members = simulationParameters.familyMembers

        for (i in 1..simulationParameters.numberToGenerate()) {
            val selectedArray = simulationParameters.selectedLocations
            if (selectedArray.length() == 0 || members.length() == 0) {
                continue
            }

            val randomIndex = (0 until selectedArray.length()).random()
            val jsonLoc = selectedArray.getJSONObject(randomIndex)

            // Naključno izberi družinskega člana
            val randomMemberIndex = (0 until members.length()).random()
            val selectedMember = members.getJSONObject(randomMemberIndex)

            val cal = now.clone() as Calendar
            if (simulationParameters.FastTestToggle) {
                val offsetMinutes = (0..timeRange).random()
                cal.add(Calendar.MINUTE, offsetMinutes)
            } else {
                val offsetMonths = (0..timeRange).random()
                cal.add(Calendar.MONTH, offsetMonths)
            }
            val date = cal.time
            val formatted = sdf.format(date)

            val maxAmount = if (changeAmount <= 0) 1 else changeAmount
            val amount = (1..maxAmount).random().toDouble()

            val outgoing = if (simulationParameters.negativeAmountSwitch) {
                listOf(true, false).random()
            } else {
                true
            }

            val inflow = if (outgoing) amount else 0.0
            val outflow = if (outgoing) 0.0 else amount

            val transactionObj = JSONObject().apply {
                put("_id", UUID.randomUUID().toString())
                put("datetime", formatted)
                put("location", JSONObject().apply {
                    put("_id", jsonLoc.getString("id"))
                    put("lat", if (jsonLoc.has("lat")) jsonLoc.getDouble("lat") else null)
                    put("lng", if (jsonLoc.has("lng")) jsonLoc.getDouble("lng") else null)
                    put(
                        "identifier",
                        if (jsonLoc.has("identifier")) jsonLoc.getString("identifier") else null
                    )
                    put(
                        "address",
                        if (jsonLoc.has("address")) jsonLoc.getString("address") else null
                    )
                    put("users", JSONArray().apply {
                        put(JSONObject().apply {
                            put("_id", selectedMember.optString("_id") ?: "unknown")
                            put("username", selectedMember.optString("username") ?: "unknown")
                            put("numbOftrans", 1)
                            put("inflow", inflow)
                            put("outflow", outflow)
                        })
                    })
                })
            }
            transactions.add(transactionObj)
        }

        val jsonArray = JSONArray()
        transactions.forEach { tx ->
            jsonArray.put(tx)
        }

        return jsonArray.toString()
    }


}