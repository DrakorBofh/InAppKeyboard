package com.fristano.inappkeyboard

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.ColorStateListDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.Log
import android.util.StateSet
import android.util.Xml
import androidx.annotation.StyleRes
import com.fristano.inappkeyboard.InAppKeyboard.Row.Companion.KEY_ALIGNMENT_CENTER
import java.util.*


/**
 * Loads an XML description of a InAppKeyboard and stores the attributes of the keys. A InAppKeyboard
 * consists of rows of keys.
 * <p>The layout file for a InAppKeyboard contains XML that looks like the following snippet:</p>
 * <pre>
 * &lt;InAppKeyboard
 *         android:keyWidth="%10p"
 *         android:keyHeight="50px"
 *         android:horizontalGap="2px"
 *         android:verticalGap="2px" &gt;
 *     &lt;Row android:keyWidth="32px" &gt;
 *         &lt;Key android:keyLabel="A" /&gt;
 *         ...
 *     &lt;/Row&gt;
 *     ...
 * &lt;/InAppKeyboard&gt;
 * </pre>
 *
 * @attr ref android.R.styleable#Keyboard_keyWidth
 * @attr ref android.R.styleable#Keyboard_keyHeight
 * @attr ref android.R.styleable#Keyboard_horizontalGap
 * @attr ref android.R.styleable#Keyboard_verticalGap
 */
open class InAppKeyboard {

    companion object {
        const val TAG = "InAppKeyboard"

        // InAppKeyboard XML Tags
        private const val XML_TAG_KEYBOARD = "InAppKeyboard"
        private const val XML_TAG_ROW = "Row"
        private const val XML_TAG_KEY = "Key"

        const val DEFAULT_KEYBOARD_MODE = 0

    }

    open var listener: InAppKeyboardActionListener? = null

    /**
     * Possible appearance override for the keys of this keyboard
     */
    protected var krdProperties: KeyboardProperties? = null
    val keyboardProperties: KeyboardProperties
        get() = krdProperties ?: KeyboardProperties.defProp

    var rowModeMap = HashMap<Int, ArrayList<Row>>()
        private set


    /**
     * Keyboard mode, or zero, if none.
     */
    private var mKeyboardMode = DEFAULT_KEYBOARD_MODE

    private val rows = ArrayList<Row>()


    val currentMode: Int
        get() = mKeyboardMode

    fun changeMode(modeId: Int) {
        mKeyboardMode = modeId
    }

    //region ___ Constructors ___

    /**
     * Creates a keyboard from the given xml key layout file.
     *
     * @param context        the application or service context
     * @param xmlLayoutResId the resource file that contains the keyboard layout and keys.
     */
    constructor(context: Context, xmlLayoutResId: Int) {
        initialConstruction(context, xmlLayoutResId)
    }

    /**
     * Creates a keyboard from the given xml key layout file. Weeds out rows
     * that have a keyboard mode defined but don't match the specified mode.
     *
     * @param context        the application or service context
     * @param xmlLayoutResId the resource file that contains the keyboard layout and keys.
     * @param modeId         keyboard mode identifier
     */
    constructor(context: Context, xmlLayoutResId: Int, modeId: Int) {
        initialConstruction(context, xmlLayoutResId, modeId)
    }


    /**
     * Creates a keyboard from the given xml key layout file. Weeds out rows
     * that have a keyboard mode defined but don't match the specified mode.
     *
     * @param context        the application or service context
     * @param xmlLayoutResId the resource file that contains the keyboard layout and keys.
     * @param modeId         keyboard mode identifier
     */
    private fun initialConstruction(
        context: Context, xmlLayoutResId: Int, modeId: Int = 0
    ) {
        mKeyboardMode = modeId
        loadKeyboard(context, context.resources.getXml(xmlLayoutResId))
    }
    //endregion ___ constructors ___


