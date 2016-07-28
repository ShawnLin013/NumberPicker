package com.shawnlin.numberpicker;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import java.lang.reflect.Field;
import java.util.Locale;

public class NumberPicker extends android.widget.NumberPicker {

    private static final float DEFAULT_WIDTH = 64;

    private static final float DEFAULT_HEIGHT = 180;

    private int mDividerColor;

    private String mFormatter;

    private boolean mFocusable = false;

    private boolean mIsHorizontal = false;

    private int mMax = 100;

    private int mMin = 1;

    private int mTextColor = 0xFF000000;

    private float mTextSize = 25f;

    private Typeface mTypeface;

    private float mWidth;

    private float mHeight;

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
        this(context, attrs, 0);
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
        mDividerColor = attributesArray.getColor(R.styleable.NumberPicker_np_dividerColor, mDividerColor);
        mFormatter = attributesArray.getString(R.styleable.NumberPicker_np_formatter);
        mFocusable = attributesArray.getBoolean(R.styleable.NumberPicker_np_focusable, mFocusable);
        mIsHorizontal = attributesArray.getBoolean(R.styleable.NumberPicker_np_horizontal, mIsHorizontal);
        mMax = attributesArray.getInt(R.styleable.NumberPicker_np_max, mMax);
        mMin = attributesArray.getInt(R.styleable.NumberPicker_np_min, mMin);
        mTextColor = attributesArray.getColor(R.styleable.NumberPicker_np_textColor, mTextColor);
        mTextSize = attributesArray.getDimension(R.styleable.NumberPicker_np_textSize, mTextSize);
        mTypeface = Typeface.create(attributesArray.getString(R.styleable.NumberPicker_np_typeface), Typeface.NORMAL);
        mWidth = attributesArray.getFloat(R.styleable.NumberPicker_np_width, 0);
        mHeight = attributesArray.getFloat(R.styleable.NumberPicker_np_height, 0);
        attributesArray.recycle();

        if (mIsHorizontal) {
            setOrientation(HORIZONTAL);
        }
        setDividerColor(mDividerColor);
        setFormatter(mFormatter);
        setRotation(mIsHorizontal ? 270 : 0);
        setMaxValue(mMax);
        setMinValue(mMin);
        setTextAttributes();

        if (mWidth != 0 && mHeight != 0) {
            setScaleX(mWidth / DEFAULT_WIDTH);
            setScaleY(mHeight / DEFAULT_HEIGHT);
        } else if (mWidth != 0) {
            setScaleX(mWidth / DEFAULT_WIDTH);
            setScaleY(mWidth / DEFAULT_WIDTH);
        } else if (mHeight != 0) {
            setScaleX(mHeight / DEFAULT_HEIGHT);
            setScaleY(mHeight / DEFAULT_HEIGHT);
        }
    }

    public void setDividerColor(@ColorInt int color) {
        if (color == 0) {
            return;
        }

        mDividerColor = color;
        Field[] fields = android.widget.NumberPicker.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals("mSelectionDivider")) {
                field.setAccessible(true);
                try {
                    field.set(this, new ColorDrawable(color));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (Resources.NotFoundException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    public void setDividerColorResource(@ColorRes int colorId) {
        setDividerColor(getResources().getColor(colorId));
    }

    public void setFormatter(String formatter) {
        if (TextUtils.isEmpty(formatter)) {
            return;
        }

        mFormatter = formatter;
        setFormatter(new Formatter() {
            @Override public String format(int i) {
                return String.format(Locale.getDefault(), mFormatter, i);
            }
        });
    }

    public void setFormatter(@StringRes int stringId) {
        setFormatter(getResources().getString(stringId));
    }

    public void setFocusable(boolean focusable) {
        mFocusable = focusable;
        setTextAttributes();
    }

    public void setTextColor(@ColorInt int color) {
        mTextColor = color;
        setTextAttributes();
    }

    public void setTextColorResource(@ColorRes int colorId) {
        setTextColor(getResources().getColor(colorId));
    }

    public void setTextSize(float textSize) {
        mTextSize = textSize;
        setTextAttributes();
    }

    public void setTextSize(@DimenRes int dimenId) {
        setTextSize(getResources().getDimension(dimenId));
    }

    public void setTypeface(Typeface typeface) {
        mTypeface = typeface;
        setTextAttributes();
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

    private void setTextAttributes() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof EditText) {
                try {
                    Field selectorWheelPaintField = android.widget.NumberPicker.class.getDeclaredField("mSelectorWheelPaint");
                    selectorWheelPaintField.setAccessible(true);

                    Paint wheelPaint = ((Paint) selectorWheelPaintField.get(this));
                    wheelPaint.setColor(mTextColor);
                    wheelPaint.setTextSize(mTextSize);
                    wheelPaint.setTypeface(mTypeface != null ? mTypeface : Typeface.MONOSPACE);

                    EditText editText = ((EditText) child);
                    editText.setTextColor(mTextColor);
                    editText.setFocusable(mFocusable);
                    editText.setTextSize(pxToSp(mTextSize));
                    editText.setTypeface(mTypeface != null ? mTypeface : Typeface.MONOSPACE);
                    invalidate();
                    break;
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private float pxToSp(float px) {
        return px / getResources().getDisplayMetrics().scaledDensity;
    }

}