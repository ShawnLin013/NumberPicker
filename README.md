# Number Picker

The android library that provides a simple and customizable NumberPicker.
It's based on [android.widget.NumberPicker](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/widget/NumberPicker.java).

[![Platform](http://img.shields.io/badge/platform-android-brightgreen.svg?style=flat)](http://developer.android.com/index.html) [![Language](http://img.shields.io/badge/language-java-orange.svg?style=flat)](http://www.oracle.com/technetwork/java/javase/downloads/index.html) [![](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT) [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-NumberPicker-green.svg?style=true)](https://android-arsenal.com/details/1/3718) [![API](https://img.shields.io/badge/API-15%2B-blue.svg?style=flat)](https://android-arsenal.com/api?level=15) [![Download](https://api.bintray.com/packages/shawnlin013/maven/number-picker/images/download.svg)](https://bintray.com/shawnlin013/maven/number-picker/_latestVersion) [![CircleCI](https://circleci.com/gh/ShawnLin013/NumberPicker.svg?style=svg)](https://circleci.com/gh/ShawnLin013/NumberPicker)

<img src="https://github.com/ShawnLin013/NumberPicker/blob/master/screenshot/number-picker-theme.png">

## Features

- Customizable fonts(color, size, strikethrough, underline, typeface)
- Customizable dividers(color, distance, thickness)
- Horizontal and Vertical mode are both supported
- Ascending and Descending order are both supported
- Also supports negative values and multiple lines

## Usage

#### Java

```java
NumberPicker numberPicker = (NumberPicker) findViewById(R.id.number_picker);

// Set divider color
numberPicker.setDividerColor(ContextCompat.getColor(this, R.color.colorPrimary));
numberPicker.setDividerColorResource(R.color.colorPrimary);

// Set formatter
numberPicker.setFormatter(getString(R.string.number_picker_formatter));
numberPicker.setFormatter(R.string.number_picker_formatter);

// Set selected text color
numberPicker.setSelectedTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
numberPicker.setSelectedTextColorResource(R.color.colorPrimary);

// Set selected text size
numberPicker.setSelectedTextSize(getResources().getDimension(R.dimen.selected_text_size));
numberPicker.setSelectedTextSize(R.dimen.selected_text_size);

// Set text color
numberPicker.setTextColor(ContextCompat.getColor(this, R.color.dark_grey));
numberPicker.setTextColorResource(R.color.dark_grey);

// Set text size
numberPicker.setTextSize(getResources().getDimension(R.dimen.text_size));
numberPicker.setTextSize(R.dimen.text_size);

// Set typeface
numberPicker.setTypeface(Typeface.create(getString(R.string.roboto_light), Typeface.NORMAL));
numberPicker.setTypeface(getString(R.string.roboto_light), Typeface.NORMAL);
numberPicker.setTypeface(getString(R.string.roboto_light));
numberPicker.setTypeface(R.string.roboto_light, Typeface.NORMAL);
numberPicker.setTypeface(R.string.roboto_light);

// Set value
numberPicker.setMaxValue(59);
numberPicker.setMinValue(0);
numberPicker.setValue(3);

// Using string values
// IMPORTANT! setMinValue to 1 and call setDisplayedValues after setMinValue and setMaxValue
String[] data = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};
numberPicker.setMinValue(1);
numberPicker.setMaxValue(data.length);
numberPicker.setDisplayedValues(data);
numberPicker.setValue(7);

// Set fading edge enabled
numberPicker.setFadingEdgeEnabled(true);

// Set scroller enabled
numberPicker.setScrollerEnabled(true);

// Set wrap selector wheel
numberPicker.setWrapSelectorWheel(true);

// OnClickListener
numberPicker.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        Log.d(TAG, "Click on current value");
    }
});

// OnValueChangeListener
numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        Log.d(TAG, String.format(Locale.US, "oldVal: %d, newVal: %d", oldVal, newVal));
    }
});
```

#### XML

add `xmlns:app="http://schemas.android.com/apk/res-auto"`

```xml
<com.shawnlin.numberpicker.NumberPicker
    android:id="@+id/number_picker"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_centerInParent="true"
    app:np_width="64dp"
    app:np_height="180dp"
    app:np_dividerColor="@color/colorPrimary"
    app:np_formatter="@string/number_picker_formatter"
    app:np_max="59"
    app:np_min="0"
    app:np_selectedTextColor="@color/colorPrimary"
    app:np_selectedTextSize="@dimen/selected_text_size"
    app:np_textColor="@color/colorPrimary"
    app:np_textSize="@dimen/text_size"
    app:np_typeface="@string/roboto_light"
    app:np_value="3" />
```

### Attributes

|attribute name|attribute description|
|:---:|:---:|
|np_width|The width of this widget.|
|np_height|The height of this widget.|
|np_dividerColor|The color of the selection divider.|
|np_dividerDistance|The distance between the two selection dividers.|
|np_dividerThickness|The thickness of the selection divider.|
|np_fadingEdgeEnabled|Flag whether the fading edge should enabled.|
|np_fadingEdgeStrength|The strength of fading edge while drawing the selector.|
|np_formatter|The formatter of the numbers.|
|np_hideWheelUntilFocused|Flag whether the selector wheel should hidden until the picker has focus.|
|np_lineSpacingMultiplier|The line spacing multiplier for the multiple lines.|
|np_max|The max value of this widget.|
|np_maxFlingVelocityCoefficient|The coefficient to adjust (divide) the max fling velocity.|
|np_min|The min value of this widget.|
|np_order|The order of this widget. Default is ascending.|
|np_orientation|The orientation of this widget. Default is vertical.|
|np_scrollerEnabled|Flag whether the scroller should enabled.|
|np_selectedTextAlign|The text align of the selected number. Default is center.|
|np_selectedTextColor|The text color of the selected number.|
|np_selectedTextSize|The text size of the selected number.|
|np_selectedTextStrikeThru|Flag whether the selected text should strikethroughed.|
|np_selectedTextUnderline|Flag whether the selected text should underlined.|
|np_textAlign|The text align of the numbers. Default is center.|
|np_textColor|The text color of the numbers.|
|np_textSize|The text size of the numbers.|
|np_textStrikeThru|Flag whether the text should strikethroughed.|
|np_textUnderline|Flag whether the text should underlined.|
|np_typeface|The typeface of the numbers.|
|np_value|The current value of this widget.|
|np_wheelItemCount|The number of items show in the selector wheel.|
|np_wrapSelectorWheel|Flag whether the selector should wrap around.|

## Gradle

Add the dependency in your `build.gradle`

```gradle
buildscript {
    repositories {
        jcenter()
    }
}

dependencies {
    implementation 'com.shawnlin:number-picker:2.4.8'
}
```

## Support

Thank you to all our backers! 🙏

<a href='https://ko-fi.com/shawnlin' target='_blank'><img height='48' style='border:0px;height:48px;' src='https://az743702.vo.msecnd.net/cdn/kofi3.png?v=0' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a>

## License

The source code is licensed under the [MIT](LICENSE) license.