    private fun loadKeyboard(context: Context, parser: XmlResourceParser) {
        var inKey = false
        var inRow = false
        rowModeMap.clear()

        var currentRow: Row? = null
        val res = context.resources

        try {
            var event: Int
            while (parser.next().also { event = it } != XmlResourceParser.END_DOCUMENT) {
                if (event == XmlResourceParser.START_TAG) {
                    when (parser.name) {
                        XML_TAG_KEYBOARD -> {
                            parseKeyboardAttributes(res, parser)
                        }
                        XML_TAG_ROW -> {
                            inRow = true
                            currentRow = Row(res, this, parser)
                            if (rowModeMap[currentRow.mode] == null)
                                rowModeMap[currentRow.mode] = ArrayList<Row>()
                            rowModeMap[currentRow.mode]!!.add(currentRow)
                            rows.add(currentRow)

                        }
                        XML_TAG_KEY -> {
                            currentRow?.let { valid_row ->
                                inKey = true
                                Key(valid_row, res, parser).let { key ->
                                    if (key.isValidConfiguration)
                                        valid_row.keys.add(key)
                                }
                            }
                        }
                    }

                } else if (event == XmlResourceParser.END_TAG) {
                    if (inKey) {
                        inKey = false
                    } else if (inRow) {
                        inRow = false
                    } else {
                        // TODO: error or extend?
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error:$e")
            e.printStackTrace()
        }
    }

    fun processKeyPressed(key: Key) {
        if (key.isAction && key.code != null) {
            listener?.onCodePressed(key.code!!)
        } else if (!key.text.isNullOrEmpty()) {
            listener?.onTextPressed(key.text.toString())
        } else if (!key.label.isNullOrBlank()) {
            listener?.onTextPressed(key.label!!)
        } else if (key.icon != null && key.code != null) {
            listener?.onCodePressed(key.code!!)
        }
        //TODO complete the rest of cases

    }

    /**
     * Parse keyboard level attributes, with global default values
     *
     * @attr ref android.R.styleable#InAppKeyboard_keyWidth
     * @attr ref android.R.styleable#InAppKeyboard_keyHeight
     * @attr ref android.R.styleable#InAppKeyboard_keyHorizontalGap
     * @attr ref android.R.styleable#InAppKeyboard_keyVerticalGap
     *
     */
    private fun parseKeyboardAttributes(res: Resources, parser: XmlResourceParser) {
        val a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.InAppKeyboard)

        krdProperties =
            KeyboardProperties.createFromStyleableAttr(res, Xml.asAttributeSet(parser), null)

        a.recycle()
    }


    //region ___ inner Classes ___

    //region ==== KeyboardProperties ====
    class KeyboardProperties(
        private val _keyWidth: Int? = null,
        private val _keyHeight: Int? = null,
        private val _keyHorGap: Int? = null,
        private val _keyVerGap: Int? = null,
        private val _keyBackground: ColorStateList? = null,
        private val _keyTextColor: ColorStateList? = null,
        private val _keyTextSize: Float? = null,
        private var _parent: (() -> (KeyboardProperties))? = null
    ) {

        val keyWidth: Int
            get() = _keyWidth ?: _parent?.invoke()?.keyWidth ?: defProp.keyWidth
        val keyHeight: Int
            get() = _keyHeight ?: _parent?.invoke()?.keyHeight ?: defProp.keyHeight
        val keyHorizontalGap: Int
            get() = _keyHorGap ?: _parent?.invoke()?.keyHorizontalGap ?: defProp.keyHorizontalGap
        val keyVerticalGap: Int
            get() = _keyVerGap ?: _parent?.invoke()?.keyVerticalGap ?: defProp.keyVerticalGap
        val keyBackground: ColorStateList
            get() = _keyBackground ?: _parent?.invoke()?.keyBackground ?: defProp.keyBackground
        val keyTextColor: ColorStateList
            get() = _keyTextColor ?: _parent?.invoke()?.keyTextColor ?: defProp.keyTextColor
        val keyTextSize: Float
            get() = _keyTextSize ?: _parent?.invoke()?.keyTextSize ?: defProp.keyTextSize

        fun setNewParent(keyboardProperties: (() -> (KeyboardProperties))) {
            _parent = keyboardProperties
        }
        companion object {

            val defProp = KeyboardProperties(
                50,
                50,
                0,
                0,
                ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()), intArrayOf(
                        Color.WHITE,
                        Color.BLACK
                    )
                ),
                ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()), intArrayOf(
                        Color.BLACK,
                        Color.WHITE
                    )
                ),
                7f
            )

            private fun makeDefaultBgSelector(): StateListDrawable {
                val res = StateListDrawable()
                res.addState(intArrayOf(android.R.attr.state_focused), ColorDrawable(Color.WHITE))
                res.addState(intArrayOf(-android.R.attr.state_focused), ColorDrawable(Color.BLACK))
                return res
            }

