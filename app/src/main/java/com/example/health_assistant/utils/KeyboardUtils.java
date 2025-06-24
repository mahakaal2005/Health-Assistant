package com.example.health_assistant.utils;

import android.app.Activity;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Utility class to handle keyboard dismissal when clicking outside edit text fields
 */
public class KeyboardUtils {

    /**
     * Sets up touch listener to hide keyboard when touching outside of edit texts
     * @param activity The activity containing the view
     * @param view The root view, typically a layout container
     */
    public static void setupUI(Activity activity, View view) {
        // Set up touch listener for non-EditText views to hide keyboard
        if (!(view instanceof EditText) && !(view instanceof TextInputEditText)) {
            view.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    hideKeyboard(activity);
                    // Clear focus from any EditText
                    View currentFocus = activity.getCurrentFocus();
                    if (currentFocus != null) {
                        currentFocus.clearFocus();
                    }
                }
                return false;
            });
        }

        // If a layout container, iterate over children and seed recursion
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View innerView = viewGroup.getChildAt(i);
                setupUI(activity, innerView);
            }
        }
    }

    /**
     * Hides the soft keyboard
     * @param activity The activity where keyboard should be hidden
     */
    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(
                Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}