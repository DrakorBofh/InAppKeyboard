package com.fristano.inappkeyboard

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.setPadding


class InAppKeyboardView : LinearLayout {

    var keyboard: InAppKeyboard? = null
        set(value) {
            field = value
            updateView()
        }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        buildInAppKeyboardView(attrs, 0, R.style.InAppKeyboard_KeyboardView)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        buildInAppKeyboardView(attrs, defStyleAttr, R.style.InAppKeyboard_KeyboardView)
    }


    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        buildInAppKeyboardView(attrs, defStyleAttr, defStyleRes)
    }


    private fun buildInAppKeyboardView(
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int?
    ) {

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.InAppKeyboardView,
            defStyleAttr,
            defStyleRes ?: R.style.InAppKeyboard_KeyboardView
        ).apply {

            keyboard?.keyboardProperties?.setNewParent {
                InAppKeyboard.KeyboardProperties.createFromTypedArray(this, null)
            }

            ViewCompat.saveAttributeDataForStyleable(
                this@InAppKeyboardView,
                context,
                R.styleable.InAppKeyboardView,
                attrs,
                this,
                defStyleAttr,
                defStyleRes ?: R.style.InAppKeyboard_KeyboardView
            )
        }

        this.orientation = VERTICAL
        this.layoutParams =  LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        this.descendantFocusability = FOCUS_AFTER_DESCENDANTS
        updateView()
    }

    private fun updateView() {
        keyboard?.let {
            renderKeyboard(it)
        }
        invalidate()
        requestLayout()
    }


    private fun renderKeyboard(kbrd: InAppKeyboard) {
        kbrd.rowModeMap[kbrd.currentMode]?.forEach { row ->
            this.addView(createRow(row))
        }
    }

    private fun createRow(row: InAppKeyboard.Row): LinearLayout? {
        if (row.keys.isEmpty()) return null

        val result = LinearLayout(context)
        result.descendantFocusability = FOCUS_AFTER_DESCENDANTS
        result.isFocusable = false


        result.orientation = HORIZONTAL
        val rowParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        rowParams.gravity = Gravity.LEFT

        result.layoutParams = rowParams
        row.keys.forEach { key ->
            val view = createKey(key )
            view.isFocusable = true

            view.setOnClickListener {
                keyboard?.processKeyPressed(key)
            }

            val params = LayoutParams(
                key.hSizeMultiply * (key.keyboardProperties.keyWidth),
                key.keyboardProperties.keyHeight,
            )

            params.gravity = Gravity.CENTER
            params.setMargins(
                key.keyboardProperties.keyHorizontalGap,
                key.keyboardProperties.keyVerticalGap,
                key.keyboardProperties.keyHorizontalGap,
                key.keyboardProperties.keyVerticalGap
            )
            view.layoutParams = params

            result.addView(view)
        }

        return result
    }


    private fun createKey(key: InAppKeyboard.Key): View {

        return if (key.icon != null) {
            val image = ImageButton(context)
            image.adjustViewBounds = true
            image.setImageDrawable(key.icon)
            image.imageTintList = key.keyboardProperties.keyTextColor
            image.scaleType = ImageView.ScaleType.FIT_CENTER
            image.backgroundTintList = key.keyboardProperties.keyBackground
            image
        } else {

            val textButton = Button(context, null, R.attr.borderlessButtonStyle, R.style.Widget_AppCompat_Button_Borderless)

            textButton.text = key.label
            textButton.setTextColor(key.keyboardProperties.keyTextColor)
            textButton.isAllCaps = false
            textButton.textSize = key.keyboardProperties.keyTextSize
            textButton.setPadding(0)
            textButton.setBackgroundColor(Color.WHITE)
            textButton.backgroundTintList = key.keyboardProperties.keyBackground

            textButton
        }
    }


}