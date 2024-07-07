package com.example.graduationproject.ui.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

abstract class BaseActivity <VDB : ViewDataBinding>: AppCompatActivity() {

    abstract val activityLayoutId : Int
    private lateinit var _binding : VDB
    val binding : VDB get() =_binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DataBindingUtil.setContentView(this,activityLayoutId)
    }
}