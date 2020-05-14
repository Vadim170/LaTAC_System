package com.kontakt1.tmonitor.ui.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.text.TextUtils.TruncateAt
import android.util.AttributeSet
import android.util.SparseArray
import android.util.SparseIntArray
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import com.kontakt1.tmonitor.R
import java.util.concurrent.atomic.AtomicInteger

class SwitchButton : RadioGroup, RadioGroup.OnCheckedChangeListener {
    private val CHECKED_STATE = intArrayOf(android.R.attr.state_checked)
    private val UNCHECKED_STATE = intArrayOf(-android.R.attr.state_checked)
    private val ENABLED_STATE = intArrayOf(android.R.attr.state_enabled)
    private val DISABLED_STATE = intArrayOf(-android.R.attr.state_enabled)
    private var mTextColor: ColorStateList? = null
    private var mParentWidth = 0
    private var mParentHeight = 0
    private var mRadioStyle = 0
    private var cornerRadius = 0f
    private var textSize = 0f
    private var checkedColor = 0
    private var unCheckedColor = 0
    private var disabledColor = 0
    private var strokeColor = 0
    private var strokeWidth = 0
    private var mTexts: Array<CharSequence>? = null
    private var switchCount = 0
    private var isMeasure = false
    private var mRadioArrays: SparseArray<RadioButton>? = null
    private var mButtonDrawables: SparseArray<Drawable>? = null
    private var mStateDrawables: SparseArray<StateListDrawable>? = null
    private var mSparseIds: SparseIntArray? = null
    private var mCurrentPosition = 0
    private var changeListener: OnChangeListener? = null

    constructor(context: Context?) : super(context, null) {}

