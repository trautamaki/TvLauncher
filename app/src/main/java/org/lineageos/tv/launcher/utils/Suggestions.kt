/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.tv.launcher.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.tv.TvContract
import android.util.Log
import androidx.core.content.edit
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lineageos.tv.launcher.R
import org.lineageos.tv.launcher.model.Channel

@SuppressLint("RestrictedApi")
object Suggestions {
    private const val TAG = "TvLauncher.Suggestions"
    private const val PACKAGE_FRAMEWORK_STUBS = "com.android.tv.frameworkpackagestubs"

    internal var onChannelHiddenCallback: (channelId: Long) -> Unit = {}
    internal var onChannelShownCallback: (channelId: Long) -> Unit = {}
    internal var onChannelSelectedCallback: (channelId: Long, index: Int) -> Unit = { _, _ -> }
    internal var onChannelOrderChangedCallback: (moveChannelId: Long?, otherChannelId: Long?) -> Unit =
        { _, _ -> }

    fun getWatchNextPrograms(context: Context): List<WatchNextProgram> {
        val cursor = context.contentResolver.query(
            TvContractCompat.WatchNextPrograms.CONTENT_URI,
            WatchNextProgram.PROJECTION,
            null,
            null,
            null
        ) ?: return listOf()

        val watchNextList = mutableListOf<WatchNextProgram>()

        if (!cursor.moveToFirst()) {
            return listOf()
        }

        while (cursor.moveToNext()) {
            try {
                val watchNextProgram = WatchNextProgram.fromCursor(cursor)
                val resolvedActivity =
                    watchNextProgram.intent.resolveActivity(context.packageManager)
                if (resolvedActivity == null ||
                    resolvedActivity.packageName == PACKAGE_FRAMEWORK_STUBS
                ) {
                    // This can't be opened with any app
                    continue
                }
                watchNextList.add(watchNextProgram)
            } catch (e: Exception) {
                Log.w(TAG, "Ignoring watch next: $e")
            }
        }

        cursor.close()
        return watchNextList
    }

