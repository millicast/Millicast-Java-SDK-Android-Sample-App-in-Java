package com.millicast.android_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.millicast.LayerData;
import com.millicast.VideoRenderer;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static final String TAG = "Utils";
    static SharedPreferences sharedPreferences;
    static int maxLogLen = 1000;

    //**********************************************************************************************
    // Data structure
    //**********************************************************************************************

    /**
     * Given a list of specified size and the current index, gets the next index.
     * If at end of list, cycle to start of the other end.
     * Returns null if none available.
     *
     * @param size      Size of the list.
     * @param now       Current index of the list.
     * @param ascending If true, cycle in the direction of increasing index,
     *                  otherwise cycle in opposite direction.
     * @param logTag
     * @return
     */
    public static Integer indexNext(int size, int now, boolean ascending, String logTag) {
        Integer next = null;
        if (size < 1) {
            logD(TAG, logTag + "Failed. List size was less than 1. Next: " + next + " Now: " + now);
            return null;
        }
        if (ascending) {
            if (now >= (size - 1)) {
                next = 0;
                logD(TAG, logTag + next + " (Cycling back to start)");
            } else {
                next = now + 1;
                logD(TAG, logTag + next + " Incrementing index.");
            }
        } else {
            if (now <= 0) {
                next = size - 1;
                logD(TAG, logTag + next + " (Cycling back to end)");
            } else {
                next = now - 1;
                logD(TAG, logTag + next + " Decrementing index.");
            }
        }

        if (next < 0) {
            logD(TAG, logTag + "Failed. Next is invalid. Next: " + next + " Now: " + now);
            return null;
        }

        logD(TAG, logTag + "Next: " + next + " Now: " + now);
        return next;
    }

    //**********************************************************************************************
    // Logging
    //**********************************************************************************************

    /**
     * logcat truncates strings longer than a certain length.
     * This method breaks the input string into smaller chunks of maxLogSize and print them all.
     *
     * @param TAG
     * @param longString
     */
    public static void logD(String TAG, String longString) {
        for (int i = 0; i < longString.length(); i += maxLogLen) {
            int end = i + maxLogLen;
            end = end > longString.length() ? longString.length() : end;
            Log.d(TAG, longString.substring(i, end));
        }
    }

    //**********************************************************************************************
    // SharedPreferences
    //**********************************************************************************************

    private static SharedPreferences getPref(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
        return sharedPreferences;
    }

    /**
     * Get a String value from the default SharedPreferences, if available.
     * If not, return the specified defaultValue.
     *
     * @param key
     * @param defaultValue
     * @param context
     * @return
     */
    public static String getSaved(String key, String defaultValue, Context context) {
        String value = getPref(context).getString(key, defaultValue);
        String log = "[Utils][String][Get] " + key + ": " + value + ".";
        logD(TAG, log);
        return value;
    }

    /**
     * Get a boolean value from the default SharedPreferences, if available.
     * If not, return the specified defaultValue.
     *
     * @param key
     * @param defaultValue
     * @param context
     * @return
     */
    public static boolean getSaved(String key, boolean defaultValue, Context context) {
        boolean value = getPref(context).getBoolean(key, defaultValue);
        String log = "[Utils][Boolean][Get] " + key + ": " + value + ".";
        logD(TAG, log);
        return value;
    }

    /**
     * Get an integer value from the default SharedPreferences, if available.
     * If not, return the specified defaultValue.
     *
     * @param key
     * @param defaultValue
     * @param context
     * @return
     */
    public static int getSaved(String key, int defaultValue, Context context) {
        int value = getPref(context).getInt(key, defaultValue);
        String log = "[Utils][Int][Get] " + key + ": " + value + ".";
        logD(TAG, log);
        return value;
    }

    /**
     * Set a String value into the default SharedPreferences, using the specified key.
     *
     * @param key
     * @param value
     * @param context
     * @return
     */
    public static void setSaved(String key, String value, Context context) {
        SharedPreferences.Editor editor = getPref(context).edit();
        editor.putString(key, value);
        editor.commit();
        String log = "[Utils][String][Set] " + key + ": " + value + ".";
        logD(TAG, log);
    }

    /**
     * Set a boolean value into the default SharedPreferences, using the specified key.
     *
     * @param key
     * @param value
     * @param context
     * @return
     */
    public static void setSaved(String key, boolean value, Context context) {
        SharedPreferences.Editor editor = getPref(context).edit();
        editor.putBoolean(key, value);
        editor.commit();
        String log = "[Utils][Boolean][Set] " + key + ": " + value + ".";
        logD(TAG, log);
    }

    /**
     * Set an integer value into the default SharedPreferences, using the specified key.
     *
     * @param key
     * @param value
     * @param context
     * @return
     */
    public static void setSaved(String key, int value, Context context) {
        SharedPreferences.Editor editor = getPref(context).edit();
        editor.putInt(key, value);
        editor.commit();
        String log = "[Utils][Int][Set] " + key + ": " + value + ".";
        logD(TAG, log);
    }

    /**
     * Save newValue into SharedPreferences using specified key.
     *
     * @param key
     * @param oldValue The current value of this new value being saved.
     * @param newValue The new value of this value being saved.
     * @param logTag
     * @param context
     */
    public static void saveValue(String key, String oldValue, String newValue, String logTag, Context context) {
        // Set new value into SharePreferences.
        setSaved(key, newValue, context);
        logD(MillicastManager.TAG, logTag + "Now: " + newValue +
                " Was: " + oldValue);
        return;
    }

    /**
     * Save newValue into SharedPreferences using specified key.
     *
     * @param key
     * @param oldValue The current value of this new value being saved.
     * @param newValue The new value of this value being saved.
     * @param logTag
     * @param context
     */
    public static void saveValue(String key, boolean oldValue, boolean newValue, String logTag, Context context) {
        // Set new value into SharePreferences.
        setSaved(key, newValue, context);
        logD(MillicastManager.TAG, logTag + "Now: " + newValue +
                " Was: " + oldValue);
        return;
    }

    /**
     * Save newValue into SharedPreferences using specified key.
     *
     * @param key
     * @param oldValue The current value of this new value being saved.
     * @param newValue The new value of this value being saved.
     * @param logTag
     * @param context
     */
    public static void saveValue(String key, int oldValue, int newValue, String logTag, Context context) {
        // Set new value into SharePreferences.
        setSaved(key, newValue, context);
        logD(MillicastManager.TAG, logTag + "Now: " + newValue +
                " Was: " + oldValue);
        return;
    }

    /**
     * Get a String property from the specified source.
     *
     * @param source
     * @param current Value to return if source is CURRENT.
     * @param file    Value to return if source is FILE.
     * @param key     SharedPreferences key for which to get property if source is SAVED.
     * @param context
     * @return
     */
    public static String getProperty(MillicastManager.Source source, String current, String file, String key, Context context) {
        String value = null;
        String log = "[Prop][Get][" + source + "] " + key + ": ";
        switch (source) {
            case CURRENT:
                value = current;
                break;
            case FILE:
                value = file;
                break;
            case SAVED:
                value = getSaved(key, file, context);
                break;
        }
        logD(MillicastManager.TAG, log + value + ".");
        return value;
    }

    /**
     * Get a boolean property from the specified source.
     *
     * @param source
     * @param current Value to return if source is CURRENT.
     * @param file    Value to return if source is FILE.
     * @param key     SharedPreferences key for which to get property if source is SAVED.
     * @param context
     * @return
     */
    public static boolean getProperty(MillicastManager.Source source, boolean current, boolean file, String key, Context context) {
        boolean value = false;
        String log = "[Prop][Get][" + source + "] " + key + ": ";
        switch (source) {
            case CURRENT:
                value = current;
                break;
            case FILE:
                value = file;
                break;
            case SAVED:
                value = getSaved(key, file, context);
                break;
        }
        logD(MillicastManager.TAG, log + value + ".");
        return value;
    }

    //**********************************************************************************************
    // Strings
    //**********************************************************************************************

    /**
     * Get a string representing all element(s) of a {@link List}, separated by the given separator.
     *
     * @param itemList
     * @param separator
     * @param lambda    See {@link #getArrayStr(Object[], String, LambdaToString)}.
     * @param <T>
     * @return The representative string or an empty string if the list was null or empty.
     */
    public static <T> String getArrayStr(List<T> itemList, String separator, LambdaToString lambda) {
        String result = "";
        if (itemList == null || itemList.size() == 0) {
            return result;
        }
        T[] itemArray = (T[]) itemList.toArray();
        return getArrayStr(itemArray, separator, lambda);
    }

    /**
     * Get a string representing all element(s) of an array, separated by the given separator.
     *
     * @param itemList
     * @param separator
     * @param lambda    A {@link LambdaToString} that takes an element of the itemList and
     *                  generates its string representation that will be in the output.
     *                  If set to null, this string representation will be generated by the
     *                  {@link #toString()} method of the element.
     * @param <T>
     * @return The representative string or an empty string if the array was null or empty.
     */
    public static <T> String getArrayStr(T[] itemList, String separator, LambdaToString lambda) {
        String result = "";
        if (itemList == null || itemList.length == 0) {
            return result;
        }
        for (T item : itemList) {
            if (lambda != null) {
                result += lambda.toString(item);
            } else {
                result += item.toString();
            }
            result += separator;
        }
        // Remove the last separator if any had been added.
        result = result.substring(0, result.length() - separator.length());
        return result;
    }

    public interface LambdaToString {
        <T> String toString(T layerData);
    }

    //**********************************************************************************************
    // Views
    //**********************************************************************************************

    public static void removeFromParentView(VideoRenderer renderer, String logTag) {
        ViewParent parent = renderer.getParent();
        if (parent != null) {
            ((FrameLayout) parent).removeAllViews();
            Log.d(logTag, "Removed renderer from its previous parent.");
        }
    }

    /**
     * Stop displaying video on UI.
     *
     * @param frameLayout The layout that might contain video.
     * @param tag
     */
    public static void stopDisplayVideo(LinearLayout frameLayout, String tag) {
        if (frameLayout != null) {
            frameLayout.removeAllViews();
            Log.d(tag, "[stopDisplayVideo] Removed video display.");
        }
        Log.d(tag, "[stopDisplayVideo] Video no longer on UI.");
    }

    public static void makeSnackbar(String logTag, String msg, Fragment fragment) {
        MillicastManager.getSingleInstance().getMainActivity().runOnUiThread(() -> {
            logD(TAG, logTag + msg);
            if (fragment == null) {
                logD(TAG, "[Utils][Snackbar] Failed! Fragment not available. " +
                        "Only logging.");
                return;
            }
            View view = fragment.getView();
            if (view != null && view.getParent() != null) {
                Snackbar snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_SHORT);
                View snackbarView = snackbar.getView();
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) snackbarView.getLayoutParams();
                params.gravity = Gravity.TOP;
                params.width = WRAP_CONTENT;
                snackbarView.setLayoutParams(params);
                TextView tv = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                tv.setGravity(Gravity.CENTER_HORIZONTAL);
                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                tv.setMaxLines(10);
                snackbar.setAction("Action", null).show();
            } else {
                logD(TAG, "[Utils][Snackbar] Failed! View or parent not available. " +
                        "Only logging.");
            }
        });
    }

    //**********************************************************************************************
    // Views - Spinner
    //**********************************************************************************************

    /**
     * Creates a spinner using the provided parameters.
     *
     * @param items
     * @param spinner
     * @param selected If not negative, will set spinner to select at this index.
     * @param context
     * @param listener
     * @param <T>
     */
    public static <T> void populateSpinner(ArrayList<T> items, Spinner spinner, final int selected, Context context, OnItemSelected listener) {
        ArrayAdapter<T> arrayAdapter = new ArrayAdapter<T>(context,
                android.R.layout.simple_spinner_item,
                items);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(itemSelectedGenerator(listener));
        if (selected >= 0) {
            spinner.setSelection(selected);
        }
    }

    /**
     * Populate a spinner using the provided parameters.
     * If the lambda provided selected index is not negative, will set spinner to select at this index.
     *
     * @param spinnerList
     * @param spinner
     * @param lambda      {@link GetSelectedIndex} Lambda.
     * @param context
     * @param listener
     * @param <T>
     */
    public static <T> void populateSpinner(ArrayList<T> spinnerList, Spinner spinner, final GetSelectedIndex lambda, Context context, OnItemSelected listener) {
        String logTag = "[View][Spinner][Load] ";
        ArrayAdapter<T> arrayAdapter = new ArrayAdapter<T>(context,
                android.R.layout.simple_spinner_item,
                spinnerList);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(itemSelectedGenerator(listener));

        String selection = "N.A.";
        int index = lambda.getSelectedIndex(spinnerList);
        if (index >= 0) {
            spinner.setSelection(index);
            selection = (String) spinnerList.get(index);
        }
        logD(TAG, logTag + "Selection:" + selection + " at index:" + index +
                " of list:" + spinnerList + ".");
    }

    /**
     * Lambda interface that provides the selected index (at call time) of the given list.
     * If no index is selected, return a negative integer.
     *
     * @param <T>
     */
    public interface GetSelectedIndex<T> {
        default int getSelectedIndex(ArrayList<T> list) {
            return -1;
        }
    }

    public static AdapterView.OnItemSelectedListener itemSelectedGenerator(OnItemSelected lambda) {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                lambda.onItemSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };
    }

    public interface OnItemSelected {
        void onItemSelected(int position);
    }

}
