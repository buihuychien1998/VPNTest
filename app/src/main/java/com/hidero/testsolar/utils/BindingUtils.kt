package com.hidero.testsolar.utils

import android.databinding.BindingAdapter
import android.support.annotation.DrawableRes
import android.widget.ImageView

@BindingAdapter("android:src")
fun setImageViewResource(imageView: ImageView, @DrawableRes resource: Int) {
    imageView.setImageResource(resource)
}