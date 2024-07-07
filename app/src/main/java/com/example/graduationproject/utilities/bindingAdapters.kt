package com.example.graduationproject.utilities


import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar

import android.widget.TextView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.graduationproject.R

@BindingAdapter("app:isVisibleOrInVisible")
fun isVisibleOrInVisible(view: View,isVisible:Boolean) {
    view.visibility =  if (isVisible) View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("app:isVisibleOrGone")
fun isVisibleOrGone(view: View,isVisible:Boolean) {
    view.visibility =  if (isVisible) View.VISIBLE else View.GONE
}







