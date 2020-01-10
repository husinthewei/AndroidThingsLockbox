/*
 * Copyright 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidthings.simpleui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.Switch;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleUiActivity extends Activity {

    private static final String TAG = SimpleUiActivity.class.getSimpleName();

    private Map<String, Gpio> mGpioMap = new LinkedHashMap<>();

    private final String DATE_FILE = "test.txt";
    private final String PASSCODE_FILE = "pass.txt";

    private String passcode;

    private void removeFocus(EditText e, TextView v) {
        e.getText().clear();
        e.setFocusableInTouchMode(false);
        e.setFocusable(false);
        e.setFocusableInTouchMode(true);
        e.setFocusable(true);
        InputMethodManager imm =
                (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private void writeToFile(String filepath, String text) {
        Context context = this;
        File path = context.getFilesDir();
        File file = new File(path, filepath);
        try {
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(text.getBytes());
            stream.close();
        } catch (Exception e) {
            Log.e(TAG, "File error: ", e);
        }
    }

    private String readFromFile(String filepath) {
        Context context = this;
        File path = context.getFilesDir();
        File file = new File(path, filepath);

        int length = (int) file.length();
        byte[] bytes = new byte[length];


        try {
            FileInputStream in = new FileInputStream(file);
            in.read(bytes);
            in.close();
        } catch (Exception e) {
            Log.e(TAG, "File error: ", e);
        }

        String contents = new String(bytes);

        Log.i(TAG, "contents: " + contents);

        return contents;
    }

    private void createPasscode(String pass) {
        writeToFile(PASSCODE_FILE, pass);
    }

    private String getPasscode() {
        return readFromFile(PASSCODE_FILE);
    }

    private void addLockDate(String date) {
        writeToFile(DATE_FILE, date);
    }

    private String readLockDate(String date) {
        return readFromFile(date);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LayoutInflater inflater = getLayoutInflater();
        PeripheralManager pioManager = PeripheralManager.getInstance();

        // Set action on entered passcode
        final EditText passcodeInput = findViewById(R.id.passcodeInput);
        passcodeInput.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String entered = passcodeInput.getText().toString();

                    final TextView passStatusText = findViewById(R.id.passStatus);
                    if (entered.equals(passcode)) {
                        passStatusText.setText("Go wild dawhg");
                        passStatusText.setTextColor(Color.GREEN);
                    } else {
                        passStatusText.setText("Try again");
                        passStatusText.setTextColor(Color.RED);
                    }

                    handled = true;
                    removeFocus(passcodeInput, v);
                }
                return handled;
            }
        });

        // Action on entered date
        final EditText dateInput = findViewById(R.id.dateInput);
        dateInput.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Log.i(TAG, "Entered " + dateInput.getText().toString());
                    handled = true;
                    removeFocus(dateInput, v);
                }
                return handled;
            }
        });

        // Get passcode from file
        passcode = getPasscode();

        /*
            try {
                final Gpio ledPin = pioManager.openGpio(name);
                ledPin.setEdgeTriggerType(Gpio.EDGE_NONE);
                ledPin.setActiveType(Gpio.ACTIVE_HIGH);
                ledPin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

                button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        try {
                            ledPin.setValue(isChecked);
                        } catch (IOException e) {
                            Log.e(TAG, "error toggling gpio:", e);
                            buttonView.setOnCheckedChangeListener(null);
                            // reset button to previous state.
                            buttonView.setChecked(!isChecked);
                            buttonView.setOnCheckedChangeListener(this);
                        }
                    }
                });

                mGpioMap.put(name, ledPin);
            } catch (IOException e) {
                Log.e(TAG, "Error initializing GPIO: " + name, e);
                // disable button
                button.setEnabled(false);
            }
        */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (Map.Entry<String, Gpio> entry : mGpioMap.entrySet()) {
            try {
                entry.getValue().close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing GPIO " + entry.getKey(), e);
            }
        }
        mGpioMap.clear();
    }
}