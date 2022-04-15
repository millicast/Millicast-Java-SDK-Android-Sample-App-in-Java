package com.millicast.android_app;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.millicast.android_app.MillicastManager.Source;

import java.util.concurrent.atomic.AtomicBoolean;

import static android.graphics.Color.GRAY;
import static com.millicast.android_app.MillicastManager.Source.CURRENT;
import static com.millicast.android_app.MillicastManager.Source.FILE;
import static com.millicast.android_app.MillicastManager.Source.SAVED;
import static com.millicast.android_app.MillicastManager.keyAccountId;
import static com.millicast.android_app.MillicastManager.keyPublishApiUrl;
import static com.millicast.android_app.MillicastManager.keyPublishToken;
import static com.millicast.android_app.MillicastManager.keySubscribeToken;
import static com.millicast.android_app.MillicastManager.keyRicohTheta;
import static com.millicast.android_app.MillicastManager.keyStreamNamePub;
import static com.millicast.android_app.MillicastManager.keyStreamNameSub;
import static com.millicast.android_app.MillicastManager.keySubscribeApiUrl;

public class SetMcFragment extends Fragment {
    public static final String TAG = "SetMcFragment";
    Drawable backgroundOriginal;
    private final MillicastManager mcManager;
    private Switch switchRicohTheta;
    private TextInputEditText editTextAccountId;
    private TextInputEditText editTextStreamNamePub;
    private TextInputEditText editTextStreamNameSub;
    private TextInputEditText editTextPublishToken;
    private TextInputEditText editTextSubscribeToken;
    private TextInputEditText editTextPublishApiUrl;
    private TextInputEditText editTextSubscribeApiUrl;
    private final AtomicBoolean changedRicohTheta = new AtomicBoolean(false);
    private final AtomicBoolean changedAccountId = new AtomicBoolean(false);
    private final AtomicBoolean changedStreamNamePub = new AtomicBoolean(false);
    private final AtomicBoolean changedStreamNameSub = new AtomicBoolean(false);
    private final AtomicBoolean changedPublishToken = new AtomicBoolean(false);
    private final AtomicBoolean changedSubscribeToken = new AtomicBoolean(false);
    private final AtomicBoolean changedPublishApiUrl = new AtomicBoolean(false);
    private final AtomicBoolean changedSubscribeApiUrl = new AtomicBoolean(false);
    private Button buttonLoadApplied;
    private Button buttonLoadSaved;
    private Button buttonLoadFile;
    private Button buttonApply;
    private Button buttonApplySave;

