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
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.json.JSONArray
import si.um.feri.kaput.MyApplication
import si.um.feri.kaput.databinding.FragmentSimulateBinding
import si.um.feri.kaput.classes.SimulationParameters
import si.um.feri.kaput.models.Location
import si.um.feri.kaput.models.LocationUser
import si.um.feri.kaput.models.Transaction
import si.um.feri.kaput.utils.MqttUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.compareTo

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
            val jsonData = startSimulationJson()
            Log.d("SimulateFragment", "Generated JSON data: $jsonData")
            MqttUtil.publish(app.mqttClient, MqttUtil.SIMULATION_TOPIC, jsonData)
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
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val number = s?.toString()?.toIntOrNull() ?: 0
                simulationParameters.updateNumberToGenerate(number)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }
    private fun initTimePeriodBar() {
        val periodRange = simulationParameters.currentTimePeriod
        val min = periodRange.first
        val max = periodRange.second
        binding.timePeriodBar.max = max - min
        binding.timePeriodBar.progress = 0
    }
    private fun updateTimePeriodLabel(value: Int) {
        val label = if (simulationParameters.fastTestToggle) {
            "$value min"
        } else {
            "$value month"
        }
        binding.timepiriodNumber.text = label
    }
    private fun updatePriceLabel(value: Int) {
        binding.priceRangeNumber.text = "${value}€"
    }
    private fun showLocationMultiSelectDialog() {
        val locations = simulationParameters.locationOptions

        if (locations.isEmpty()) return

        val names = locations.map { it.identifier ?: "" }.toTypedArray()

        val checkedItems = BooleanArray(names.size) { index ->
            val id = locations[index]._id
            simulationParameters.isLocationSelected(id)
        }
        val previousCheckedItems = checkedItems.copyOf()
        val builder = AlertDialog.Builder(requireContext()).setTitle("Izberi lokacije")
            .setMultiChoiceItems(names, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
                simulationParameters.updateSelectedLocations(locations, checkedItems)
                updateLocationText(locations, checkedItems)
            }.setPositiveButton("OK") { dialog, _ ->
                simulationParameters.updateSelectedLocations(locations, checkedItems)
                updateLocationText(locations, checkedItems)
                dialog.dismiss()
            }.setNegativeButton("Prekliči") { dialog, _ ->
                simulationParameters.updateSelectedLocations(locations, previousCheckedItems)
                updateLocationText(locations, previousCheckedItems)
                dialog.dismiss()
            }.setNeutralButton("Select all") { dialog, _ ->
                for (i in checkedItems.indices) {
                    checkedItems[i] = true
                    val listView = (dialog as AlertDialog).listView
                    listView.setItemChecked(i, true)
                }
                simulationParameters.updateSelectedLocations(locations, checkedItems)
                updateLocationText(locations, checkedItems)
            }
        builder.show()
    }
    private fun updateLocationText(locations: List<Location>, checkedItems: BooleanArray) {
        val selected = locations.filterIndexed { index, _ -> checkedItems[index] }
        val selectedNames = if (selected.size > 3) {
            selected.take(3).joinToString(", ") { it.identifier ?: "" } + " ..."
        } else {
            selected.joinToString(", ") { it.identifier ?: "" }
        }
        binding.locationInputText.text = selectedNames
    }
    private fun startSimulationJson(): String {
        val transactions = mutableListOf<Transaction>()
        val now = Calendar.getInstance()

        val members = simulationParameters.familyMembers
        val selectedLocations = simulationParameters.selectedLocations

        for (i in 1..simulationParameters.numberToGenerate) {
            if (selectedLocations.isEmpty() || members.isEmpty()) continue

            val randomLocation = selectedLocations.random().copy()
            val randomMember = members.random().copy()

            val cal = now.clone() as Calendar
            if (simulationParameters.fastTestToggle) {
                val offsetMinutes = (0..timeRange).random()
                cal.add(Calendar.MINUTE, offsetMinutes)
            } else {
                val offsetMonths = (0..timeRange).random()
                cal.add(Calendar.MONTH, offsetMonths)
            }
            val date = cal.time

            val maxAmount = if (changeAmount <= 0) 1 else changeAmount
            val amount = (1..maxAmount).random().toDouble()

            val outgoing = if (simulationParameters.negativeAmountSwitch) {
                listOf(true, false).random()
            } else {
                true
            }

            val inflow = if (outgoing) amount else 0.0
            val outflow = if (outgoing) 0.0 else amount

            randomLocation.total_inflow = inflow
            randomLocation.total_outflow = outflow
            randomLocation.numbOfTrans = 1
            val newUsers: List<LocationUser> = listOf(
                LocationUser(
                    userId = randomMember.id,
                    username = randomMember.username,
                    numbOfTrans = 1,
                    inflow = inflow,
                    outflow = outflow
                )
            )
            randomLocation.users = newUsers

            val transaction = Transaction(
                location = randomLocation,
                datetime = date.time
            )
            transactions.add(transaction)
        }

        val gson: Gson = GsonBuilder().create()
        return gson.toJson(transactions)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