            fun createFromTypedArray(
                typedArray: TypedArray,
                parentKrd: (() -> (KeyboardProperties))?
            ): KeyboardProperties {

                val keyWidth =
                    typedArray.getDimensionPixelSize(R.styleable.InAppKeyboardView_keyWidth, -1)
                val keyHeight =
                    typedArray.getDimensionPixelSize(R.styleable.InAppKeyboardView_keyHeight, -1)

                val keyHorizontalGap =
                    typedArray.getDimensionPixelSize(
                        R.styleable.InAppKeyboardView_keyHorizontalGap,
                        -1
                    )
                val keyVerticalGap =
                    typedArray.getDimensionPixelSize(
                        R.styleable.InAppKeyboardView_keyVerticalGap,
                        -1
                    )

                val keyBackground =
                    typedArray.getColorStateList(R.styleable.InAppKeyboardView_keyBackground)

                val keyTextColor =
                    typedArray.getColorStateList(R.styleable.InAppKeyboardView_keyTextColor)
                val keyTextSize =
                    typedArray.getDimension(R.styleable.InAppKeyboardView_keyTextSize, -1f)

                typedArray.recycle()

                return KeyboardProperties(
                    if (keyWidth == -1) null else keyWidth,
                    if (keyHeight == -1) null else keyHeight,
                    if (keyHorizontalGap == -1) null else keyHorizontalGap,
                    if (keyVerticalGap == -1) null else keyVerticalGap,
                    keyBackground,
                    keyTextColor,
                    if (keyTextSize == -1f) null else keyTextSize,
                    parentKrd
                )
            }

