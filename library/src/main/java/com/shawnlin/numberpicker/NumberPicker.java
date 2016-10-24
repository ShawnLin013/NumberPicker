package com.shawnlin.numberpicker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.StringRes;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.LayoutInflater.Filter;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A widget that enables the user to select a number form a predefined range.
 */
public class NumberPicker extends LinearLayout {

    /**
     * The default update interval during long press.
     */
    private static final long DEFAULT_LONG_PRESS_UPDATE_INTERVAL = 300;

    /**
     * The coefficient by which to adjust (divide) the max fling velocity.
     */
    private static final int SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT = 8;

    /**
     * The the duration for adjusting the selector wheel.
     */
    private static final int SELECTOR_ADJUSTMENT_DURATION_MILLIS = 800;

    /**
     * The duration of scrolling while snapping to a given position.
     */
    private static final int SNAP_SCROLL_DURATION = 300;

    /**
     * The strength of fading in the top and bottom while drawing the selector.
     */
    private static final float FADING_EDGE_STRENGTH = 0.9f;

    /**
     * The default unscaled height of the selection divider.
     */
    private static final int UNSCALED_DEFAULT_SELECTION_DIVIDER_THICKNESS = 2;

    /**
     * The default unscaled distance between the selection dividers.
     */
    private static final int UNSCALED_DEFAULT_SELECTION_DIVIDERS_DISTANCE = 48;

    /**
     * Constant for unspecified size.
     */
    private static final int SIZE_UNSPECIFIED = -1;

    /**
     * The default max value of this widget.
     */
    private static final int DEFAULT_MAX_VALUE = 100;

    /**
     * The default min value of this widget.
     */
    private static final int DEFAULT_MIN_VALUE = 1;

    /**
     * The default max height of this widget.
     */
    private static final int DEFAULT_MAX_HEIGHT = 180;

    /**
     * The default min width of this widget.
     */
    private static final int DEFAULT_MIN_WIDTH = 64;

    /**
     * Use a custom NumberPicker formatting callback to use two-digit minutes
     * strings like "01". Keeping a static formatter etc. is the most efficient
     * way to do this; it avoids creating temporary objects on every call to
     * format().
     */
    private static class TwoDigitFormatter implements Formatter {
        final StringBuilder mBuilder = new StringBuilder();

        char mZeroDigit;
        java.util.Formatter mFmt;

        final Object[] mArgs = new Object[1];

        TwoDigitFormatter() {
            final Locale locale = Locale.getDefault();
            init(locale);
        }

        private void init(Locale locale) {
            mFmt = createFormatter(locale);
            mZeroDigit = getZeroDigit(locale);
        }

        public String format(int value) {
            final Locale currentLocale = Locale.getDefault();
            if (mZeroDigit != getZeroDigit(currentLocale)) {
                init(currentLocale);
            }
            mArgs[0] = value;
            mBuilder.delete(0, mBuilder.length());
            mFmt.format("%02d", mArgs);
            return mFmt.toString();
        }

        private static char getZeroDigit(Locale locale) {
            // return LocaleData.get(locale).zeroDigit;
            return new DecimalFormatSymbols(locale).getZeroDigit();
        }

        private java.util.Formatter createFormatter(Locale locale) {
            return new java.util.Formatter(mBuilder, locale);
        }
    }

    private static final TwoDigitFormatter sTwoDigitFormatter = new TwoDigitFormatter();

    public static final Formatter getTwoDigitFormatter() {
        return sTwoDigitFormatter;
    }

    /**
     * The text for showing the current value.
     */
    private final EditText mInputText;

    /**
     * The distance between the two selection dividers.
     */
    private final int mSelectionDividersDistance;

    /**
     * The min height of this widget.
     */
    private final int mMinHeight;

    /**
     * The max height of this widget.
     */
    private final int mMaxHeight;

    /**
     * The max width of this widget.
     */
    private final int mMinWidth;

    /**
     * The max width of this widget.
     */
    private int mMaxWidth;

    /**
     * Flag whether to compute the max width.
     */
    private final boolean mComputeMaxWidth;

    /**
     * The color of the text.
     */
    private int mTextColor = 0xFF000000;

    /**
     * The size of the text.
     */
    private float mTextSize = 25f;

    /**
     * The typeface of the text.
     */
    private Typeface mTypeface;

    /**
     * The width of the gap between text elements if the selector wheel.
     */
    private int mSelectorTextGapWidth;

    /**
     * The height of the gap between text elements if the selector wheel.
     */
    private int mSelectorTextGapHeight;

    /**
     * The values to be displayed instead the indices.
     */
    private String[] mDisplayedValues;

    /**
     * Lower value of the range of numbers allowed for the NumberPicker
     */
    private int mMinValue = DEFAULT_MIN_VALUE;

    /**
     * Upper value of the range of numbers allowed for the NumberPicker
     */
    private int mMaxValue = DEFAULT_MAX_VALUE;

    /**
     * Current value of this NumberPicker
     */
    private int mValue;

    /**
     * Listener to be notified upon current value change.
     */
    private OnValueChangeListener mOnValueChangeListener;

    /**
     * Listener to be notified upon scroll state change.
     */
    private OnScrollListener mOnScrollListener;

    /**
     * Formatter for for displaying the current value.
     */
    private Formatter mFormatter;

    /**
     * The speed for updating the value form long press.
     */
    private long mLongPressUpdateInterval = DEFAULT_LONG_PRESS_UPDATE_INTERVAL;

    /**
     * Cache for the string representation of selector indices.
     */
    private final SparseArray<String> mSelectorIndexToStringCache = new SparseArray<>();

    /**
     * The number of items show in the selector wheel.
     */
    private int mWheelItemCount = 3;

    /**
     * The index of the middle selector item.
     */
    private int mWheelMiddleItemIndex = mWheelItemCount / 2;

    /**
     * The selector indices whose value are show by the selector.
     */
    private int[] mSelectorIndices = new int[mWheelItemCount];

    /**
     * The {@link Paint} for drawing the selector.
     */
    private final Paint mSelectorWheelPaint;

    /**
     * The size of a selector element (text + gap).
     */
    private int mSelectorElementSize;

    /**
     * The initial offset of the scroll selector.
     */
    private int mInitialScrollOffset = Integer.MIN_VALUE;

    /**
     * The current offset of the scroll selector.
     */
    private int mCurrentScrollOffset;

    /**
     * The {@link Scroller} responsible for flinging the selector.
     */
    private final Scroller mFlingScroller;

    /**
     * The {@link Scroller} responsible for adjusting the selector.
     */
    private final Scroller mAdjustScroller;

    /**
     * The previous X coordinate while scrolling the selector.
     */
    private int mPreviousScrollerX;

    /**
     * The previous Y coordinate while scrolling the selector.
     */
    private int mPreviousScrollerY;

    /**
     * Handle to the reusable command for setting the input text selection.
     */
    private SetSelectionCommand mSetSelectionCommand;

    /**
     * Handle to the reusable command for changing the current value from long press by one.
     */
    private ChangeCurrentByOneFromLongPressCommand mChangeCurrentByOneFromLongPressCommand;

    /**
     * The X position of the last down event.
     */
    private float mLastDownEventX;

    /**
     * The Y position of the last down event.
     */
    private float mLastDownEventY;

    /**
     * The X position of the last down or move event.
     */
    private float mLastDownOrMoveEventX;

    /**
     * The Y position of the last down or move event.
     */
    private float mLastDownOrMoveEventY;

    /**
     * Determines speed during touch scrolling.
     */
    private VelocityTracker mVelocityTracker;

    /**
     * @see ViewConfiguration#getScaledTouchSlop()
     */
    private int mTouchSlop;

    /**
     * @see ViewConfiguration#getScaledMinimumFlingVelocity()
     */
    private int mMinimumFlingVelocity;

    /**
     * @see ViewConfiguration#getScaledMaximumFlingVelocity()
     */
    private int mMaximumFlingVelocity;

    /**
     * Flag whether the selector should wrap around.
     */
    private boolean mWrapSelectorWheel;

    /**
     * Divider for showing item to be selected while scrolling
     */
    private Drawable mSelectionDivider;

    /**
     * The color of the selection divider.
     */
    private int mSelectionDividerColor;

    /**
     * The thickness of the selection divider.
     */
    private final int mSelectionDividerThickness;

    /**
     * The current scroll state of the number picker.
     */
    private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;

    /**
     * The top of the top selection divider.
     */
    private int mTopSelectionDividerTop;

    /**
     * The bottom of the bottom selection divider.
     */
    private int mBottomSelectionDividerBottom;

    /**
     * The left of the top selection divider.
     */
    private int mLeftOfSelectionDividerLeft;

    /**
     * The right of the bottom selection divider.
     */
    private int mRightOfSelectionDividerRight;

    /**
     * The virtual id of the last hovered child.
     */
    private int mLastHoveredChildVirtualViewId;

    /**
     * Provider to report to clients the semantic structure of this widget.
     */
    private SupportAccessibilityNodeProvider mAccessibilityNodeProvider;

    /**
     * The keycode of the last handled DPAD down event.
     */
    private int mLastHandledDownDpadKeyCode = -1;

    /**
     * The width of this widget.
     */
    private float mWidth;

    /**
     * The height of this widget.
     */
    private float mHeight;

    /**
     * The orientation of this widget.
     */
    private int mOrientation;

    /**
     * Interface to listen for changes of the current value.
     */
    public interface OnValueChangeListener {

        /**
         * Called upon a change of the current value.
         *
         * @param picker The NumberPicker associated with this listener.
         * @param oldVal The previous value.
         * @param newVal The new value.
         */
        void onValueChange(NumberPicker picker, int oldVal, int newVal);
    }

    /**
     * Interface to listen for the picker scroll state.
     */
    public interface OnScrollListener {

        /**
         * The view is not scrolling.
         */
        public static int SCROLL_STATE_IDLE = 0;

        /**
         * The user is scrolling using touch, and his finger is still on the screen.
         */
        public static int SCROLL_STATE_TOUCH_SCROLL = 1;

        /**
         * The user had previously been scrolling using touch and performed a fling.
         */
        public static int SCROLL_STATE_FLING = 2;

        /**
         * Callback invoked while the number picker scroll state has changed.
         *
         * @param view The view whose scroll state is being reported.
         * @param scrollState The current scroll state. One of
         *            {@link #SCROLL_STATE_IDLE},
         *            {@link #SCROLL_STATE_TOUCH_SCROLL} or
         *            {@link #SCROLL_STATE_IDLE}.
         */
        public void onScrollStateChange(NumberPicker view, int scrollState);
    }

