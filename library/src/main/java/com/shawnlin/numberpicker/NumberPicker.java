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

    private int mDividerColor;

    private String mFormatter;

    private boolean mFocusable = false;

    private int mMax = 100;

    private int mMin = 1;

    private int mTextColor = 0xFF000000;

    private float mTextSize = 25f;

    private Typeface mTypeface;

    public NumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.NumberPicker);
        initAttributes(typedArray);
        typedArray.recycle();
    }

    private void initAttributes(TypedArray typedArray) {
        mDividerColor = typedArray.getColor(R.styleable.NumberPicker_np_dividerColor, mDividerColor);
        mFormatter = typedArray.getString(R.styleable.NumberPicker_np_formatter);
        mFocusable = typedArray.getBoolean(R.styleable.NumberPicker_np_focusable, mFocusable);
        mMax = typedArray.getInt(R.styleable.NumberPicker_np_max, mMax);
        mMin = typedArray.getInt(R.styleable.NumberPicker_np_min, mMin);
        mTextColor = typedArray.getColor(R.styleable.NumberPicker_np_textColor, mTextColor);
        mTextSize = typedArray.getDimension(R.styleable.NumberPicker_np_textSize, mTextSize);
        mTypeface = Typeface.create(typedArray.getString(R.styleable.NumberPicker_np_typeface), Typeface.NORMAL);

        setDividerColor(mDividerColor);
        setFormatter(mFormatter);
        setMaxValue(mMax);
        setMinValue(mMin);
        setTextAttributes();
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