            fun createFromStyleableAttr(
                res: Resources,
                attrs: AttributeSet,
                parentKrd: (() -> (KeyboardProperties))?
            ): KeyboardProperties {
                return createFromTypedArray(
                    res.obtainAttributes(
                        attrs,
                        R.styleable.InAppKeyboardView
                    ), parentKrd
                )
            }
        }
    }
    //endregion

    //region ==== Row ====
    /**
     * Container for keys in the keyboard. All keys in a row are at the same Y-coordinate.
     * Some of the key size defaults can be overridden per row from what the [InAppKeyboard]
     * defines.
     *
     * @attr ref android.R.styleable#InAppKeyboard_Row_keyAlignment
     * @attr ref android.R.styleable#InAppKeyboard_Row_keyboardMode
     * @attr ref android.R.styleable#InAppKeyboard_Row_marginTop
     * @attr ref android.R.styleable#InAppKeyboard_Row_marginLeft
     * @attr ref android.R.styleable#InAppKeyboard_Row_marginRight
     * @attr ref android.R.styleable#InAppKeyboard_Row_marginBottom
     */
    open class Row {
        companion object {
            const val KEY_ALIGNMENT_CENTER = 0
            const val KEY_ALIGNMENT_LEFT = 1
            const val KEY_ALIGNMENT_RIGHT = 2
        }

        /**
         * Possible appearance override for this key
         */
        protected var krdProperties: KeyboardProperties? = null
        val keyboardProperties: KeyboardProperties
            get() = krdProperties ?: parentKeyboard.keyboardProperties

        fun setDefaultKeyboardProperties(keyboardProperties: KeyboardProperties) {
            krdProperties = keyboardProperties
        }

        /**
         * Edge flags for this row of key alignment. Possible values that can be assigned are
         * [Centered][KEY_ALIGNMENT_CENTER], [Left][KEY_ALIGNMENT_LEFT] or [right][KEY_ALIGNMENT_RIGHT]
         */
        var keyAlignment = 0

        /**
         * The keyboard mode for this row
         */
        var mode = 0

        protected var parentKeyboard: InAppKeyboard


        var marginTop: Int = 0
        var marginLeft: Int = 0
        var marginRight: Int = 0
        var marginBottom: Int = 0

        var keys = ArrayList<Key>()

        constructor(parentKeyboard: InAppKeyboard) {
            this.parentKeyboard = parentKeyboard
        }

        constructor(res: Resources, parentKeyboard: InAppKeyboard, parser: XmlResourceParser?) {
            this.parentKeyboard = parentKeyboard

            krdProperties =
                KeyboardProperties.createFromStyleableAttr(res, Xml.asAttributeSet(parser)) {
                    parentKeyboard.keyboardProperties
                }

            val a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.InAppKeyboard_Row)

            keyAlignment =
                a.getInt(R.styleable.InAppKeyboard_Row_keyAlignment, KEY_ALIGNMENT_CENTER)
            mode =
                a.getInt(R.styleable.InAppKeyboard_keyboardMode, DEFAULT_KEYBOARD_MODE)

            marginTop =
                a.getInt(R.styleable.InAppKeyboard_Row_marginTop, 0)
            marginLeft =
                a.getInt(R.styleable.InAppKeyboard_Row_marginLeft, 0)
            marginRight =
                a.getInt(R.styleable.InAppKeyboard_Row_marginRight, 0)
            marginBottom =
                a.getInt(R.styleable.InAppKeyboard_Row_marginBottom, 0)

            a.recycle()

        }
    }
    //endregion ==== Row ====

    //region ==== Key ====
    /**
     * Class for describing the position and characteristics of a single key in the keyboard.
     *
     * @see [https://developer.android.com/reference/android/view/KeyEvent]
     *
     * @attr ref android.R.styleable#InAppKeyboard_Key_keyLabel
     * @attr ref android.R.styleable#InAppKeyboard_Key_code
     * @attr ref android.R.styleable#InAppKeyboard_Key_keyIcon
     * @attr ref android.R.styleable#InAppKeyboard_Key_keyOutputText
     * @attr ref android.R.styleable#InAppKeyboard_Key_keyboardMode
     * @attr ref android.R.styleable#InAppKeyboard_Key_isSticky
     *
     */
    open class Key {

        companion object {
            const val INT_NULL = -99999
        }

        /**
         * Parent row
         */
        protected var parentRow: Row

        /**
         * Possible appearance override for this key
         */
        protected var krdProperties: KeyboardProperties? = null
        val keyboardProperties: KeyboardProperties
            get() = krdProperties ?: parentRow.keyboardProperties


        /**
         * Label to display
         */
        var label: String? = null

        /**
         * KeyEvent code that this key could generate
         */
        var code: Int? = null

        /**
         * Icon to display instead of a label. Icon takes precedence over a label
         */
        var icon: Drawable? = null

        /**
         * Text to output when pressed. This can be multiple characters, like ".com"
         * If this is empty, the code will be sent instead
         */
        var text: CharSequence? = null

        /**
         * If this value is not null, this key will be a toggle key that will be indicate the
         * current keyboardMode status showed like pressed and will change the mode otherwise
         */
        var keyboardMode: Int? = null

        /**
         * Whether this key is sticky, i.e., a toggle key
         */
        var isSticky = false

        /**
         * If this is a sticky key, is it on?
         */
        var isStickyActive = false

        var isAction = false

        /**
         * The amount of key size that will occupy horizontally
         */
        var hSizeMultiply = 1


        /**
         * Return true if the current configuration is valid.
         * The valid cases are:
         *      - KeyLabel alone: The label will be the press result
         *      - KeyLabel && keyOutputText: The keyOutputText will be the press result
         *      - Code && (KeyLabel | Icon): The code will be the result of press
         *      - KeyboardMode && (KeyLabel | Icon): Will be stickyPressed on current keyboardMode
         *      - Code && isSticky : Will be a sticky that will inform toggle status with code
         */
        val isValidConfiguration: Boolean
            get() {
                return ((!label.isNullOrBlank()) ||
                        (code != null && (isSticky || icon != null)) ||
                        (keyboardMode != null && icon != null) ||
                        (code != null && isAction)
                        )
            }


        constructor(parentRow: Row, res: Resources, parser: XmlResourceParser?) {
            this.parentRow = parentRow

            krdProperties =
                KeyboardProperties.createFromStyleableAttr(res, Xml.asAttributeSet(parser)) {
                    parentRow.keyboardProperties
                }

            val a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.InAppKeyboard_Key)

            label = a.getString(R.styleable.InAppKeyboard_Key_keyLabel)
            code = a.getInt(R.styleable.InAppKeyboard_Key_code, INT_NULL)
            if (code == INT_NULL) code = null

            icon = a.getDrawable(R.styleable.InAppKeyboard_Key_icon)
            text = a.getString(R.styleable.InAppKeyboard_Key_keyOutputText)

            keyboardMode = a.getInt(R.styleable.InAppKeyboard_keyboardMode, INT_NULL)
            if (keyboardMode == INT_NULL) keyboardMode = null

            isSticky = a.getBoolean(R.styleable.InAppKeyboard_Key_isSticky, false)
            isAction = a.getBoolean(R.styleable.InAppKeyboard_Key_isAction, false)

            hSizeMultiply = a.getInt(R.styleable.InAppKeyboard_Key_hSizeMultiply, 1)

            a.recycle()

        }
    }

    //endregion ==== Key ====


    interface InAppKeyboardActionListener {
        fun onTextPressed(string: String) {}
        fun onCodePressed(code: Int) {}
    }

    //endregion ___ inner Classes ___
}