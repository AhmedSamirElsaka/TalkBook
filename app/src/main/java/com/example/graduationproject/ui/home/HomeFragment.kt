package com.example.graduationproject.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.graduationproject.R
import com.example.graduationproject.databinding.HomeFragmentBinding

import com.example.graduationproject.ui.base.BaseFragment
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BaseFragment<HomeFragmentBinding>(){
    override val layoutIdFragment  = R.layout.home_fragment
    override val viewModel: HomeViewModel by  viewModels()

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.logOut.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_homeFragment_to_logInFragment)
        }

    }
}