    @SuppressLint("ResourceType")
    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        var a = context.obtainStyledAttributes(
            attrs,
            intArrayOf(android.R.attr.orientation, android.R.attr.layout_height)
        )
        orientation = a.getInt(0, LinearLayout.HORIZONTAL)
        mParentHeight = a.getDimensionPixelSize(1, 0)
        a.recycle()
        a = context.obtainStyledAttributes(attrs, R.styleable.SwitchButton)
        setTextColor(a.getColorStateList(R.styleable.SwitchButton_android_textColor))
        setTextArray(a.getTextArray(R.styleable.SwitchButton_sw_textArray))
        setSwitchCount(
            a.getInteger(
                R.styleable.SwitchButton_sw_switchCount,
                DEFAULT_SWITCH_COUNT
            )
        )
        setSwitchStyle(a.getResourceId(R.styleable.SwitchButton_sw_ThemeStyle, 0))
        setCornerRadius(
            a.getDimension(
                R.styleable.SwitchButton_sw_CornerRadius,
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    5f,
                    resources.displayMetrics
                )
            )
        )
        setCheckedColor(
            a.getColor(
                R.styleable.SwitchButton_sw_checkedColor,
                Color.GREEN
            )
        )
        setUnCheckedColor(
            a.getColor(
                R.styleable.SwitchButton_sw_unCheckedColor,
                Color.WHITE
            )
        )
        setDisabledColor(
            a.getColor(
                R.styleable.SwitchButton_sw_disabledColor,
                Color.GRAY
            )
        )
        setStrokeColor(
            a.getColor(
                R.styleable.SwitchButton_sw_strokeColor,
                Color.BLACK
            )
        )
        setStrokeWidth(
            a.getDimension(
                R.styleable.SwitchButton_sw_strokeWidth,
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    1f,
                    resources.displayMetrics
                )
            ).toInt()
        )
        setTextSize(a.getDimension(R.styleable.SwitchButton_android_textSize, 0f))
        a.recycle()
        setOnCheckedChangeListener(this)
    }

    @SuppressLint("NewApi", "ResourceType")
    private fun initUI(context: Context) {
        require(!(mTexts != null && mTexts!!.size != switchCount)) { "The textArray's length must equal to the switchCount" }
        val colorDrawable = ColorDrawable()
        val mParams = LayoutParams(
            mParentWidth / if (switchCount > 2) switchCount else switchCount + 1,
            mParentHeight,
            1F
        )
        val mFirstParams = LayoutParams(
            mParentWidth / if (switchCount > 2) switchCount else switchCount + 1,
            mParentHeight,
            1F
        )
        for (i in 0 until switchCount) {
            if (mRadioArrays == null) mRadioArrays = SparseArray()
            val mRadioButton = mRadioArrays!![i, createRadioView()]
            mParams.leftMargin = if (i > 0) -strokeWidth else 0
            mRadioButton.layoutParams = if (i == 0) mFirstParams else mParams
            mRadioButton.buttonDrawable =
                if (mButtonDrawables != null) mButtonDrawables!![i, colorDrawable] else colorDrawable
            if (Build.VERSION.SDK_INT >= 16) {
                mRadioButton.background = getStateDrawable(i)
            } else {
                mRadioButton.setBackgroundDrawable(getStateDrawable(i))
            }
            mRadioButton.text = mTexts!![i]
            if (mRadioButton.id < 0) {
                val id = viewId
                if (mSparseIds == null) mSparseIds = SparseIntArray()
                mSparseIds!!.put(i, id)
                mRadioButton.id = id
            } else {
                removeView(mRadioButton)
            }
            mRadioButton.isChecked = mCurrentPosition == i
            addView(mRadioButton, i)
            mRadioArrays!!.put(i, mRadioButton)
        }
    }

    private fun getStateDrawable(i: Int): Drawable {
        if (mStateDrawables == null) mStateDrawables =
            SparseArray()
        var mStateListDrawable =
            if (mStateDrawables!!.size() >= i + 1 && (i != switchCount - 1 || i == switchCount - 1)) null else mStateDrawables!![i]
        if (mStateListDrawable == null) {
            val leftRadius: Float = if (i == 0) cornerRadius else 0F
            val rightRadius: Float =
                when (i) {
                    0 -> 0F
                    switchCount - 1 -> cornerRadius
                    else -> 0F
                }
            val cRadius = floatArrayOf(
                leftRadius,
                leftRadius,
                rightRadius,
                rightRadius,
                rightRadius,
                rightRadius,
                leftRadius,
                leftRadius
            )
            mStateListDrawable = StateListDrawable()
            var cornerDrawable = GradientDrawable()
            cornerDrawable.setColor(checkedColor)
            //cornerDrawable.setStroke(strokeWidth, unCheckedColor);
            cornerDrawable.cornerRadii = cRadius
            mStateListDrawable.addState(CHECKED_STATE, cornerDrawable)
            cornerDrawable = GradientDrawable()
            cornerDrawable.setColor(disabledColor)
            cornerDrawable.cornerRadii = cRadius
            mStateListDrawable.addState(DISABLED_STATE, cornerDrawable)
            cornerDrawable = GradientDrawable()
            cornerDrawable.setStroke(strokeWidth, strokeColor)
            cornerDrawable.setColor(unCheckedColor)
            cornerDrawable.cornerRadii = cRadius
            mStateListDrawable.addState(UNCHECKED_STATE, cornerDrawable)
            mStateDrawables!!.put(i, mStateListDrawable)
        }
        return mStateListDrawable
    }

    private fun createRadioView(): RadioButton {
        val mRadioButton = RadioButton(
            context,
            null,
            if (mRadioStyle > 0) mRadioStyle else android.R.attr.radioButtonStyle
        )
        if (mRadioStyle == 0) {
            mRadioButton.gravity = Gravity.CENTER
            mRadioButton.ellipsize = TruncateAt.END
        }
        if (mTextColor != null) mRadioButton.setTextColor(mTextColor)
        if (textSize > 0) mRadioButton.textSize = textSize
        return mRadioButton
    }

    private fun initialize() {
        removeAllViews()
        switchCount = if (mTexts != null) mTexts!!.size else switchCount
        initUI(context)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!isMeasure) {
            initUI(context)
            isMeasure = !isMeasure
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mParentWidth = widthMeasureSpec - EX
        mParentHeight = if (mParentHeight == 0) heightMeasureSpec else mParentHeight
    }

    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        if (changeListener != null) changeListener!!.onChange(mSparseIds!!.indexOfValue(checkedId))
    }

    fun setCurrentPosition(selectedPosition: Int) {
        if (selectedPosition in 0..switchCount) {
            mCurrentPosition = selectedPosition
        }
        initialize()
    }

    fun setCheckedPosition(selectedPosition: Int) {
        if (selectedPosition in 0..switchCount) {
            mCurrentPosition = selectedPosition
            if (mSparseIds != null) check(mSparseIds!![mSparseIds!!.keyAt(selectedPosition)])
        }
    }

    fun setTextColor(mTextColor: ColorStateList?) {
        this.mTextColor = mTextColor
    }

    fun setSwitchStyle(mSwitchStyle: Int) {
        mRadioStyle = mSwitchStyle
    }

    fun setTextArray(mTexts: Array<CharSequence>?) {
        this.mTexts = mTexts
    }

    fun getSwitchCount(): Int {
        return switchCount
    }

    fun setParentWidth(mParentWidth: Int) {
        this.mParentWidth = mParentWidth
    }

    fun setParentHeight(mParentHeight: Int) {
        this.mParentHeight = mParentHeight
    }

    fun setSwitchCount(switchCount: Int) {
        this.switchCount =
            if (switchCount < 2) DEFAULT_SWITCH_COUNT else switchCount
        if (mButtonDrawables == null) mButtonDrawables = SparseArray()
    }

    fun setCornerRadius(cornerRadius: Float) {
        this.cornerRadius = cornerRadius
    }

    fun setCheckedColor(checkedColor: Int) {
        this.checkedColor = checkedColor
    }

    fun setUnCheckedColor(unCheckedColor: Int) {
        this.unCheckedColor = unCheckedColor
    }

    fun setDisabledColor(disabledColor: Int) {
        this.disabledColor = disabledColor
    }

    fun setStrokeColor(strokeColor: Int) {
        this.strokeColor = strokeColor
    }

    fun setStrokeWidth(strokeWidth: Int) {
        this.strokeWidth = strokeWidth
    }

    fun setTextSize(textSize: Float) {
        this.textSize = textSize
    }

    fun setSwitchButton(position: Int, mDrawableResId: Int) {
        mButtonDrawables!!.put(position, resources.getDrawable(mDrawableResId))
    }

    fun setOnChangeListener(eventListener: OnChangeListener?) {
        changeListener = eventListener
    }

    fun setEnabledButton(i: Int, enabled: Boolean) {
        if (mRadioArrays != null) {
            val mRadioButton = mRadioArrays!![i]
            if (mRadioButton != null) mRadioButton.isEnabled = enabled
        }
    }

    fun setEnabledButtons(enabled: Array<Boolean>) {
        enabled.forEachIndexed { index, enabled ->
            setEnabledButton(index, enabled)
        }
    }

    interface OnChangeListener {
        fun onChange(position: Int)
    }

    val viewId: Int
        get() {
            while (true) {
                val result = sNextGeneratedId.get()
                var newValue = result + 1
                if (newValue > 0x00FFFFFF) newValue = 1
                if (sNextGeneratedId.compareAndSet(result, newValue))
                    return result
            }
        }

    companion object {
        private const val DEFAULT_SWITCH_COUNT = 2
        private const val EX = 5
        private val sNextGeneratedId = AtomicInteger(1)
    }
}