    fun getPreviewChannels(context: Context): List<PreviewChannel> {
        val cursor = context.contentResolver.query(
            TvContractCompat.Channels.CONTENT_URI,
            PreviewChannel.Columns.PROJECTION,
            null,
            null,
            null
        ) ?: return listOf()

        val previewChannelList = mutableListOf<PreviewChannel>()

        if (!cursor.moveToFirst()) {
            return listOf()
        }

        while (cursor.moveToNext()) {
            try {
                if (!cursor.getString(PreviewChannel.Columns.COL_APP_LINK_INTENT_URI)
                        .isNullOrEmpty()
                    && !cursor.getString(PreviewChannel.Columns.COL_DISPLAY_NAME).isNullOrEmpty()
                ) {
                    previewChannelList.add(PreviewChannel.fromCursor(cursor))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Ignoring preview channel: $e")
            }
        }

        cursor.close()
        return previewChannelList
    }

    suspend fun getPreviewChannelsAsync(context: Context): List<PreviewChannel> {
        return withContext(Dispatchers.IO) {
            return@withContext getPreviewChannels(context)
        }
    }

    fun getSuggestions(context: Context, id: Long): MutableList<PreviewProgram> {
        val cursor = context.contentResolver.query(
            TvContractCompat.buildPreviewProgramsUriForChannel(id),
            PreviewProgram.PROJECTION,
            null,
            null,
            null,
        ) ?: return mutableListOf()

        val previewProgramList = mutableListOf<PreviewProgram>()

        if (!cursor.moveToFirst()) {
            return mutableListOf()
        }

        while (cursor.moveToNext()) {
            try {
                val previewProgram = PreviewProgram.fromCursor(cursor)
                val resolvedActivity = previewProgram.intent.resolveActivity(context.packageManager)
                if (resolvedActivity == null ||
                    resolvedActivity.packageName == PACKAGE_FRAMEWORK_STUBS
                ) {
                    // This can't be opened with any app
                    continue
                }
                previewProgramList.add(previewProgram)
            } catch (e: Exception) {
                Log.w(TAG, "Ignoring preview program: $e")
            }
        }

        cursor.close()
        return previewProgramList
    }

    suspend fun getSuggestionsAsync(context: Context, id: Long): MutableList<PreviewProgram> {
        return withContext(Dispatchers.IO) {
            return@withContext getSuggestions(context, id)
        }
    }

    fun setHiddenChannels(context: Context, hiddenChannels: MutableList<Long>) {
        val sharedPreferences = context.getSharedPreferences("Channels", Context.MODE_PRIVATE)
        val serializedList = hiddenChannels.joinToString(",")
        sharedPreferences.edit {
            putString("hiddenChannels", serializedList)
        }
    }

    fun getHiddenChannels(context: Context): MutableList<Long> {
        val sharedPreferences =
            context.getSharedPreferences("Channels", Context.MODE_PRIVATE)
        val serializedList = sharedPreferences.getString("hiddenChannels", "") ?: ""
        if (serializedList == "") {
            return mutableListOf()
        }
        return serializedList.split(",").map { it.toLong() }.toMutableList()
    }

    fun hideChannel(context: Context, channelId: Long?) {
        channelId ?: return
        val hiddenChannels = getHiddenChannels(context)
        hiddenChannels.add(channelId)
        setHiddenChannels(context, hiddenChannels)

        // Notify
        onChannelHiddenCallback(channelId)
    }

    fun showChannel(context: Context, channelId: Long?) {
        channelId ?: return
        val hiddenChannels = getHiddenChannels(context)
        hiddenChannels.remove(channelId)
        setHiddenChannels(context, hiddenChannels)

        // Notify
        onChannelShownCallback(channelId)
    }

    fun getChannelTitle(context: Context, previewChannel: PreviewChannel): String {
        return context.resources.getString(
            R.string.channel_title, previewChannel.getAppName(context), previewChannel.displayName
        )
    }

    fun saveChannelOrder(
        context: Context,
        from: Int,
        to: Int,
        channels: List<Long>,
        notify: Boolean,
    ) {
        val sharedPreferences = context.getSharedPreferences("Channels", Context.MODE_PRIVATE)
        val serializedList = channels.joinToString(",")
        sharedPreferences.edit {
            putString("channels", serializedList)
        }

        if (!notify) {
            return
        }

        // Notify
        onChannelOrderChangedCallback(channels[from], channels[to])
    }

    fun getChannelOrder(context: Context): MutableList<Long> {
        val sharedPreferences =
            context.getSharedPreferences("Channels", Context.MODE_PRIVATE)
        val serializedList = sharedPreferences.getString("channels", "") ?: ""
        if (serializedList == "") {
            return mutableListOf()
        }

        return serializedList.split(",").map { it.toLong() }.toMutableList()
    }

    fun <T, K> List<T>.orderSuggestions(orderIds: List<K>, idSelector: (T) -> K?): List<T> {
        if (orderIds.isEmpty()) {
            val (presentItems, remainingItems) = this.partition { idSelector(it) == Channel.ALL_APPS_ID }
            return remainingItems + presentItems
        }

        val (presentItems, remainingItems) = this.partition { idSelector(it) in orderIds }
        val sortedPresentItems = presentItems.sortedBy { orderIds.indexOf(idSelector(it)) }
        return sortedPresentItems + remainingItems
    }

    fun aspectRatioToFloat(aspectRatio: Int): Float {
        return when (aspectRatio) {
            TvContract.PreviewPrograms.ASPECT_RATIO_16_9 -> 16f / 9f
            TvContract.PreviewPrograms.ASPECT_RATIO_4_3 -> 4f / 3f
            TvContract.PreviewPrograms.ASPECT_RATIO_1_1 -> 1f
            TvContract.PreviewPrograms.ASPECT_RATIO_3_2 -> 3f / 2f
            TvContract.PreviewPrograms.ASPECT_RATIO_2_3 -> 2f / 3f
            else -> -1f
        }
    }

    fun PreviewChannel.getAppName(context: Context): String {
        val packageManager: PackageManager = context.packageManager
        return try {
            val applicationInfo = packageManager.getApplicationInfo(this.packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }
}
