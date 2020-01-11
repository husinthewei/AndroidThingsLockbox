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
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
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

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.contrib.driver.pwmservo.Servo;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleUiActivity extends Activity {

    private static final String TAG = SimpleUiActivity.class.getSimpleName();

    private final String DATE_FILE = "test.txt";
    private final String PASSCODE_FILE = "pass.txt";

    private String passcode;

    private static final String lockServoPin = "PWM1";
    private Servo lockServo = null;

    // Close servo object
    private void closeServo() {
        if (lockServo != null) {
            try {
                lockServo.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close lock servo", e);
            } finally {
                lockServo = null;
            }
        }
    }

    // Disable servo after 0.75 seconds
    private void disableServo() {
        if (lockServo == null) {
            return;
        }

        // Wait 0.75 seconds
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    lockServo.setEnabled(false);
                    closeServo();
                } catch (IOException e) {
                    Log.e(TAG, "Could not disable/enable lock servo", e);
                }
            }
        }, 750);
    }



    // Move servo to `angle` degrees and enable or disable servo
    private void moveServo(int angle) {
        if (lockServo == null) {
            return;
        }
        try {
            lockServo.setAngle(angle);
        } catch (IOException e) {
            Log.e(TAG, "Could not set angle on lock servo", e);
        }
    }

    // Initialize and replace servo object if exists
    private void initializeServo() {
        if (lockServo != null) {
            return;
        }
        try {
            lockServo = new Servo(lockServoPin);
            lockServo.setPulseDurationRange(0.5, 2.4); // according to your servo's specifications
            lockServo.setAngleRange(0, 180);       // according to your servo's specifications
            lockServo.setEnabled(true);
        } catch (IOException e) {
            Log.e(TAG, "Could not open lock servo", e);
            lockServo = null;
        }
    }

    // Close the lock and hide lock button
    private void lockTheBox() {
        initializeServo();
        moveServo(0);
        disableServo();

        // Hide lock button
        Button lockButton = findViewById(R.id.lockButton);
        lockButton.setVisibility(View.GONE);
    }

    // Unlock the box and add lock button
    private void unlockTheBox() {
        initializeServo();
        moveServo(180 );

        // Show lock button
        Button lockButton = findViewById(R.id.lockButton);
        lockButton.setVisibility(View.VISIBLE);
    }

    // Remove an EditText from focus
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

    // Write text to file at filepath
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

    // Returns contents of file at filepath
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

    // Set the password in the password file
    private void createPasscode(String pass) {
        writeToFile(PASSCODE_FILE, pass);
    }

    // Get current passcode in string format
    private String getPasscode() {
        return readFromFile(PASSCODE_FILE);
    }

    // Set a date to lock
    private void addLockDate(String date) {
        writeToFile(DATE_FILE, date);
    }

    // Check and return the current date in the lockdate file
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
        final TextView passStatusText = findViewById(R.id.passStatus);
        passcodeInput.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String entered = passcodeInput.getText().toString();

                    if (entered.equals(passcode)) {
                        passStatusText.setText("Go wild dawhg");
                        passStatusText.setTextColor(Color.GREEN);
                        unlockTheBox();
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

        // Action on lock button press
        Button lockButton = findViewById(R.id.lockButton);
        lockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                passStatusText.setText("");
                lockTheBox();
            }
        });

        // Get passcode from file
        passcode = getPasscode();

        // Lock the box by default
        lockTheBox();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeServo();
    }
}