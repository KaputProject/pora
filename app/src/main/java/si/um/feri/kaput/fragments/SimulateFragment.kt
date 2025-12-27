package si.um.feri.kaput.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import si.um.feri.kaput.MyApplication
import si.um.feri.kaput.databinding.FragmentSimulateBinding

/**
 * A simple [Fragment] subclass.
 * Use the [SimulateFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SimulateFragment : Fragment() {
    private var _binding: FragmentSimulateBinding? = null
    private val binding get() = _binding!!
    private lateinit var app: MyApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSimulateBinding.inflate(inflater, container, false)
        app = requireActivity().application as MyApplication
        return binding.root
    }
}