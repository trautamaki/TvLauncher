/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.tv.launcher.model

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

class ActivityLauncher(
    label: String,
    icon: Drawable,
    context: Context,
    private val intent: Intent,
) : Launchable(label, "", icon, context) {
    override var hasMenu: Boolean = false

    init {
        icon.setTint(this.context.getColor(android.R.color.black))
    }

    override fun setIntent() = intent
}
