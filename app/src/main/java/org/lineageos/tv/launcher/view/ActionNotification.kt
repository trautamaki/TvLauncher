package org.lineageos.tv.launcher.view

import android.animation.AnimatorInflater
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import org.lineageos.tv.launcher.R


class ActionNotification : LinearLayout, View.OnClickListener {
    private val mContext: Context
    private val mTextView: TextView by lazy { findViewById(R.id.text) }
    private val mImageView: ImageView by lazy { findViewById(R.id.image) }
    private var mIntent: Intent? = null

    constructor(context: Context) : super(context) {
        mContext = context
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setup(attrs)
        mContext = context
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setup(attrs)
        mContext = context
    }

    fun setText(string: String) {
        mTextView.text = string
    }

    fun setImageDrawable(drawable: Drawable?) {
        mImageView.setImageDrawable(drawable)
    }

    fun setAction(intent: Intent) {
        mIntent = intent
    }

    private fun setup(attrs: AttributeSet) {
        val text = attrs.getAttributeResourceValue(
            "http://schemas.android.com/apk/res/android",
            "text", R.string.empty
        )
        val drawableResource =
            attrs.getAttributeResourceValue(
                "http://schemas.android.com/apk/res/android", "src",
                R.drawable.ic_dead
            )
        mTextView.text = context.getString(text)
        mImageView.setImageDrawable(AppCompatResources.getDrawable(context, drawableResource))
    }

    init {
        inflate(context, R.layout.large_image_button, this)
        isFocusable = true
        isClickable = true
        stateListAnimator =
            AnimatorInflater.loadStateListAnimator(context, R.anim.action_notification_animator)
        setOnClickListener(this)
    }

    override fun onClick(view: View) {
        if (mIntent != null) {
            mContext.startActivity(mIntent)
        }
    }
}
