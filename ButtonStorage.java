package com.btmacromouse;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ButtonStorage {

    private static final String PREFS_NAME = "macro_buttons";
    private static final String KEY_BUTTONS = "buttons";

    private final SharedPreferences prefs;

    public ButtonStorage(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<MacroButton> loadButtons() {
        List<MacroButton> list = new ArrayList<>();
        String json = prefs.getString(KEY_BUTTONS, null);
        if (json == null) {
            // Default sample buttons
            list.add(new MacroButton("Top Left", 200, 150, Color.parseColor("#E53935")));
            list.add(new MacroButton("Center", 960, 540, Color.parseColor("#1E88E5")));
            list.add(new MacroButton("Bottom Right", 1720, 930, Color.parseColor("#43A047")));
            return list;
        }
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new MacroButton(
                        obj.getString("name"),
                        obj.getInt("x"),
                        obj.getInt("y"),
                        obj.getInt("color")
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void saveButtons(List<MacroButton> buttons) {
        JSONArray arr = new JSONArray();
        for (MacroButton b : buttons) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("name", b.getName());
                obj.put("x", b.getTargetX());
                obj.put("y", b.getTargetY());
                obj.put("color", b.getColor());
                arr.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        prefs.edit().putString(KEY_BUTTONS, arr.toString()).apply();
    }

    public String getSavedDeviceAddress() {
        return prefs.getString("device_address", null);
    }

    public void saveDeviceAddress(String address) {
        prefs.edit().putString("device_address", address).apply();
    }
}
