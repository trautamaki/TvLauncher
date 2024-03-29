/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.tv.launcher.view

import android.animation.AnimatorInflater
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import org.lineageos.tv.launcher.R
import org.lineageos.tv.launcher.model.AppInfo
import org.lineageos.tv.launcher.model.Launchable

open class AppCard : Card {
    // Views
    private val bannerView by lazy { findViewById<ImageView>(R.id.app_banner) }
    private val cardContainer by lazy { findViewById<LinearLayout>(R.id.card_container) }
    private val iconContainer by lazy { findViewById<LinearLayout>(R.id.app_with_icon) }
    private val iconView by lazy { findViewById<ImageView>(R.id.app_icon) }
    private val nameView by lazy { findViewById<TextView>(R.id.app_name) }

    private var hasFocus: Boolean = false

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        stateListAnimator =
            AnimatorInflater.loadStateListAnimator(context, R.anim.app_card_state_animator)

        setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                this.hasFocus = true
                nameView.postDelayed({
                    if (this.hasFocus) {
                        nameView.isSelected = true
                    }
                }, 2000)
            } else {
                nameView.isSelected = false
                this.hasFocus = false
            }
        }
    }

    override fun inflate() {
        inflate(context, R.layout.app_card, this)
    }

    override fun setCardInfo(appInfo: Launchable) {
        super.setCardInfo(appInfo)

        nameView.text = appInfo.label
        iconView.setImageDrawable(appInfo.icon)

        if (appInfo is AppInfo && appInfo.banner != null) {
            // App with a banner
            bannerView.setImageDrawable(appInfo.banner)
            bannerView.visibility = View.VISIBLE
            iconContainer.visibility = View.GONE
            cardContainer.background =
                AppCompatResources.getDrawable(context, R.drawable.card_border_only)
        } else {
            // App with an icon
            iconView.setImageDrawable(appInfo.icon)
        }
    }
}
