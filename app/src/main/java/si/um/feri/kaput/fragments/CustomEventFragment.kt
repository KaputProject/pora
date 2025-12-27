package si.um.feri.kaput.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import si.um.feri.kaput.MyApplication
import si.um.feri.kaput.databinding.FragmentCustomEventBinding

/**
 * A simple [Fragment] subclass.
 * Use the [CustomEventFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CustomEventFragment : Fragment() {
    private var _binding: FragmentCustomEventBinding? = null
    private val binding get() = _binding!!
    private lateinit var app: MyApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomEventBinding.inflate(inflater, container, false)
        app = requireActivity().application as MyApplication
        return binding.root
    }
}