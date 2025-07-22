package id.rhiquest.mozzic.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding
import id.rhiquest.mozzic.R

abstract class BaseFragment<VB: ViewBinding, VM: ViewModel> : Fragment() {

    protected abstract val viewModel: VM
    protected var binding:VB? = null
    abstract val inflateBinding: (LayoutInflater, ViewGroup?, Boolean) -> VB

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = inflateBinding.invoke(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObserver()
    }

    protected abstract fun initObserver()

    protected abstract fun initView()

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

}