    public SetMcFragment() {
        mcManager = MillicastManager.getSingleInstance();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_set_mc, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        switchRicohTheta = view.findViewById(R.id.switchRicohTheta);
        switchRicohTheta.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean valueApplied = mcManager.isRicohTheta(CURRENT);
                boolean hasChanged = (valueApplied != isChecked);
                setMark(switchRicohTheta, hasChanged, changedRicohTheta);
            }
        });
        editTextAccountId = view.findViewById(R.id.account_id);
        editTextAccountId.setOnFocusChangeListener(createListener(() -> {
            return mcManager.getAccountId(CURRENT);
        }, changedAccountId));
        editTextStreamNamePub = view.findViewById(R.id.stream_name_pub);
        editTextStreamNamePub.setOnFocusChangeListener(createListener(() -> {
            return mcManager.getStreamNamePub(CURRENT);
        }, changedStreamNamePub));
        editTextStreamNameSub = view.findViewById(R.id.stream_name_sub);
        editTextStreamNameSub.setOnFocusChangeListener(createListener(() -> {
            return mcManager.getStreamNameSub(CURRENT);
        }, changedStreamNameSub));
        editTextPublishToken = view.findViewById(R.id.publish_token);
        editTextPublishToken.setOnFocusChangeListener(createListener(() -> {
            return mcManager.getPublishToken(CURRENT);
        }, changedPublishToken));
        editTextSubscribeToken = view.findViewById(R.id.subscribe_token);
        editTextSubscribeToken.setOnFocusChangeListener(createListener(() -> {
            return mcManager.getSubscribeToken(CURRENT);
        }, changedSubscribeToken));
        editTextPublishApiUrl = view.findViewById(R.id.publish_url);
        editTextPublishApiUrl.setOnFocusChangeListener(createListener(() -> {
            return mcManager.getPublishApiUrl(CURRENT);
        }, changedPublishApiUrl));
        editTextSubscribeApiUrl = view.findViewById(R.id.subscribe_url);
        editTextSubscribeApiUrl.setOnFocusChangeListener(createListener(() -> {
            return mcManager.getSubscribeApiUrl(CURRENT);
        }, changedSubscribeApiUrl));

        buttonLoadApplied = view.findViewById(R.id.button_applied);
        buttonLoadSaved = view.findViewById(R.id.button_saved);
        buttonLoadFile = view.findViewById(R.id.button_file);
        buttonApply = view.findViewById(R.id.button_apply);
        buttonApplySave = view.findViewById(R.id.button_apply_save);

        if (savedInstanceState == null) {
            // Will only lock if it's the first time.
            mcManager.setCameraLock(true);

            loadProps(CURRENT);
            // Do not consider as change for the first load.
            changedRicohTheta.set(false);
            changedAccountId.set(false);
            changedStreamNamePub.set(false);
            changedStreamNameSub.set(false);
            changedPublishToken.set(false);
            changedSubscribeToken.set(false);
            changedPublishApiUrl.set(false);
            changedSubscribeApiUrl.set(false);
        } else {
            changedRicohTheta.set(savedInstanceState.getBoolean(keyRicohTheta));
            changedAccountId.set(savedInstanceState.getBoolean(keyAccountId));
            changedStreamNamePub.set(savedInstanceState.getBoolean(keyStreamNamePub));
            changedStreamNameSub.set(savedInstanceState.getBoolean(keyStreamNameSub));
            changedPublishToken.set(savedInstanceState.getBoolean(keyPublishToken));
            changedSubscribeToken.set(savedInstanceState.getBoolean(keySubscribeToken));
            changedPublishApiUrl.set(savedInstanceState.getBoolean(keyPublishApiUrl));
            changedSubscribeApiUrl.set(savedInstanceState.getBoolean(keySubscribeApiUrl));
        }

        buttonLoadApplied.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFocus();
                loadProps(CURRENT);
            }
        });

        buttonLoadSaved.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFocus();
                loadProps(SAVED);
            }
        });

        buttonLoadFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFocus();
                loadProps(FILE);
            }
        });

        buttonApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFocus();
                applyProps(false);
            }
        });

        buttonApplySave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFocus();
                applyProps(true);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(keyRicohTheta, changedRicohTheta.get());
        outState.putBoolean(keyAccountId, changedAccountId.get());
        outState.putBoolean(keyAccountId, changedAccountId.get());
        outState.putBoolean(keyStreamNamePub, changedStreamNamePub.get());
        outState.putBoolean(keyStreamNameSub, changedStreamNameSub.get());
        outState.putBoolean(keyPublishToken, changedPublishToken.get());
        outState.putBoolean(keySubscribeToken, changedSubscribeToken.get());
        outState.putBoolean(keyPublishApiUrl, changedPublishApiUrl.get());
        outState.putBoolean(keySubscribeApiUrl, changedSubscribeApiUrl.get());
        super.onSaveInstanceState(outState);
    }

    private View.OnFocusChangeListener createListener(getValueApplied lambda, final AtomicBoolean changed) {
        return new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String valueApplied = lambda.getValueApplied();
                    String valueNew = ((TextInputEditText) v).getText().toString();
                    boolean hasChanged = (!valueApplied.equals(valueNew));
                    setMark(v, hasChanged, changed);
                }
            }
        };
    }

    /**
     * If the new value is different from the applied value,
     * set the change marker to true and set background of view to a new color.
     * Else, reset the view and marker to original state.
     *
     * @param view
     * @param hasChanged
     * @param changed
     */
    private void setMark(View view, boolean hasChanged, AtomicBoolean changed) {
        if (hasChanged) {
            // Indicate that value is different from applied value.
            changed.set(true);
            if (backgroundOriginal == null) {
                backgroundOriginal = view.getBackground();
            }
            view.setBackgroundColor(GRAY);
        } else {
            // If it was formerly changed, reset to original.
            if (changed.get()) {
                changed.set(false);
                if (backgroundOriginal != null) {
                    view.setBackground(backgroundOriginal);
                }
            }
        }
    }

    private interface getValueApplied {
        String getValueApplied();
    }

    private void clearFocus() {
        View current = getActivity().getCurrentFocus();
        if (current != null) current.clearFocus();
    }

    /**
     * Get MillicastManager properties from the specified Source
     * and populate the UI with them.
     *
     * @param source
     */
    private void loadProps(Source source) {
        setMarkChange(editTextAccountId, mcManager.getAccountId(CURRENT), mcManager.getAccountId(source), changedAccountId);
        setMarkChange(editTextStreamNamePub, mcManager.getStreamNamePub(CURRENT), mcManager.getStreamNamePub(source), changedStreamNamePub);
        setMarkChange(editTextStreamNameSub, mcManager.getStreamNameSub(CURRENT), mcManager.getStreamNameSub(source), changedStreamNameSub);
        setMarkChange(editTextPublishToken, mcManager.getPublishToken(CURRENT), mcManager.getPublishToken(source), changedPublishToken);
        setMarkChange(editTextSubscribeToken, mcManager.getSubscribeToken(CURRENT), mcManager.getSubscribeToken(source), changedSubscribeToken);
        setMarkChange(editTextPublishApiUrl, mcManager.getPublishApiUrl(CURRENT), mcManager.getPublishApiUrl(source), changedPublishApiUrl);
        setMarkChange(editTextSubscribeApiUrl, mcManager.getSubscribeApiUrl(CURRENT), mcManager.getSubscribeApiUrl(source), changedSubscribeApiUrl);
        setMarkChange(switchRicohTheta, mcManager.isRicohTheta(CURRENT), mcManager.isRicohTheta(source), changedRicohTheta);
    }

    /**
     * Set the new value into the TextInputEditText view.
     * If it differs from the current value, marked that value has changed.
     *
     * @param view
     * @param valueApplied
     * @param valueNew
     * @param changed
     */
    private void setMarkChange(TextInputEditText view, String valueApplied, String valueNew, AtomicBoolean changed) {
        boolean hasChanged = (!valueApplied.equals(valueNew));
        setMark(view, hasChanged, changed);
        view.setText(valueNew);
    }

    /**
     * Set the new value into the Switch view.
     * If it differs from the current value, marked that value has changed.
     *
     * @param view
     * @param valueApplied
     * @param valueNew
     * @param changed
     */
    private void setMarkChange(Switch view, boolean valueApplied, boolean valueNew, AtomicBoolean changed) {
        boolean hasChanged = (valueApplied != valueNew);
        setMark(view, hasChanged, changed);
        view.setChecked(valueNew);
    }

    /**
     * Set MillicastManager properties with the values on UI, if they differ from applied values.
     *
     * @param save Save to device memory if true.
     */
    private void applyProps(boolean save) {
        String logTag = "[Props][Apply] ";
        String logTagError = "Unable to set values as ";
        String logTagPub = "publishing state is: " + mcManager.getPubState() + "\n";
        String logTagSub = "subscribing state is: " + mcManager.getSubState() + "\n";
        String logTagCap = "capturing state is: " + mcManager.getCapState() + "\n";
        String logPub = "";
        String logSub = "";
        String logCap = "";
        boolean applied = false;
        if (changedAccountId.get() || save) {
            if (!mcManager.setAccountId(editTextAccountId.getText().toString(), save)) {
                logSub += keyAccountId + " ";
            } else {
                applied = true;
            }
            setMark(editTextAccountId, false, changedAccountId);
        }
        if (changedStreamNamePub.get() || save) {
            if (!mcManager.setStreamNamePub(editTextStreamNamePub.getText().toString(), save)) {
                logPub += keyStreamNamePub + " ";
            } else {
                applied = true;
            }
            setMark(editTextStreamNamePub, false, changedStreamNamePub);
        }
        if (changedStreamNameSub.get() || save) {
            if (!mcManager.setStreamNameSub(editTextStreamNameSub.getText().toString(), save)) {
                logSub += keyStreamNameSub + " ";
            } else {
                applied = true;
            }
            setMark(editTextStreamNameSub, false, changedStreamNameSub);
        }
        if (changedPublishToken.get() || save) {
            if (!mcManager.setPublishToken(editTextPublishToken.getText().toString(), save)) {
                logPub += keyPublishToken + " ";
            } else {
                applied = true;
            }
            setMark(editTextPublishToken, false, changedPublishToken);
        }
        if (changedSubscribeToken.get() || save) {
            if (!mcManager.setSubscribeToken(editTextSubscribeToken.getText().toString(), save)) {
                logSub += keySubscribeToken + " ";
            } else {
                applied = true;
            }
            setMark(editTextSubscribeToken, false, changedSubscribeToken);
        }
        if (changedPublishApiUrl.get() || save) {
            if (!mcManager.setPublishApiUrl(editTextPublishApiUrl.getText().toString(), save)) {
                logPub += keyPublishApiUrl + " ";
            } else {
                applied = true;
            }
            setMark(editTextPublishApiUrl, false, changedPublishApiUrl);
        }
        if (changedSubscribeApiUrl.get() || save) {
            if (!mcManager.setSubscribeApiUrl(editTextSubscribeApiUrl.getText().toString(), save)) {
                logSub += keySubscribeApiUrl + " ";
            } else {
                applied = true;
            }
            setMark(editTextSubscribeApiUrl, false, changedSubscribeApiUrl);
        }
        if (changedRicohTheta.get() || save) {
            if (!mcManager.setRicohTheta(switchRicohTheta.isChecked(), save)) {
                logCap += keyRicohTheta;
            } else {
                applied = true;
            }
            setMark(switchRicohTheta, false, changedRicohTheta);
        }

        // Load values from applied in case these values were not set.
        loadProps(CURRENT);
        String log = "";
        log = addLog(log, logTagError + logTagPub, logPub);
        log = addLog(log, logTagError + logTagSub, logSub);
        log = addLog(log, logTagError + logTagCap, logCap);

        if (log.isEmpty()) {
            if (applied) {
                log = "Values applied";
                if (save) {
                    log += " and saved";
                }
                log += ".";
            } else {
                log = "No change.";
            }
        }
        Utils.makeSnackbar(logTag, log, this);
    }

    /**
     * Log and create Snackbar if error is not null.
     *
     * @param log
     * @param logTag
     * @param error
     * @return
     */
    private String addLog(String log, String logTag, String error) {
        if (!error.isEmpty()) {
            if (!log.isEmpty()) {
                log += "\n";
            }
            log += logTag + error;
        }
        return log;
    }
}