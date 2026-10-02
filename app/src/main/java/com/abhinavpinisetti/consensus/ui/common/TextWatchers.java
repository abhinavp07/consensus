package com.abhinavpinisetti.consensus.ui.common;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

public final class TextWatchers {

    private TextWatchers() {}

    /** Clears the layout's error as soon as the user types. */
    public static void clearErrorOnEdit(TextInputLayout layout) {
        EditText edit = layout.getEditText();
        if (edit == null) return;
        edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    public static String text(TextInputLayout layout) {
        EditText edit = layout.getEditText();
        return edit == null || edit.getText() == null ? "" : edit.getText().toString().trim();
    }
}
