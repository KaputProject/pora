package si.um.feri.kaput.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.squareup.moshi.Moshi
import org.json.JSONObject
import si.um.feri.kaput.MyApplication
import si.um.feri.kaput.databinding.FragmentSimulateBinding
import si.um.feri.kaput.utils.MqttUtil
import si.um.feri.kaput.classes.SimulationParameters
import si.um.feri.kaput.models.LocationItem
import si.um.feri.kaput.models.LocationResponse
import kotlin.toString

class SimulateFragment : Fragment() {
    private var _binding: FragmentSimulateBinding? = null
    private val binding get() = _binding!!
    private lateinit var app: MyApplication
    private lateinit var simulationParameters: SimulationParameters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSimulateBinding.inflate(inflater, container, false)
        app = requireActivity().application as MyApplication
        simulationParameters = SimulationParameters(app)
        Log.d("SimulateFragment", app.JWTtoken)
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
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.submitButton.setOnClickListener {
            MqttUtil.publish(
                app.mqttClient,
                MqttUtil.SIMULATION_TOPIC,
                "Some random data.... in simulate fragment"
            )
        }

        binding.priceRangeBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val min = simulationParameters.priceRange.first
                val value = min + progress
                simulationParameters.updateCurrentPrice(value)
                updatePriceLabel(value)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.locationInputText.setOnClickListener {
            simulationParameters.setLocationOptions()
            Log.d(
                "SimulateFragment", "Location options: ${simulationParameters.locationOptions}"
            )
            showLocationMultiSelectDialog()
        }
    }

    private fun initTimePeriodBar() {
        val periodRange = simulationParameters.currentTimePeriod
        val min = periodRange.first
        val max = periodRange.second
        binding.timePeriodBar.max = max - min
        binding.timePeriodBar.progress = 0
    }

    private fun updateTimePeriodLabel(value: Int) {
        val label = if (simulationParameters.FastTestToggle) {
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
        val jsonObject: JSONObject = simulationParameters.locationOptions
        val jsonString = jsonObject.toString()

        val moshi =
            Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()
        val adapter = moshi.adapter(LocationResponse::class.java)

        val locations = try {
            adapter.fromJson(jsonString)?.locations ?: emptyList<LocationItem>()
        } catch (e: Exception) {
            Log.e("SimulateFragment", "Error parsing locations JSON", e)
            emptyList<LocationItem>()
        }

        if (locations.isEmpty()) return

        val names = locations.map { it.name }.toTypedArray()

        // Prefill from previously selectedLocations
        val checkedItems = BooleanArray(names.size) { index ->
            val id = locations[index]._id
            simulationParameters.isLocationSelected(id)
        }

        val builder =
            androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle("Izberi lokacije")
                .setMultiChoiceItems(names, checkedItems) { _, which, isChecked ->
                    checkedItems[which] = isChecked
                    simulationParameters.updateSelectedLocations(locations, checkedItems)

                    val selected = locations.filterIndexed { index, _ -> checkedItems[index] }
                    val selectedNames = if (selected.size > 3) {
                        selected.take(3).joinToString(", ") { it.name } + " ..."
                    } else {
                        selected.joinToString(", ") { it.name }
                    }
                    binding.locationInputText.text = selectedNames
                }.setPositiveButton("OK") { dialog, _ ->
                    simulationParameters.updateSelectedLocations(locations, checkedItems)
                    val selected = locations.filterIndexed { index, _ -> checkedItems[index] }
                    val selectedNames = if (selected.size > 3) {
                        selected.take(3).joinToString(", ") { it.name } + " ..."
                    } else {
                        selected.joinToString(", ") { it.name }
                    }
                    binding.locationInputText.text = selectedNames
                    dialog.dismiss()
                }.setNegativeButton("Prekliči") { dialog, _ ->
                    dialog.dismiss()
                }.setNeutralButton("Select all") { dialog, _ ->
                    for (i in checkedItems.indices) {
                        checkedItems[i] = true
                        val listView = (dialog as androidx.appcompat.app.AlertDialog).listView
                        listView.setItemChecked(i, true)
                    }
                    simulationParameters.updateSelectedLocations(locations, checkedItems)
                    val selectedNames = if (locations.size > 3) {
                        locations.take(3).joinToString(", ") { it.name } + " ..."
                    } else {
                        locations.joinToString(", ") { it.name }
                    }
                    binding.locationInputText.text = selectedNames
                }
        builder.show()
    }
}