    /**
     * Interface used to format current value into a string for presentation.
     */
    public interface Formatter {

        /**
         * Formats a string representation of the current value.
         *
         * @param value The currently selected value.
         * @return A formatted string representation.
         */
        public String format(int value);
    }

    /**
     * Create a new number picker.
     *
     * @param context The application environment.
     */
    public NumberPicker(Context context) {
        this(context, null);
    }

    /**
     * Create a new number picker.
     *
     * @param context The application environment.
     * @param attrs A collection of attributes.
     */
    public NumberPicker(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.numberPickerStyle);
    }

    /**
     * Create a new number picker
     *
     * @param context the application environment.
     * @param attrs a collection of attributes.
     * @param defStyle The default style to apply to this view.
     */
    public NumberPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        TypedArray attributesArray = context.obtainStyledAttributes(attrs, R.styleable.NumberPicker, defStyle, 0);

        mSelectionDivider = getResources().getDrawable(R.drawable.np_numberpicker_selection_divider);

        mSelectionDividerColor = attributesArray.getColor(R.styleable.NumberPicker_np_dividerColor, mSelectionDividerColor);

        final int defSelectionDividerThickness = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, UNSCALED_DEFAULT_SELECTION_DIVIDER_THICKNESS,
            getResources().getDisplayMetrics());
        mSelectionDividerThickness = attributesArray.getDimensionPixelSize(
            R.styleable.NumberPicker_np_dividerThickness, defSelectionDividerThickness);

        final int defSelectionDividerDistance = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, UNSCALED_DEFAULT_SELECTION_DIVIDERS_DISTANCE,
            getResources().getDisplayMetrics());
        mSelectionDividersDistance = attributesArray.getDimensionPixelSize(
            R.styleable.NumberPicker_np_dividerDistance, defSelectionDividerDistance);

        mOrientation = attributesArray.getInt(R.styleable.NumberPicker_np_orientation, VERTICAL);

        mWidth = attributesArray.getDimensionPixelSize(R.styleable.NumberPicker_np_width, SIZE_UNSPECIFIED);
        mHeight = attributesArray.getDimensionPixelSize(R.styleable.NumberPicker_np_height, SIZE_UNSPECIFIED);

        if (isHorizontalMode()) {
            mMinHeight = SIZE_UNSPECIFIED;
            mMaxHeight = dpToPx(DEFAULT_MIN_WIDTH);
            mMinWidth = dpToPx(DEFAULT_MAX_HEIGHT);
            mMaxWidth = SIZE_UNSPECIFIED;
        } else {
            mMinHeight = SIZE_UNSPECIFIED;
            mMaxHeight = dpToPx(DEFAULT_MAX_HEIGHT);
            mMinWidth = dpToPx(DEFAULT_MIN_WIDTH);
            mMaxWidth = SIZE_UNSPECIFIED;
        }

        mComputeMaxWidth = true;

        mMaxValue = attributesArray.getInt(R.styleable.NumberPicker_np_max, mMaxValue);
        mMinValue = attributesArray.getInt(R.styleable.NumberPicker_np_min, mMinValue);

        mTextColor = attributesArray.getColor(R.styleable.NumberPicker_np_textColor, mTextColor);
        mTextSize = attributesArray.getDimension(R.styleable.NumberPicker_np_textSize, mTextSize);
        mTypeface = Typeface.create(attributesArray.getString(R.styleable.NumberPicker_np_typeface), Typeface.NORMAL);
        mFormatter = stringToFormatter(attributesArray.getString(R.styleable.NumberPicker_np_formatter));
        mWheelItemCount = attributesArray.getInt(R.styleable.NumberPicker_np_wheel_item_count, mWheelItemCount);
        attributesArray.recycle();

        // By default Linearlayout that we extend is not drawn. This is
        // its draw() method is not called but dispatchDraw() is called
        // directly (see ViewGroup.drawChild()). However, this class uses
        // the fading edge effect implemented by View and we need our
        // draw() method to be called. Therefore, we declare we will draw.
        setWillNotDraw(false);

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.number_picker_with_selector_wheel, this, true);

        // input text
        mInputText = (EditText) findViewById(R.id.np__numberpicker_input);
        mInputText.setEnabled(false);
        mInputText.setFocusable(false);
        mInputText.setFilters(new InputFilter[] {
            new InputTextFilter()
        });
        mInputText.setImeOptions(EditorInfo.IME_ACTION_NONE);
        mInputText.setTextColor(mTextColor);

        // create the selector wheel paint
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextAlign(Align.CENTER);
        ColorStateList colors = mInputText.getTextColors();
        int color = colors.getColorForState(ENABLED_STATE_SET, Color.WHITE);
        paint.setColor(color);
        mSelectorWheelPaint = paint;

        setTextSize(mTextSize);
        setTypeface(mTypeface);
        setFormatter(mFormatter);
        updateInputTextView();

        setMaxValue(mMaxValue);
        setMinValue(mMinValue);

        setDividerColor(mSelectionDividerColor);

        setWheelItemCount(mWheelItemCount);

        if (mWidth != SIZE_UNSPECIFIED && mHeight != SIZE_UNSPECIFIED) {
            setScaleX(mWidth / mMinWidth);
            setScaleY(mHeight / mMaxHeight);
        } else if (mWidth != SIZE_UNSPECIFIED) {
            setScaleX(mWidth / mMinWidth);
            setScaleY(mWidth / mMinWidth);
        } else if (mHeight != SIZE_UNSPECIFIED) {
            setScaleX(mHeight / mMaxHeight);
            setScaleY(mHeight / mMaxHeight);
        }

        // initialize constants
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity() / SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT;

        // create the fling and adjust scrollers
        mFlingScroller = new Scroller(getContext(), null, true);
        mAdjustScroller = new Scroller(getContext(), new DecelerateInterpolator(2.5f));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // If not explicitly specified this view is important for accessibility.
            if (getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
                setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int msrdWdth = getMeasuredWidth();
        final int msrdHght = getMeasuredHeight();

        // Input text centered horizontally.
        final int inptTxtMsrdWdth = mInputText.getMeasuredWidth();
        final int inptTxtMsrdHght = mInputText.getMeasuredHeight();
        final int inptTxtLeft = (msrdWdth - inptTxtMsrdWdth) / 2;
        final int inptTxtTop = (msrdHght - inptTxtMsrdHght) / 2;
        final int inptTxtRight = inptTxtLeft + inptTxtMsrdWdth;
        final int inptTxtBottom = inptTxtTop + inptTxtMsrdHght;
        mInputText.layout(inptTxtLeft, inptTxtTop, inptTxtRight, inptTxtBottom);

        if (changed) {
            // need to do all this when we know our size
            initializeSelectorWheel();
            initializeFadingEdges();

            if (isHorizontalMode()) {
                mLeftOfSelectionDividerLeft = (getWidth() - mSelectionDividersDistance) / 2 - mSelectionDividerThickness;
                mRightOfSelectionDividerRight = mLeftOfSelectionDividerLeft + 2 * mSelectionDividerThickness + mSelectionDividersDistance;
            } else {
                mTopSelectionDividerTop = (getHeight() - mSelectionDividersDistance) / 2 - mSelectionDividerThickness;
                mBottomSelectionDividerBottom = mTopSelectionDividerTop + 2 * mSelectionDividerThickness + mSelectionDividersDistance;
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Try greedily to fit the max width and height.
        final int newWidthMeasureSpec = makeMeasureSpec(widthMeasureSpec, mMaxWidth);
        final int newHeightMeasureSpec = makeMeasureSpec(heightMeasureSpec, mMaxHeight);
        super.onMeasure(newWidthMeasureSpec, newHeightMeasureSpec);
        // Flag if we are measured with width or height less than the respective min.
        final int widthSize = resolveSizeAndStateRespectingMinSize(mMinWidth, getMeasuredWidth(), widthMeasureSpec);
        final int heightSize = resolveSizeAndStateRespectingMinSize(mMinHeight, getMeasuredHeight(), heightMeasureSpec);
        setMeasuredDimension(widthSize, heightSize);
    }

    /**
     * Move to the final position of a scroller. Ensures to force finish the scroller
     * and if it is not at its final position a scroll of the selector wheel is
     * performed to fast forward to the final position.
     *
     * @param scroller The scroller to whose final position to get.
     * @return True of the a move was performed, i.e. the scroller was not in final position.
     */
    private boolean moveToFinalScrollerPosition(Scroller scroller) {
        scroller.forceFinished(true);
        if (isHorizontalMode()) {
            int amountToScroll = scroller.getFinalX() - scroller.getCurrX();
            int futureScrollOffset = (mCurrentScrollOffset + amountToScroll) % mSelectorElementSize;
            int overshootAdjustment = mInitialScrollOffset - futureScrollOffset;
            if (overshootAdjustment != 0) {
                if (Math.abs(overshootAdjustment) > mSelectorElementSize / 2) {
                    if (overshootAdjustment > 0) {
                        overshootAdjustment -= mSelectorElementSize;
                    } else {
                        overshootAdjustment += mSelectorElementSize;
                    }
                }
                amountToScroll += overshootAdjustment;
                scrollBy(amountToScroll, 0);
                return true;
            }
        } else {
            int amountToScroll = scroller.getFinalY() - scroller.getCurrY();
            int futureScrollOffset = (mCurrentScrollOffset + amountToScroll) % mSelectorElementSize;
            int overshootAdjustment = mInitialScrollOffset - futureScrollOffset;
            if (overshootAdjustment != 0) {
                if (Math.abs(overshootAdjustment) > mSelectorElementSize / 2) {
                    if (overshootAdjustment > 0) {
                        overshootAdjustment -= mSelectorElementSize;
                    } else {
                        overshootAdjustment += mSelectorElementSize;
                    }
                }
                amountToScroll += overshootAdjustment;
                scrollBy(0, amountToScroll);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                removeAllCallbacks();
                mInputText.setVisibility(View.INVISIBLE);
                if (isHorizontalMode()) {
                    mLastDownOrMoveEventX = mLastDownEventX = event.getX();
                    // Make sure we support flinging inside scrollables.
                    getParent().requestDisallowInterceptTouchEvent(true);
                    if (!mFlingScroller.isFinished()) {
                        mFlingScroller.forceFinished(true);
                        mAdjustScroller.forceFinished(true);
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                    } else if (!mAdjustScroller.isFinished()) {
                        mFlingScroller.forceFinished(true);
                        mAdjustScroller.forceFinished(true);
                    } else if (mLastDownEventX < mLeftOfSelectionDividerLeft) {
                        postChangeCurrentByOneFromLongPress(false, ViewConfiguration.getLongPressTimeout());
                    } else if (mLastDownEventX > mRightOfSelectionDividerRight) {
                        postChangeCurrentByOneFromLongPress(true, ViewConfiguration.getLongPressTimeout());
                    }
                    return true;
                } else {
                    mLastDownOrMoveEventY = mLastDownEventY = event.getY();
                    // Make sure we support flinging inside scrollables.
                    getParent().requestDisallowInterceptTouchEvent(true);
                    if (!mFlingScroller.isFinished()) {
                        mFlingScroller.forceFinished(true);
                        mAdjustScroller.forceFinished(true);
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                    } else if (!mAdjustScroller.isFinished()) {
                        mFlingScroller.forceFinished(true);
                        mAdjustScroller.forceFinished(true);
                    } else if (mLastDownEventX < mLeftOfSelectionDividerLeft) {
                        postChangeCurrentByOneFromLongPress(false, ViewConfiguration.getLongPressTimeout());
                    } else if (mLastDownEventX > mRightOfSelectionDividerRight) {
                        postChangeCurrentByOneFromLongPress(true, ViewConfiguration.getLongPressTimeout());
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                if (isHorizontalMode()) {
                    float currentMoveX = event.getX();
                    if (mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                        int deltaDownX = (int) Math.abs(currentMoveX - mLastDownEventX);
                        if (deltaDownX > mTouchSlop) {
                            removeAllCallbacks();
                            onScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                        }
                    } else {
                        int deltaMoveX = (int) ((currentMoveX - mLastDownOrMoveEventX));
                        scrollBy(deltaMoveX, 0);
                        invalidate();
                    }
                    mLastDownOrMoveEventX = currentMoveX;
                } else {
                    float currentMoveY = event.getY();
                    if (mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                        int deltaDownY = (int) Math.abs(currentMoveY - mLastDownEventY);
                        if (deltaDownY > mTouchSlop) {
                            removeAllCallbacks();
                            onScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                        }
                    } else {
                        int deltaMoveY = (int) ((currentMoveY - mLastDownOrMoveEventY));
                        scrollBy(0, deltaMoveY);
                        invalidate();
                    }
                    mLastDownOrMoveEventY = currentMoveY;
                }
            }
            break;
            case MotionEvent.ACTION_UP: {
                removeChangeCurrentByOneFromLongPress();
                VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                if (isHorizontalMode()) {
                    int initialVelocity = (int) velocityTracker.getXVelocity();
                    if (Math.abs(initialVelocity) > mMinimumFlingVelocity) {
                        fling(initialVelocity);
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
                    } else {
                        int eventX = (int) event.getX();
                        int deltaMoveX = (int) Math.abs(eventX - mLastDownEventX);
                        if (deltaMoveX <= mTouchSlop) { // && deltaTime < ViewConfiguration.getTapTimeout()) {
                            int selectorIndexOffset = (eventX / mSelectorElementSize) - mWheelMiddleItemIndex;
                            if (selectorIndexOffset > 0) {
                                changeValueByOne(true);
                            } else if (selectorIndexOffset < 0) {
                                changeValueByOne(false);
                            }
                        } else {
                            ensureScrollWheelAdjusted();
                        }
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                    }
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                } else {
                    int initialVelocity = (int) velocityTracker.getYVelocity();
                    if (Math.abs(initialVelocity) > mMinimumFlingVelocity) {
                        fling(initialVelocity);
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
                    } else {
                        int eventY = (int) event.getY();
                        int deltaMoveY = (int) Math.abs(eventY - mLastDownEventY);
                        if (deltaMoveY <= mTouchSlop) { // && deltaTime < ViewConfiguration.getTapTimeout()) {
                            int selectorIndexOffset = (eventY / mSelectorElementSize) - mWheelMiddleItemIndex;
                            if (selectorIndexOffset > 0) {
                                changeValueByOne(true);
                            } else if (selectorIndexOffset < 0) {
                                changeValueByOne(false);
                            }
                        } else {
                            ensureScrollWheelAdjusted();
                        }
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                    }
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
            }
            break;
        }
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                removeAllCallbacks();
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                removeAllCallbacks();
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_UP:
                switch (event.getAction()) {
                    case KeyEvent.ACTION_DOWN:
                        if (mWrapSelectorWheel || (keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
                                ? getValue() < getMaxValue() : getValue() > getMinValue()) {
                            requestFocus();
                            mLastHandledDownDpadKeyCode = keyCode;
                            removeAllCallbacks();
                            if (mFlingScroller.isFinished()) {
                                changeValueByOne(keyCode == KeyEvent.KEYCODE_DPAD_DOWN);
                            }
                            return true;
                        }
                        break;
                    case KeyEvent.ACTION_UP:
                        if (mLastHandledDownDpadKeyCode == keyCode) {
                            mLastHandledDownDpadKeyCode = -1;
                            return true;
                        }
                        break;
                }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent event) {
        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                removeAllCallbacks();
                break;
        }
        return super.dispatchTrackballEvent(event);
    }

    @Override
    protected boolean dispatchHoverEvent(MotionEvent event) {
        if (((AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE)).isEnabled()) {
            final int hoveredVirtualViewId;
            if (isHorizontalMode()) {
                final int eventX = (int) event.getX();
                if (eventX < mLeftOfSelectionDividerLeft) {
                    hoveredVirtualViewId = AccessibilityNodeProviderImpl.VIRTUAL_VIEW_ID_DECREMENT;
                } else if (eventX > mRightOfSelectionDividerRight) {
                    hoveredVirtualViewId = AccessibilityNodeProviderImpl.VIRTUAL_VIEW_ID_INCREMENT;
                } else {
                    hoveredVirtualViewId = AccessibilityNodeProviderImpl.VIRTUAL_VIEW_ID_INPUT;
                }
            } else {
                final int eventY = (int) event.getY();
                if (eventY < mTopSelectionDividerTop) {
                    hoveredVirtualViewId = AccessibilityNodeProviderImpl.VIRTUAL_VIEW_ID_DECREMENT;
                } else if (eventY > mBottomSelectionDividerBottom) {
                    hoveredVirtualViewId = AccessibilityNodeProviderImpl.VIRTUAL_VIEW_ID_INCREMENT;
                } else {
                    hoveredVirtualViewId = AccessibilityNodeProviderImpl.VIRTUAL_VIEW_ID_INPUT;
                }
            }
            final int action = event.getAction() & MotionEvent.ACTION_MASK;
            SupportAccessibilityNodeProvider provider = getSupportAccessibilityNodeProvider();

            switch (action) {
            case MotionEvent.ACTION_HOVER_ENTER: {
                provider.sendAccessibilityEventForVirtualView(hoveredVirtualViewId, AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);
                mLastHoveredChildVirtualViewId = hoveredVirtualViewId;
                provider.performAction(hoveredVirtualViewId, AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
                break;
            }
            case MotionEvent.ACTION_HOVER_MOVE: {
                if (mLastHoveredChildVirtualViewId != hoveredVirtualViewId && mLastHoveredChildVirtualViewId != View.NO_ID) {
                    provider.sendAccessibilityEventForVirtualView(mLastHoveredChildVirtualViewId, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);
                    provider.sendAccessibilityEventForVirtualView(hoveredVirtualViewId, AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);
                    mLastHoveredChildVirtualViewId = hoveredVirtualViewId;
                    provider.performAction(hoveredVirtualViewId, AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
                }
                break;
            }
            case MotionEvent.ACTION_HOVER_EXIT: {
                provider.sendAccessibilityEventForVirtualView(hoveredVirtualViewId, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);
                mLastHoveredChildVirtualViewId = View.NO_ID;
                break;
            }
            }
        }
        return false;
    }

    @Override
    public void computeScroll() {
        Scroller scroller = mFlingScroller;
        if (scroller.isFinished()) {
            scroller = mAdjustScroller;
            if (scroller.isFinished()) {
                return;
            }
        }
        scroller.computeScrollOffset();
        if (isHorizontalMode()) {
            int currentScrollerX = scroller.getCurrX();
            if (mPreviousScrollerX == 0) {
                mPreviousScrollerX = scroller.getStartX();
            }
            scrollBy(currentScrollerX - mPreviousScrollerX, 0);
            mPreviousScrollerX = currentScrollerX;
        } else {
            int currentScrollerY = scroller.getCurrY();
            if (mPreviousScrollerY == 0) {
                mPreviousScrollerY = scroller.getStartY();
            }
            scrollBy(0, currentScrollerY - mPreviousScrollerY);
            mPreviousScrollerY = currentScrollerY;
        }
        if (scroller.isFinished()) {
            onScrollerFinished(scroller);
        } else {
            invalidate();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mInputText.setEnabled(enabled);
    }

    @Override
    public void scrollBy(int x, int y) {
        int[] selectorIndices = mSelectorIndices;
        if (!mWrapSelectorWheel && y > 0
                && selectorIndices[mWheelMiddleItemIndex] <= mMinValue) {
            mCurrentScrollOffset = mInitialScrollOffset;
            return;
        }
        if (!mWrapSelectorWheel && y < 0
                && selectorIndices[mWheelMiddleItemIndex] >= mMaxValue) {
            mCurrentScrollOffset = mInitialScrollOffset;
            return;
        }
        if (isHorizontalMode()) {
            mCurrentScrollOffset += x;
            while (mCurrentScrollOffset - mInitialScrollOffset > mSelectorTextGapWidth) {
                mCurrentScrollOffset -= mSelectorElementSize;
                decrementSelectorIndices(selectorIndices);
                setValueInternal(selectorIndices[mWheelMiddleItemIndex], true);
                if (!mWrapSelectorWheel && selectorIndices[mWheelMiddleItemIndex] <= mMinValue) {
                    mCurrentScrollOffset = mInitialScrollOffset;
                }
            }
            while (mCurrentScrollOffset - mInitialScrollOffset < -mSelectorTextGapWidth) {
                mCurrentScrollOffset += mSelectorElementSize;
                incrementSelectorIndices(selectorIndices);
                setValueInternal(selectorIndices[mWheelMiddleItemIndex], true);
                if (!mWrapSelectorWheel && selectorIndices[mWheelMiddleItemIndex] >= mMaxValue) {
                    mCurrentScrollOffset = mInitialScrollOffset;
                }
            }
        } else {
            mCurrentScrollOffset += y;
            while (mCurrentScrollOffset - mInitialScrollOffset > mSelectorTextGapHeight) {
                mCurrentScrollOffset -= mSelectorElementSize;
                decrementSelectorIndices(selectorIndices);
                setValueInternal(selectorIndices[mWheelMiddleItemIndex], true);
                if (!mWrapSelectorWheel && selectorIndices[mWheelMiddleItemIndex] <= mMinValue) {
                    mCurrentScrollOffset = mInitialScrollOffset;
                }
            }
            while (mCurrentScrollOffset - mInitialScrollOffset < -mSelectorTextGapHeight) {
                mCurrentScrollOffset += mSelectorElementSize;
                incrementSelectorIndices(selectorIndices);
                setValueInternal(selectorIndices[mWheelMiddleItemIndex], true);
                if (!mWrapSelectorWheel && selectorIndices[mWheelMiddleItemIndex] >= mMaxValue) {
                    mCurrentScrollOffset = mInitialScrollOffset;
                }
            }
        }
    }

    /**
     * Sets the listener to be notified on change of the current value.
     *
     * @param onValueChangedListener The listener.
     */
    public void setOnValueChangedListener(OnValueChangeListener onValueChangedListener) {
        mOnValueChangeListener = onValueChangedListener;
    }

    /**
     * Set listener to be notified for scroll state changes.
     *
     * @param onScrollListener The listener.
     */
    public void setOnScrollListener(OnScrollListener onScrollListener) {
        mOnScrollListener = onScrollListener;
    }

    /**
     * Set the formatter to be used for formatting the current value.
     * <p>
     * Note: If you have provided alternative values for the values this
     * formatter is never invoked.
     * </p>
     *
     * @param formatter The formatter object. If formatter is <code>null</code>,
     *            {@link String#valueOf(int)} will be used.
     *@see #setDisplayedValues(String[])
     */
    public void setFormatter(Formatter formatter) {
        if (formatter == mFormatter) {
            return;
        }
        mFormatter = formatter;
        initializeSelectorWheelIndices();
        updateInputTextView();
    }

    /**
     * Set the current value for the number picker.
     * <p>
     * If the argument is less than the {@link NumberPicker#getMinValue()} and
     * {@link NumberPicker#getWrapSelectorWheel()} is <code>false</code> the
     * current value is set to the {@link NumberPicker#getMinValue()} value.
     * </p>
     * <p>
     * If the argument is less than the {@link NumberPicker#getMinValue()} and
     * {@link NumberPicker#getWrapSelectorWheel()} is <code>true</code> the
     * current value is set to the {@link NumberPicker#getMaxValue()} value.
     * </p>
     * <p>
     * If the argument is less than the {@link NumberPicker#getMaxValue()} and
     * {@link NumberPicker#getWrapSelectorWheel()} is <code>false</code> the
     * current value is set to the {@link NumberPicker#getMaxValue()} value.
     * </p>
     * <p>
     * If the argument is less than the {@link NumberPicker#getMaxValue()} and
     * {@link NumberPicker#getWrapSelectorWheel()} is <code>true</code> the
     * current value is set to the {@link NumberPicker#getMinValue()} value.
     * </p>
     *
     * @param value The current value.
     * @see #setWrapSelectorWheel(boolean)
     * @see #setMinValue(int)
     * @see #setMaxValue(int)
     */
    public void setValue(int value) {
        setValueInternal(value, false);
    }

    /**
     * Computes the max width if no such specified as an attribute.
     */
    private void tryComputeMaxWidth() {
        if (!mComputeMaxWidth) {
            return;
        }
        int maxTextWidth = 0;
        if (mDisplayedValues == null) {
            float maxDigitWidth = 0;
            for (int i = 0; i <= 9; i++) {
                final float digitWidth = mSelectorWheelPaint.measureText(formatNumberWithLocale(i));
                if (digitWidth > maxDigitWidth) {
                    maxDigitWidth = digitWidth;
                }
            }
            int numberOfDigits = 0;
            int current = mMaxValue;
            while (current > 0) {
                numberOfDigits++;
                current = current / 10;
            }
            maxTextWidth = (int) (numberOfDigits * maxDigitWidth);
        } else {
            final int valueCount = mDisplayedValues.length;
            for (int i = 0; i < valueCount; i++) {
                final float textWidth = mSelectorWheelPaint.measureText(mDisplayedValues[i]);
                if (textWidth > maxTextWidth) {
                    maxTextWidth = (int) textWidth;
                }
            }
        }
        maxTextWidth += mInputText.getPaddingLeft() + mInputText.getPaddingRight();
        if (mMaxWidth != maxTextWidth) {
            if (maxTextWidth > mMinWidth) {
                mMaxWidth = maxTextWidth;
            } else {
                mMaxWidth = mMinWidth;
            }
            invalidate();
        }
    }

    /**
     * Gets whether the selector wheel wraps when reaching the min/max value.
     *
     * @return True if the selector wheel wraps.
     *
     * @see #getMinValue()
     * @see #getMaxValue()
     */
    public boolean getWrapSelectorWheel() {
        return mWrapSelectorWheel;
    }

    /**
     * Sets whether the selector wheel shown during flinging/scrolling should
     * wrap around the {@link NumberPicker#getMinValue()} and
     * {@link NumberPicker#getMaxValue()} values.
     * <p>
     * By default if the range (max - min) is more than the number of items shown
     * on the selector wheel the selector wheel wrapping is enabled.
     * </p>
     * <p>
     * <strong>Note:</strong> If the number of items, i.e. the range (
     * {@link #getMaxValue()} - {@link #getMinValue()}) is less than
     * the number of items shown on the selector wheel, the selector wheel will
     * not wrap. Hence, in such a case calling this method is a NOP.
     * </p>
     *
     * @param wrapSelectorWheel Whether to wrap.
     */
    public void setWrapSelectorWheel(boolean wrapSelectorWheel) {
        final boolean wrappingAllowed = (mMaxValue - mMinValue) >= mSelectorIndices.length;
        if ((!wrapSelectorWheel || wrappingAllowed) && wrapSelectorWheel != mWrapSelectorWheel) {
            mWrapSelectorWheel = wrapSelectorWheel;
        }
    }

    /**
     * Sets the speed at which the numbers be incremented and decremented when
     * the up and down buttons are long pressed respectively.
     * <p>
     * The default value is 300 ms.
     * </p>
     *
     * @param intervalMillis The speed (in milliseconds) at which the numbers
     *            will be incremented and decremented.
     */
    public void setOnLongPressUpdateInterval(long intervalMillis) {
        mLongPressUpdateInterval = intervalMillis;
    }

    /**
     * Returns the value of the picker.
     *
     * @return The value.
     */
    public int getValue() {
        return mValue;
    }

    /**
     * Returns the min value of the picker.
     *
     * @return The min value
     */
    public int getMinValue() {
        return mMinValue;
    }

    /**
     * Sets the min value of the picker.
     *
     * @param minValue The min value inclusive.
     *
     * <strong>Note:</strong> The length of the displayed values array
     * set via {@link #setDisplayedValues(String[])} must be equal to the
     * range of selectable numbers which is equal to
     * {@link #getMaxValue()} - {@link #getMinValue()} + 1.
     */
    public void setMinValue(int minValue) {
//        if (minValue < 0) {
//            throw new IllegalArgumentException("minValue must be >= 0");
//        }
        mMinValue = minValue;
        if (mMinValue > mValue) {
            mValue = mMinValue;
        }
        boolean wrapSelectorWheel = mMaxValue - mMinValue > mSelectorIndices.length;
        setWrapSelectorWheel(wrapSelectorWheel);
        initializeSelectorWheelIndices();
        updateInputTextView();
        tryComputeMaxWidth();
        invalidate();
    }

    /**
     * Returns the max value of the picker.
     *
     * @return The max value.
     */
    public int getMaxValue() {
        return mMaxValue;
    }

    /**
     * Sets the max value of the picker.
     *
     * @param maxValue The max value inclusive.
     *
     * <strong>Note:</strong> The length of the displayed values array
     * set via {@link #setDisplayedValues(String[])} must be equal to the
     * range of selectable numbers which is equal to
     * {@link #getMaxValue()} - {@link #getMinValue()} + 1.
     */
    public void setMaxValue(int maxValue) {
        if (maxValue < 0) {
            throw new IllegalArgumentException("maxValue must be >= 0");
        }
        mMaxValue = maxValue;
        if (mMaxValue < mValue) {
            mValue = mMaxValue;
        }

        boolean wrapSelectorWheel = mMaxValue - mMinValue > mSelectorIndices.length;
        setWrapSelectorWheel(wrapSelectorWheel);
        initializeSelectorWheelIndices();
        updateInputTextView();
        tryComputeMaxWidth();
        invalidate();
    }

    /**
     * Gets the values to be displayed instead of string values.
     *
     * @return The displayed values.
     */
    public String[] getDisplayedValues() {
        return mDisplayedValues;
    }

    /**
     * Sets the values to be displayed.
     *
     * @param displayedValues The displayed values.
     *
     * <strong>Note:</strong> The length of the displayed values array
     * must be equal to the range of selectable numbers which is equal to
     * {@link #getMaxValue()} - {@link #getMinValue()} + 1.
     */
    public void setDisplayedValues(String[] displayedValues) {
        if (mDisplayedValues == displayedValues) {
            return;
        }
        mDisplayedValues = displayedValues;
        if (mDisplayedValues != null) {
            // Allow text entry rather than strictly numeric entry.
            mInputText.setRawInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        } else {
            mInputText.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        }
        updateInputTextView();
        initializeSelectorWheelIndices();
        tryComputeMaxWidth();
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        return isHorizontalMode() ? 0: FADING_EDGE_STRENGTH;
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        return isHorizontalMode() ? 0: FADING_EDGE_STRENGTH;
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        return isHorizontalMode() ? FADING_EDGE_STRENGTH : 0;
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        return isHorizontalMode() ? FADING_EDGE_STRENGTH : 0;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeAllCallbacks();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float x, y;
        if (isHorizontalMode()) {
            x = mCurrentScrollOffset;
            y = mInputText.getBaseline() + mInputText.getTop();
        } else {
            x = (getRight() - getLeft()) / 2;
            y = mCurrentScrollOffset;
        }

        // draw the selector wheel
        int[] selectorIndices = mSelectorIndices;
        for (int i = 0; i < selectorIndices.length; i++) {
            int selectorIndex = selectorIndices[i];
            String scrollSelectorValue = mSelectorIndexToStringCache.get(selectorIndex);
            // Do not draw the middle item if input is visible since the input
            // is shown only if the wheel is static and it covers the middle
            // item. Otherwise, if the user starts editing the text via the
            // IME he may see a dimmed version of the old value intermixed
            // with the new one.
            if (i != mWheelMiddleItemIndex || mInputText.getVisibility() != VISIBLE) {
                canvas.drawText(scrollSelectorValue, x, y, mSelectorWheelPaint);
            }

            if (isHorizontalMode()) {
                x += mSelectorElementSize;
            } else {
                y += mSelectorElementSize;
            }
        }

        // draw the selection dividers
        if (mSelectionDivider != null) {
            if (isHorizontalMode()) {
                // draw the left divider
                int leftOfLeftDivider = mLeftOfSelectionDividerLeft;
                int rightOfLeftDivider = leftOfLeftDivider + mSelectionDividerThickness;
                mSelectionDivider.setBounds(leftOfLeftDivider, 0, rightOfLeftDivider, getBottom());
                mSelectionDivider.draw(canvas);

                // draw the right divider
                int rightOfRightDivider = mRightOfSelectionDividerRight;
                int leftOfRightDivider = rightOfRightDivider - mSelectionDividerThickness;
                mSelectionDivider.setBounds(leftOfRightDivider, 0, rightOfRightDivider, getBottom());
                mSelectionDivider.draw(canvas);
            } else {
                // draw the top divider
                int topOfTopDivider = mTopSelectionDividerTop;
                int bottomOfTopDivider = topOfTopDivider + mSelectionDividerThickness;
                mSelectionDivider.setBounds(0, topOfTopDivider, getRight(), bottomOfTopDivider);
                mSelectionDivider.draw(canvas);

                // draw the bottom divider
                int bottomOfBottomDivider = mBottomSelectionDividerBottom;
                int topOfBottomDivider = bottomOfBottomDivider - mSelectionDividerThickness;
                mSelectionDivider.setBounds(0, topOfBottomDivider, getRight(), bottomOfBottomDivider);
                mSelectionDivider.draw(canvas);
            }
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(NumberPicker.class.getName());
        event.setScrollable(true);
        final int scroll = (mMinValue + mValue) * mSelectorElementSize;
        final int maxScroll = (mMaxValue - mMinValue) * mSelectorElementSize;
        if (isHorizontalMode()) {
            event.setScrollX(scroll);
            event.setMaxScrollX(maxScroll);
        } else {
            event.setScrollY(scroll);
            event.setMaxScrollY(maxScroll);
        }
    }

    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        if (mAccessibilityNodeProvider == null) {
            mAccessibilityNodeProvider = new SupportAccessibilityNodeProvider();
        }
        return mAccessibilityNodeProvider.mProvider;
    }

    /**
     * Makes a measure spec that tries greedily to use the max value.
     *
     * @param measureSpec The measure spec.
     * @param maxSize The max value for the size.
     * @return A measure spec greedily imposing the max size.
     */
    private int makeMeasureSpec(int measureSpec, int maxSize) {
        if (maxSize == SIZE_UNSPECIFIED) {
            return measureSpec;
        }
        final int size = MeasureSpec.getSize(measureSpec);
        final int mode = MeasureSpec.getMode(measureSpec);
        switch (mode) {
            case MeasureSpec.EXACTLY:
                return measureSpec;
            case MeasureSpec.AT_MOST:
                return MeasureSpec.makeMeasureSpec(Math.min(size, maxSize), MeasureSpec.EXACTLY);
            case MeasureSpec.UNSPECIFIED:
                return MeasureSpec.makeMeasureSpec(maxSize, MeasureSpec.EXACTLY);
            default:
                throw new IllegalArgumentException("Unknown measure mode: " + mode);
        }
    }

    /**
     * Utility to reconcile a desired size and state, with constraints imposed
     * by a MeasureSpec. Tries to respect the min size, unless a different size
     * is imposed by the constraints.
     *
     * @param minSize The minimal desired size.
     * @param measuredSize The currently measured size.
     * @param measureSpec The current measure spec.
     * @return The resolved size and state.
     */
    private int resolveSizeAndStateRespectingMinSize(int minSize, int measuredSize, int measureSpec) {
        if (minSize != SIZE_UNSPECIFIED) {
            final int desiredWidth = Math.max(minSize, measuredSize);
            return resolveSizeAndState(desiredWidth, measureSpec, 0);
        } else {
            return measuredSize;
        }
    }

    /**
     * Utility to reconcile a desired size and state, with constraints imposed
     * by a MeasureSpec.  Will take the desired size, unless a different size
     * is imposed by the constraints.  The returned value is a compound integer,
     * with the resolved size in the {@link #MEASURED_SIZE_MASK} bits and
     * optionally the bit {@link #MEASURED_STATE_TOO_SMALL} set if the resulting
     * size is smaller than the size the view wants to be.
     *
     * @param size How big the view wants to be
     * @param measureSpec Constraints imposed by the parent
     * @return Size information bit mask as defined by
     * {@link #MEASURED_SIZE_MASK} and {@link #MEASURED_STATE_TOO_SMALL}.
     */
    public static int resolveSizeAndState(int size, int measureSpec, int childMeasuredState) {
        int result = size;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize =  MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                result = size;
                break;
            case MeasureSpec.AT_MOST:
                if (specSize < size) {
                    result = specSize | MEASURED_STATE_TOO_SMALL;
                } else {
                    result = size;
                }
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;
        }
        return result | (childMeasuredState&MEASURED_STATE_MASK);
    }

    /**
     * Resets the selector indices and clear the cached string representation of
     * these indices.
     */
    private void initializeSelectorWheelIndices() {
        mSelectorIndexToStringCache.clear();
        int[] selectorIndices = mSelectorIndices;
        int current = getValue();
        for (int i = 0; i < mSelectorIndices.length; i++) {
            int selectorIndex = current + (i - mWheelMiddleItemIndex);
            if (mWrapSelectorWheel) {
                selectorIndex = getWrappedSelectorIndex(selectorIndex);
            }
            selectorIndices[i] = selectorIndex;
            ensureCachedScrollSelectorValue(selectorIndices[i]);
        }
    }

    /**
     * Sets the current value of this NumberPicker.
     *
     * @param current The new value of the NumberPicker.
     * @param notifyChange Whether to notify if the current value changed.
     */
    private void setValueInternal(int current, boolean notifyChange) {
        if (mValue == current) {
            return;
        }
        // Wrap around the values if we go past the start or end
        if (mWrapSelectorWheel) {
            current = getWrappedSelectorIndex(current);
        } else {
            current = Math.max(current, mMinValue);
            current = Math.min(current, mMaxValue);
        }
        int previous = mValue;
        mValue = current;
        updateInputTextView();
        if (notifyChange) {
            notifyChange(previous, current);
        }
        initializeSelectorWheelIndices();
        invalidate();
    }

    /**
     * Changes the current value by one which is increment or
     * decrement based on the passes argument.
     * decrement the current value.
     *
     * @param increment True to increment, false to decrement.
     */
    private void changeValueByOne(boolean increment) {
        mInputText.setVisibility(View.INVISIBLE);
        if (!moveToFinalScrollerPosition(mFlingScroller)) {
            moveToFinalScrollerPosition(mAdjustScroller);
        }
        if (isHorizontalMode()) {
            mPreviousScrollerX = 0;
            if (increment) {
                mFlingScroller.startScroll(0, 0, -mSelectorElementSize, 0, SNAP_SCROLL_DURATION);
            } else {
                mFlingScroller.startScroll(0, 0, mSelectorElementSize, 0, SNAP_SCROLL_DURATION);
            }
        } else {
            mPreviousScrollerY = 0;
            if (increment) {
                mFlingScroller.startScroll(0, 0, 0, -mSelectorElementSize, SNAP_SCROLL_DURATION);
            } else {
                mFlingScroller.startScroll(0, 0, 0, mSelectorElementSize, SNAP_SCROLL_DURATION);
            }
        }
        invalidate();
    }

    private void initializeSelectorWheel() {
        initializeSelectorWheelIndices();
        int[] selectorIndices = mSelectorIndices;
        int totalTextSize = selectorIndices.length * (int) mTextSize;
        int editTextTextPosition;
        if (isHorizontalMode()) {
            float totalTextGapWidth = (getRight() - getLeft()) - totalTextSize;
            float textGapCount = selectorIndices.length;
            mSelectorTextGapWidth = (int) (totalTextGapWidth / textGapCount + 0.5f);
            mSelectorElementSize = (int) mTextSize + mSelectorTextGapWidth;
            // Ensure that the middle item is positioned the same as the text in mInputText
            editTextTextPosition = mInputText.getRight() / 2;
        } else {
            float totalTextGapHeight = (getBottom() - getTop()) - totalTextSize;
            float textGapCount = selectorIndices.length;
            mSelectorTextGapHeight = (int) (totalTextGapHeight / textGapCount + 0.5f);
            mSelectorElementSize = (int) mTextSize + mSelectorTextGapHeight;
            // Ensure that the middle item is positioned the same as the text in mInputText
            editTextTextPosition = mInputText.getBaseline() + mInputText.getTop();
        }
        mInitialScrollOffset = editTextTextPosition - (mSelectorElementSize * mWheelMiddleItemIndex);
        mCurrentScrollOffset = mInitialScrollOffset;
        updateInputTextView();
    }

    private void initializeFadingEdges() {
        if (isHorizontalMode()) {
            setHorizontalFadingEdgeEnabled(true);
            setFadingEdgeLength((getRight() - getLeft() - (int) mTextSize) / 2);
        } else {
            setVerticalFadingEdgeEnabled(true);
            setFadingEdgeLength((getBottom() - getTop() - (int) mTextSize) / 2);
        }
    }

    /**
     * Callback invoked upon completion of a given <code>scroller</code>.
     */
    private void onScrollerFinished(Scroller scroller) {
        if (scroller == mFlingScroller) {
            if (!ensureScrollWheelAdjusted()) {
                updateInputTextView();
            }
            onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
        } else {
            if (mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                updateInputTextView();
            }
        }
    }

    /**
     * Handles transition to a given <code>scrollState</code>
     */
    private void onScrollStateChange(int scrollState) {
        if (mScrollState == scrollState) {
            return;
        }
        mScrollState = scrollState;
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChange(this, scrollState);
        }
    }

    /**
     * Flings the selector with the given <code>velocityY</code>.
     */
    private void fling(int velocity) {
        if (isHorizontalMode()) {
            mPreviousScrollerX = 0;
            if (velocity > 0) {
                mFlingScroller.fling(0, 0, velocity, 0, 0, Integer.MAX_VALUE, 0, 0);
            } else {
                mFlingScroller.fling(Integer.MAX_VALUE, 0, velocity, 0, 0, Integer.MAX_VALUE, 0, 0);
            }
        } else {
            mPreviousScrollerY = 0;
            if (velocity > 0) {
                mFlingScroller.fling(0, 0, 0, velocity, 0, 0, 0, Integer.MAX_VALUE);
            } else {
                mFlingScroller.fling(0, Integer.MAX_VALUE, 0, velocity, 0, 0, 0, Integer.MAX_VALUE);
            }
        }

        invalidate();
    }

    /**
     * @return The wrapped index <code>selectorIndex</code> value.
     */
    private int getWrappedSelectorIndex(int selectorIndex) {
        if (selectorIndex > mMaxValue) {
            return mMinValue + (selectorIndex - mMaxValue) % (mMaxValue - mMinValue) - 1;
        } else if (selectorIndex < mMinValue) {
            return mMaxValue - (mMinValue - selectorIndex) % (mMaxValue - mMinValue) + 1;
        }
        return selectorIndex;
    }

    /**
     * Increments the <code>selectorIndices</code> whose string representations
     * will be displayed in the selector.
     */
    private void incrementSelectorIndices(int[] selectorIndices) {
        for (int i = 0; i < selectorIndices.length - 1; i++) {
            selectorIndices[i] = selectorIndices[i + 1];
        }
        int nextScrollSelectorIndex = selectorIndices[selectorIndices.length - 2] + 1;
        if (mWrapSelectorWheel && nextScrollSelectorIndex > mMaxValue) {
            nextScrollSelectorIndex = mMinValue;
        }
        selectorIndices[selectorIndices.length - 1] = nextScrollSelectorIndex;
        ensureCachedScrollSelectorValue(nextScrollSelectorIndex);
    }

    /**
     * Decrements the <code>selectorIndices</code> whose string representations
     * will be displayed in the selector.
     */
    private void decrementSelectorIndices(int[] selectorIndices) {
        for (int i = selectorIndices.length - 1; i > 0; i--) {
            selectorIndices[i] = selectorIndices[i - 1];
        }
        int nextScrollSelectorIndex = selectorIndices[1] - 1;
        if (mWrapSelectorWheel && nextScrollSelectorIndex < mMinValue) {
            nextScrollSelectorIndex = mMaxValue;
        }
        selectorIndices[0] = nextScrollSelectorIndex;
        ensureCachedScrollSelectorValue(nextScrollSelectorIndex);
    }

    /**
     * Ensures we have a cached string representation of the given <code>
     * selectorIndex</code> to avoid multiple instantiations of the same string.
     */
    private void ensureCachedScrollSelectorValue(int selectorIndex) {
        SparseArray<String> cache = mSelectorIndexToStringCache;
        String scrollSelectorValue = cache.get(selectorIndex);
        if (scrollSelectorValue != null) {
            return;
        }
        if (selectorIndex < mMinValue || selectorIndex > mMaxValue) {
            scrollSelectorValue = "";
        } else {
            if (mDisplayedValues != null) {
                int displayedValueIndex = selectorIndex - mMinValue;
                scrollSelectorValue = mDisplayedValues[displayedValueIndex];
            } else {
                scrollSelectorValue = formatNumber(selectorIndex);
            }
        }
        cache.put(selectorIndex, scrollSelectorValue);
    }

    private String formatNumber(int value) {
        return (mFormatter != null) ? mFormatter.format(value) : formatNumberWithLocale(value);
    }

    /**
     * Updates the view of this NumberPicker. If displayValues were specified in
     * the string corresponding to the index specified by the current value will
     * be returned. Otherwise, the formatter specified in {@link #setFormatter}
     * will be used to format the number.
     *
     * @return Whether the text was updated.
     */
    private boolean updateInputTextView() {
        /*
         * If we don't have displayed values then use the current number else
         * find the correct value in the displayed values for the current
         * number.
         */
        String text = (mDisplayedValues == null) ? formatNumber(mValue)
            : mDisplayedValues[mValue - mMinValue];
        if (!TextUtils.isEmpty(text) && !text.equals(mInputText.getText().toString())) {
            mInputText.setText(text);
            return true;
        }

        return false;
    }

    /**
     * Notifies the listener, if registered, of a change of the value of this
     * NumberPicker.
     */
    private void notifyChange(int previous, int current) {
        if (mOnValueChangeListener != null) {
            mOnValueChangeListener.onValueChange(this, previous, mValue);
        }
    }

    /**
     * Posts a command for changing the current value by one.
     *
     * @param increment Whether to increment or decrement the value.
     */
    private void postChangeCurrentByOneFromLongPress(boolean increment, long delayMillis) {
        if (mChangeCurrentByOneFromLongPressCommand == null) {
            mChangeCurrentByOneFromLongPressCommand = new ChangeCurrentByOneFromLongPressCommand();
        } else {
            removeCallbacks(mChangeCurrentByOneFromLongPressCommand);
        }
        mChangeCurrentByOneFromLongPressCommand.setStep(increment);
        postDelayed(mChangeCurrentByOneFromLongPressCommand, delayMillis);
    }

    /**
     * Removes the command for changing the current value by one.
     */
    private void removeChangeCurrentByOneFromLongPress() {
        if (mChangeCurrentByOneFromLongPressCommand != null) {
            removeCallbacks(mChangeCurrentByOneFromLongPressCommand);
        }
    }

    /**
     * Removes all pending callback from the message queue.
     */
    private void removeAllCallbacks() {
        if (mChangeCurrentByOneFromLongPressCommand != null) {
            removeCallbacks(mChangeCurrentByOneFromLongPressCommand);
        }
        if (mSetSelectionCommand != null) {
            removeCallbacks(mSetSelectionCommand);
        }
    }

    /**
     * @return The selected index given its displayed <code>value</code>.
     */
    private int getSelectedPos(String value) {
        if (mDisplayedValues == null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // Ignore as if it's not a number we don't care
            }
        } else {
            for (int i = 0; i < mDisplayedValues.length; i++) {
                // Don't force the user to type in jan when ja will do
                value = value.toLowerCase();
                if (mDisplayedValues[i].toLowerCase().startsWith(value)) {
                    return mMinValue + i;
                }
            }

            /*
             * The user might have typed in a number into the month field i.e.
             * 10 instead of OCT so support that too.
             */
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // Ignore as if it's not a number we don't care
            }
        }
        return mMinValue;
    }

    /**
     * Posts an {@link SetSelectionCommand} from the given <code>selectionStart
     * </code> to <code>selectionEnd</code>.
     */
    private void postSetSelectionCommand(int selectionStart, int selectionEnd) {
        if (mSetSelectionCommand == null) {
            mSetSelectionCommand = new SetSelectionCommand();
        } else {
            removeCallbacks(mSetSelectionCommand);
        }
        mSetSelectionCommand.mSelectionStart = selectionStart;
        mSetSelectionCommand.mSelectionEnd = selectionEnd;
        post(mSetSelectionCommand);
    }

    /**
     * The numbers accepted by the input text's {@link Filter}
     */
    private static final char[] DIGIT_CHARACTERS = new char[] {
        // Latin digits are the common case
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        // Arabic-Indic
        '\u0660', '\u0661', '\u0662', '\u0663', '\u0664',
        '\u0665', '\u0666', '\u0667', '\u0668', '\u0669',
        // Extended Arabic-Indic
        '\u06f0', '\u06f1', '\u06f2', '\u06f3', '\u06f4',
        '\u06f5', '\u06f6', '\u06f7', '\u06f8', '\u06f9'
    };

    /**
     * Filter for accepting only valid indices or prefixes of the string
     * representation of valid indices.
     */
    class InputTextFilter extends NumberKeyListener {

        // XXX This doesn't allow for range limits when controlled by a soft input method!
        public int getInputType() {
            return InputType.TYPE_CLASS_TEXT;
        }

        @Override
        protected char[] getAcceptedChars() {
            return DIGIT_CHARACTERS;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (mDisplayedValues == null) {
                CharSequence filtered = super.filter(source, start, end, dest, dstart, dend);
                if (filtered == null) {
                    filtered = source.subSequence(start, end);
                }

                String result = String.valueOf(dest.subSequence(0, dstart)) + filtered
                    + dest.subSequence(dend, dest.length());

                if ("".equals(result)) {
                    return result;
                }
                int val = getSelectedPos(result);

                /*
                 * Ensure the user can't type in a value greater than the max
                 * allowed. We have to allow less than min as the user might
                 * want to delete some numbers and then type a new number.
                 */
                if (val > mMaxValue) {
                    return "";
                } else {
                    return filtered;
                }
            } else {
                CharSequence filtered = String.valueOf(source.subSequence(start, end));
                if (TextUtils.isEmpty(filtered)) {
                    return "";
                }
                String result = String.valueOf(dest.subSequence(0, dstart)) + filtered
                    + dest.subSequence(dend, dest.length());
                String str = String.valueOf(result).toLowerCase();
                for (String val : mDisplayedValues) {
                    String valLowerCase = val.toLowerCase();
                    if (valLowerCase.startsWith(str)) {
                        postSetSelectionCommand(result.length(), val.length());
                        return val.subSequence(dstart, val.length());
                    }
                }
                return "";
            }
        }
    }

    /**
     * Ensures that the scroll wheel is adjusted i.e. there is no offset and the
     * middle element is in the middle of the widget.
     *
     * @return Whether an adjustment has been made.
     */
    private boolean ensureScrollWheelAdjusted() {
        // adjust to the closest value
        int delta = mInitialScrollOffset - mCurrentScrollOffset;
        if (delta != 0) {
            if (Math.abs(delta) > mSelectorElementSize / 2) {
                delta += (delta > 0) ? -mSelectorElementSize : mSelectorElementSize;
            }
            if (isHorizontalMode()) {
                mPreviousScrollerX = 0;
                mAdjustScroller.startScroll(0, 0, delta, 0, SELECTOR_ADJUSTMENT_DURATION_MILLIS);
            } else {
                mPreviousScrollerY = 0;
                mAdjustScroller.startScroll(0, 0, 0, delta, SELECTOR_ADJUSTMENT_DURATION_MILLIS);
            }
            invalidate();
            return true;
        }
        return false;
    }

    /**
     * Command for setting the input text selection.
     */
    class SetSelectionCommand implements Runnable {
        private int mSelectionStart;

        private int mSelectionEnd;

        public void run() {
            mInputText.setSelection(mSelectionStart, mSelectionEnd);
        }
    }

    /**
     * Command for changing the current value from a long press by one.
     */
    class ChangeCurrentByOneFromLongPressCommand implements Runnable {
        private boolean mIncrement;

        private void setStep(boolean increment) {
            mIncrement = increment;
        }

        @Override
        public void run() {
            changeValueByOne(mIncrement);
            postDelayed(this, mLongPressUpdateInterval);
        }
    }

    private SupportAccessibilityNodeProvider getSupportAccessibilityNodeProvider() {
        return new SupportAccessibilityNodeProvider();
    }

    class SupportAccessibilityNodeProvider {

        AccessibilityNodeProviderImpl mProvider;

        private SupportAccessibilityNodeProvider() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mProvider = new AccessibilityNodeProviderImpl();
            }
        }

        public boolean performAction(int virtualViewId, int action, Bundle arguments) {
            if (mProvider != null) {
                return mProvider.performAction(virtualViewId, action, arguments);
            }

            return false;
        }

        public void sendAccessibilityEventForVirtualView(int virtualViewId, int eventType) {
            if (mProvider != null) mProvider.sendAccessibilityEventForVirtualView(virtualViewId, eventType);
        }
    }

    /**
     * Class for managing virtual view tree rooted at this picker.
     */
    class AccessibilityNodeProviderImpl extends AccessibilityNodeProvider {
        private static final int UNDEFINED = Integer.MIN_VALUE;

        private static final int VIRTUAL_VIEW_ID_INCREMENT = 1;

        private static final int VIRTUAL_VIEW_ID_INPUT = 2;

        private static final int VIRTUAL_VIEW_ID_DECREMENT = 3;

        private final Rect mTempRect = new Rect();

        private final int[] mTempArray = new int[2];

        private int mAccessibilityFocusedView = UNDEFINED;

        @Override
        public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
            switch (virtualViewId) {
                case View.NO_ID:
                    return createAccessibilityNodeInfoForNumberPicker(getScrollX(), getScrollY(),
                        getScrollX() + (getRight() - getLeft()), getScrollY() + (getBottom() - getTop()));
                case VIRTUAL_VIEW_ID_DECREMENT:
                    if (isHorizontalMode()) {
                        return createAccessibilityNodeInfoForVirtualButton(VIRTUAL_VIEW_ID_DECREMENT,
                            getVirtualDecrementButtonText(), getScrollX(), getScrollY(),
                            mLeftOfSelectionDividerLeft + mSelectionDividerThickness,
                            getScrollY() + (getBottom() - getBottom()));
                    } else {
                        return createAccessibilityNodeInfoForVirtualButton(VIRTUAL_VIEW_ID_DECREMENT,
                            getVirtualDecrementButtonText(), getScrollX(), getScrollY(),
                            getScrollX() + (getRight() - getLeft()),
                            mTopSelectionDividerTop + mSelectionDividerThickness);
                    }
                case VIRTUAL_VIEW_ID_INPUT:
                    return createAccessibiltyNodeInfoForInputText();
                case VIRTUAL_VIEW_ID_INCREMENT:
                    if (isHorizontalMode()) {
                        return createAccessibilityNodeInfoForVirtualButton(VIRTUAL_VIEW_ID_INCREMENT,
                            getVirtualIncrementButtonText(),
                            mRightOfSelectionDividerRight - mSelectionDividerThickness, getScrollY(),
                            getScrollX() + (getRight() - getLeft()), getScrollY() + (getBottom() - getTop()));
                    } else {
                        return createAccessibilityNodeInfoForVirtualButton(VIRTUAL_VIEW_ID_INCREMENT,
                            getVirtualIncrementButtonText(), getScrollX(),
                            mBottomSelectionDividerBottom - mSelectionDividerThickness,
                            getScrollX() + (getRight() - getLeft()), getScrollY() + (getBottom() - getTop()));
                    }
            }
            return super.createAccessibilityNodeInfo(virtualViewId);
        }

        @Override
        public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(String searched, int virtualViewId) {
            if (TextUtils.isEmpty(searched)) {
                return Collections.emptyList();
            }
            String searchedLowerCase = searched.toLowerCase();
            List<AccessibilityNodeInfo> result = new ArrayList<>();
            switch (virtualViewId) {
                case View.NO_ID: {
                    findAccessibilityNodeInfosByTextInChild(searchedLowerCase, VIRTUAL_VIEW_ID_DECREMENT, result);
                    findAccessibilityNodeInfosByTextInChild(searchedLowerCase, VIRTUAL_VIEW_ID_INPUT, result);
                    findAccessibilityNodeInfosByTextInChild(searchedLowerCase, VIRTUAL_VIEW_ID_INCREMENT, result);
                    return result;
                }
                case VIRTUAL_VIEW_ID_DECREMENT:
                case VIRTUAL_VIEW_ID_INCREMENT:
                case VIRTUAL_VIEW_ID_INPUT: {
                    findAccessibilityNodeInfosByTextInChild(searchedLowerCase, virtualViewId, result);
                    return result;
                }
            }
            return super.findAccessibilityNodeInfosByText(searched, virtualViewId);
        }

        @Override
        public boolean performAction(int virtualViewId, int action, Bundle arguments) {
            switch (virtualViewId) {
                case View.NO_ID: {
                    switch (action) {
                        case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView != virtualViewId) {
                                mAccessibilityFocusedView = virtualViewId;
                                // requestAccessibilityFocus();
                                performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
                                return true;
                            }
                        } return false;
                        case AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView == virtualViewId) {
                                mAccessibilityFocusedView = UNDEFINED;
                                // clearAccessibilityFocus();
                                performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null);
                                return true;
                            }
                            return false;
                        }
                        case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD: {
                            if (NumberPicker.this.isEnabled() && (getWrapSelectorWheel() || getValue() < getMaxValue())) {
                                changeValueByOne(true);
                                return true;
                            }
                        } return false;
                        case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD: {
                            if (NumberPicker.this.isEnabled() && (getWrapSelectorWheel() || getValue() > getMinValue())) {
                                changeValueByOne(false);
                                return true;
                            }
                        } return false;
                    }
                } break;
                case VIRTUAL_VIEW_ID_INPUT: {
                    switch (action) {
                        case AccessibilityNodeInfo.ACTION_FOCUS: {
                            if (NumberPicker.this.isEnabled() && !mInputText.isFocused()) {
                                return mInputText.requestFocus();
                            }
                        } break;
                        case AccessibilityNodeInfo.ACTION_CLEAR_FOCUS: {
                            if (NumberPicker.this.isEnabled() && mInputText.isFocused()) {
                                mInputText.clearFocus();
                                return true;
                            }
                            return false;
                        }
                        case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView != virtualViewId) {
                                mAccessibilityFocusedView = virtualViewId;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                                mInputText.invalidate();
                                return true;
                            }
                        } return false;
                        case  AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView == virtualViewId) {
                                mAccessibilityFocusedView = UNDEFINED;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                                mInputText.invalidate();
                                return true;
                            }
                        } return false;
                        default: {
                            return mInputText.performAccessibilityAction(action, arguments);
                        }
                    }
                } return false;
                case VIRTUAL_VIEW_ID_INCREMENT: {
                    switch (action) {
                        case AccessibilityNodeInfo.ACTION_CLICK: {
                            if (NumberPicker.this.isEnabled()) {
                                NumberPicker.this.changeValueByOne(true);
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
                                return true;
                            }
                        } return false;
                        case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView != virtualViewId) {
                                mAccessibilityFocusedView = virtualViewId;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                                if (isHorizontalMode()) {
                                    invalidate(mRightOfSelectionDividerRight, 0, getRight(), getBottom());
                                } else {
                                    invalidate(0, mBottomSelectionDividerBottom, getRight(), getBottom());
                                }
                                return true;
                            }
                        } return false;
                        case  AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView == virtualViewId) {
                                mAccessibilityFocusedView = UNDEFINED;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                                if (isHorizontalMode()) {
                                    invalidate(mRightOfSelectionDividerRight, 0, getRight(), getBottom());
                                } else {
                                    invalidate(0, mBottomSelectionDividerBottom, getRight(), getBottom());
                                }
                                return true;
                            }
                        } return false;
                    }
                } return false;
                case VIRTUAL_VIEW_ID_DECREMENT: {
                    switch (action) {
                        case AccessibilityNodeInfo.ACTION_CLICK: {
                            if (NumberPicker.this.isEnabled()) {
                                final boolean increment = (virtualViewId == VIRTUAL_VIEW_ID_INCREMENT);
                                NumberPicker.this.changeValueByOne(increment);
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
                                return true;
                            }
                        } return false;
                        case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView != virtualViewId) {
                                mAccessibilityFocusedView = virtualViewId;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                                if (isHorizontalMode()) {
                                    invalidate(0, 0, mLeftOfSelectionDividerLeft, getBottom());
                                } else {
                                    invalidate(0, 0, getRight(), mTopSelectionDividerTop);
                                }
                                return true;
                            }
                        } return false;
                        case  AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView == virtualViewId) {
                                mAccessibilityFocusedView = UNDEFINED;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                                if (isHorizontalMode()) {
                                    invalidate(0, 0, mLeftOfSelectionDividerLeft, getBottom());
                                } else {
                                    invalidate(0, 0, getRight(), mTopSelectionDividerTop);
                                }
                                return true;
                            }
                        } return false;
                    }
                } return false;
            }
            return super.performAction(virtualViewId, action, arguments);
        }

        public void sendAccessibilityEventForVirtualView(int virtualViewId, int eventType) {
            switch (virtualViewId) {
            case VIRTUAL_VIEW_ID_DECREMENT: {
                if (hasVirtualDecrementButton()) {
                    sendAccessibilityEventForVirtualButton(virtualViewId, eventType, getVirtualDecrementButtonText());
                }
                break;
            }
            case VIRTUAL_VIEW_ID_INPUT: {
                sendAccessibilityEventForVirtualText(eventType);
                break;
            }
            case VIRTUAL_VIEW_ID_INCREMENT: {
                if (hasVirtualIncrementButton()) {
                    sendAccessibilityEventForVirtualButton(virtualViewId, eventType, getVirtualIncrementButtonText());
                }
                break;
            }
            }
        }

        private void sendAccessibilityEventForVirtualText(int eventType) {
            if (((AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE)).isEnabled()) {
                AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
                mInputText.onInitializeAccessibilityEvent(event);
                mInputText.onPopulateAccessibilityEvent(event);
                event.setSource(NumberPicker.this, VIRTUAL_VIEW_ID_INPUT);
                requestSendAccessibilityEvent(NumberPicker.this, event);
            }
        }

        private void sendAccessibilityEventForVirtualButton(int virtualViewId, int eventType, String text) {
            if (((AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE)).isEnabled()) {
                AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
                event.setClassName(Button.class.getName());
                event.setPackageName(getContext().getPackageName());
                event.getText().add(text);
                event.setEnabled(NumberPicker.this.isEnabled());
                event.setSource(NumberPicker.this, virtualViewId);
                requestSendAccessibilityEvent(NumberPicker.this, event);
            }
        }

        private void findAccessibilityNodeInfosByTextInChild(String searchedLowerCase, int virtualViewId, List<AccessibilityNodeInfo> outResult) {
            switch (virtualViewId) {
            case VIRTUAL_VIEW_ID_DECREMENT: {
                String text = getVirtualDecrementButtonText();
                if (!TextUtils.isEmpty(text) && text.toString().toLowerCase().contains(searchedLowerCase)) {
                    outResult.add(createAccessibilityNodeInfo(VIRTUAL_VIEW_ID_DECREMENT));
                }
                break;
            }
            case VIRTUAL_VIEW_ID_INPUT: {
                CharSequence text = mInputText.getText();
                if (!TextUtils.isEmpty(text) && text.toString().toLowerCase().contains(searchedLowerCase)) {
                    outResult.add(createAccessibilityNodeInfo(VIRTUAL_VIEW_ID_INPUT));
                    break;
                }
                CharSequence contentDesc = mInputText.getText();
                if (!TextUtils.isEmpty(contentDesc) && contentDesc.toString().toLowerCase().contains(searchedLowerCase)) {
                    outResult.add(createAccessibilityNodeInfo(VIRTUAL_VIEW_ID_INPUT));
                    break;
                }
            }
            case VIRTUAL_VIEW_ID_INCREMENT: {
                String text = getVirtualIncrementButtonText();
                if (!TextUtils.isEmpty(text) && text.toString().toLowerCase().contains(searchedLowerCase)) {
                    outResult.add(createAccessibilityNodeInfo(VIRTUAL_VIEW_ID_INCREMENT));
                }
                break;
            }
            }
        }

        private AccessibilityNodeInfo createAccessibiltyNodeInfoForInputText() {
            AccessibilityNodeInfo info = mInputText.createAccessibilityNodeInfo();
            info.setSource(NumberPicker.this, VIRTUAL_VIEW_ID_INPUT);
            if (mAccessibilityFocusedView != VIRTUAL_VIEW_ID_INPUT) {
                info.addAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            }
            if (mAccessibilityFocusedView == VIRTUAL_VIEW_ID_INPUT) {
                info.addAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
            }
            return info;
        }

        private AccessibilityNodeInfo createAccessibilityNodeInfoForVirtualButton(int virtualViewId,
            String text, int left, int top, int right, int bottom) {
            AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
            info.setClassName(Button.class.getName());
            info.setPackageName(getContext().getPackageName());
            info.setSource(NumberPicker.this, virtualViewId);
            info.setParent(NumberPicker.this);
            info.setText(text);
            info.setClickable(true);
            info.setLongClickable(true);
            info.setEnabled(NumberPicker.this.isEnabled());
            Rect boundsInParent = mTempRect;
            boundsInParent.set(left, top, right, bottom);
            // TODO info.setVisibleToUser(isVisibleToUser(boundsInParent));
            info.setBoundsInParent(boundsInParent);
            Rect boundsInScreen = boundsInParent;
            int[] locationOnScreen = mTempArray;
            getLocationOnScreen(locationOnScreen);
            boundsInScreen.offset(locationOnScreen[0], locationOnScreen[1]);
            info.setBoundsInScreen(boundsInScreen);

            if (mAccessibilityFocusedView != virtualViewId) {
                info.addAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            }
            if (mAccessibilityFocusedView == virtualViewId) {
                info.addAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
            }
            if (NumberPicker.this.isEnabled()) {
                info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
            }

            return info;
        }

        private AccessibilityNodeInfo createAccessibilityNodeInfoForNumberPicker(int left, int top, int right, int bottom) {
            AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
            info.setClassName(NumberPicker.class.getName());
            info.setPackageName(getContext().getPackageName());
            info.setSource(NumberPicker.this);

            if (hasVirtualDecrementButton()) {
                info.addChild(NumberPicker.this, VIRTUAL_VIEW_ID_DECREMENT);
            }
            info.addChild(NumberPicker.this, VIRTUAL_VIEW_ID_INPUT);
            if (hasVirtualIncrementButton()) {
                info.addChild(NumberPicker.this, VIRTUAL_VIEW_ID_INCREMENT);
            }

            info.setParent((View) getParentForAccessibility());
            info.setEnabled(NumberPicker.this.isEnabled());
            info.setScrollable(true);

            /** TODO: Figure out compat implementation for this
            final float applicationScale =
                    getContext().getResources().getCompatibilityInfo().applicationScale;

            Rect boundsInParent = mTempRect;
            boundsInParent.set(left, top, right, bottom);
            boundsInParent.scale(applicationScale);
            info.setBoundsInParent(boundsInParent);

            info.setVisibleToUser(isVisibleToUser());

            Rect boundsInScreen = boundsInParent;
            int[] locationOnScreen = mTempArray;
            getLocationOnScreen(locationOnScreen);
            boundsInScreen.offset(locationOnScreen[0], locationOnScreen[1]);
            boundsInScreen.scale(applicationScale);
            info.setBoundsInScreen(boundsInScreen);
             */

            if (mAccessibilityFocusedView != View.NO_ID) {
                info.addAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            }
            if (mAccessibilityFocusedView == View.NO_ID) {
                info.addAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
            }
            if (NumberPicker.this.isEnabled()) {
                if (getWrapSelectorWheel() || getValue() < getMaxValue()) {
                    info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                }
                if (getWrapSelectorWheel() || getValue() > getMinValue()) {
                    info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                }
            }

            return info;
        }

        private boolean hasVirtualDecrementButton() {
            return getWrapSelectorWheel() || getValue() > getMinValue();
        }

        private boolean hasVirtualIncrementButton() {
            return getWrapSelectorWheel() || getValue() < getMaxValue();
        }

        private String getVirtualDecrementButtonText() {
            int value = mValue - 1;
            if (mWrapSelectorWheel) {
                value = getWrappedSelectorIndex(value);
            }
            if (value >= mMinValue) {
                return (mDisplayedValues == null) ? formatNumber(value)
                    : mDisplayedValues[value - mMinValue];
            }
            return null;
        }

        private String getVirtualIncrementButtonText() {
            int value = mValue + 1;
            if (mWrapSelectorWheel) {
                value = getWrappedSelectorIndex(value);
            }
            if (value <= mMaxValue) {
                return (mDisplayedValues == null) ? formatNumber(value)
                    : mDisplayedValues[value - mMinValue];
            }
            return null;
        }
    }

    private String formatNumberWithLocale(int value) {
        return String.format(Locale.getDefault(), "%d", value);
    }

    public void setDividerColor(@ColorInt int color) {
        mSelectionDividerColor = color;
        mSelectionDivider = new ColorDrawable(color);
    }

    public void setDividerColorResource(@ColorRes int colorId) {
        setDividerColor(getResources().getColor(colorId));
    }

    public void setWheelItemCount(final int count) {
        mWheelItemCount = count;
        mWheelMiddleItemIndex = mWheelItemCount / 2;
        mSelectorIndices = new int[mWheelItemCount];
    }

    public void setFormatter(final String formatter) {
        if (TextUtils.isEmpty(formatter)) {
            return;
        }

        setFormatter(stringToFormatter(formatter));
    }

    public void setFormatter(@StringRes int stringId) {
        setFormatter(getResources().getString(stringId));
    }

    public void setTextColor(@ColorInt int color) {
        mTextColor = color;
        mInputText.setTextColor(color);
        mSelectorWheelPaint.setColor(color);
    }

    public void setTextColorResource(@ColorRes int colorId) {
        setTextColor(getResources().getColor(colorId));
    }

    public void setTextSize(float textSize) {
        mTextSize = textSize;
        mInputText.setTextSize(pxToSp(mTextSize));
        mSelectorWheelPaint.setTextSize(mTextSize);
    }

    public void setTextSize(@DimenRes int dimenId) {
        setTextSize(getResources().getDimension(dimenId));
    }

    public void setTypeface(Typeface typeface) {
        mTypeface = typeface;
        if (mTypeface != null) {
            mInputText.setTypeface(mTypeface);
            mSelectorWheelPaint.setTypeface(mTypeface);
        } else {
            mInputText.setTypeface(Typeface.MONOSPACE);
            mSelectorWheelPaint.setTypeface(Typeface.MONOSPACE);
        }
    }

    public void setTypeface(String string, int style) {
        if (TextUtils.isEmpty(string)) {
            return;
        }
        setTypeface(Typeface.create(string, style));
    }

    public void setTypeface(String string) {
        setTypeface(string, Typeface.NORMAL);
    }

    public void setTypeface(@StringRes int stringId, int style) {
        setTypeface(getResources().getString(stringId), style);
    }

    public void setTypeface(@StringRes int stringId) {
        setTypeface(stringId, Typeface.NORMAL);
    }

    private Formatter stringToFormatter(final String formatter) {
        if (TextUtils.isEmpty(formatter)) {
            return null;
        }

        return new Formatter() {
            @Override
            public String format(int i) {
                return String.format(Locale.getDefault(), formatter, i);
            }
        };
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private float pxToSp(float px) {
        return px / getResources().getDisplayMetrics().scaledDensity;
    }

    private boolean isHorizontalMode() {
        return mOrientation == HORIZONTAL;
    }

}
