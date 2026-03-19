package com.btmacromouse;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.Executor;

public class BluetoothHidService {

    private static final String TAG = "BluetoothHidService";

    // HID Report Descriptor for a standard mouse
    private static final byte[] MOUSE_REPORT_DESCRIPTOR = {
        (byte)0x05, (byte)0x01,  // Usage Page (Generic Desktop)
        (byte)0x09, (byte)0x02,  // Usage (Mouse)
        (byte)0xA1, (byte)0x01,  // Collection (Application)
        (byte)0x09, (byte)0x01,  //   Usage (Pointer)
        (byte)0xA1, (byte)0x00,  //   Collection (Physical)
        (byte)0x05, (byte)0x09,  //     Usage Page (Button)
        (byte)0x19, (byte)0x01,  //     Usage Minimum (Button 1)
        (byte)0x29, (byte)0x03,  //     Usage Maximum (Button 3)
        (byte)0x15, (byte)0x00,  //     Logical Minimum (0)
        (byte)0x25, (byte)0x01,  //     Logical Maximum (1)
        (byte)0x95, (byte)0x03,  //     Report Count (3)
        (byte)0x75, (byte)0x01,  //     Report Size (1)
        (byte)0x81, (byte)0x02,  //     Input (Data, Variable, Absolute)
        (byte)0x95, (byte)0x01,  //     Report Count (1)
        (byte)0x75, (byte)0x05,  //     Report Size (5)
        (byte)0x81, (byte)0x03,  //     Input (Constant)
        (byte)0x05, (byte)0x01,  //     Usage Page (Generic Desktop)
        (byte)0x09, (byte)0x30,  //     Usage (X)
        (byte)0x09, (byte)0x31,  //     Usage (Y)
        (byte)0x15, (byte)0x81,  //     Logical Minimum (-127)
        (byte)0x25, (byte)0x7F,  //     Logical Maximum (127)
        (byte)0x75, (byte)0x08,  //     Report Size (8)
        (byte)0x95, (byte)0x02,  //     Report Count (2)
        (byte)0x81, (byte)0x06,  //     Input (Data, Variable, Relative)
        (byte)0xC0,              //   End Collection
        (byte)0xC0               // End Collection
    };

    public interface ConnectionListener {
        void onConnected(BluetoothDevice device);
        void onDisconnected(BluetoothDevice device);
        void onReady();
        void onError(String message);
    }

    private final Context context;
    private BluetoothHidDevice hidDevice;
    private BluetoothDevice connectedDevice;
    private ConnectionListener listener;
    private boolean isRegistered = false;

    private final BluetoothHidDevice.Callback hidCallback = new BluetoothHidDevice.Callback() {
        @Override
        public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
            isRegistered = registered;
            if (registered && listener != null) {
                new Handler(Looper.getMainLooper()).post(() -> listener.onReady());
            }
        }

        @Override
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            if (state == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device;
                if (listener != null) {
                    new Handler(Looper.getMainLooper()).post(() -> listener.onConnected(device));
                }
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                if (device.equals(connectedDevice)) connectedDevice = null;
                if (listener != null) {
                    new Handler(Looper.getMainLooper()).post(() -> listener.onDisconnected(device));
                }
            }
        }
    };

    private final BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            hidDevice = (BluetoothHidDevice) proxy;
            BluetoothHidDeviceAppSdpSettings sdp = new BluetoothHidDeviceAppSdpSettings(
                    "BT Macro Mouse",
                    "Macro Mouse Controller",
                    "BTMacroMouse",
                    BluetoothHidDevice.SUBCLASS1_MOUSE,
                    MOUSE_REPORT_DESCRIPTOR
            );
            Executor executor = command -> new Handler(Looper.getMainLooper()).post(command);
            hidDevice.registerApp(sdp, null, null, executor, hidCallback);
        }

        @Override
        public void onServiceDisconnected(int profile) {
            hidDevice = null;
        }
    };

    public BluetoothHidService(Context context) {
        this.context = context;
    }

    public void setListener(ConnectionListener listener) {
        this.listener = listener;
    }

    public void initialize(android.bluetooth.BluetoothAdapter adapter) {
        adapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE);
    }

    public void release(android.bluetooth.BluetoothAdapter adapter) {
        if (hidDevice != null) {
            hidDevice.unregisterApp();
            adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice);
        }
    }

    public boolean isConnected() {
        return connectedDevice != null;
    }

    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }

    // Move mouse by relative dx, dy (-127 to 127)
    public void moveMouse(int dx, int dy) {
        if (hidDevice == null || connectedDevice == null) return;
        // Clamp
        dx = Math.max(-127, Math.min(127, dx));
        dy = Math.max(-127, Math.min(127, dy));
        byte[] report = {0x00, (byte) dx, (byte) dy};
        hidDevice.sendReport(connectedDevice, 0, report);
    }

    // Left click down
    public void mouseDown() {
        if (hidDevice == null || connectedDevice == null) return;
        byte[] report = {0x01, 0x00, 0x00};
        hidDevice.sendReport(connectedDevice, 0, report);
    }

    // Left click up
    public void mouseUp() {
        if (hidDevice == null || connectedDevice == null) return;
        byte[] report = {0x00, 0x00, 0x00};
        hidDevice.sendReport(connectedDevice, 0, report);
    }

    /**
     * Move cursor to target by sending relative movements in chunks of max 127,
     * then perform a left click.
     * totalX, totalY: total relative pixels to move from current position
     */
    public void moveAndClick(int totalX, int totalY, Runnable onDone) {
        new Thread(() -> {
            try {
                // First send a large move to push cursor to top-left corner (reset position)
                // Send strong up-left to home the cursor
                for (int i = 0; i < 30; i++) {
                    moveMouse(-127, -127);
                    Thread.sleep(8);
                }
                Thread.sleep(50);

                // Now move to target
                int remainX = totalX;
                int remainY = totalY;
                while (remainX != 0 || remainY != 0) {
                    int stepX = Math.max(-127, Math.min(127, remainX));
                    int stepY = Math.max(-127, Math.min(127, remainY));
                    moveMouse(stepX, stepY);
                    remainX -= stepX;
                    remainY -= stepY;
                    Thread.sleep(8);
                }

                Thread.sleep(50);

                // Click
                mouseDown();
                Thread.sleep(80);
                mouseUp();

                if (onDone != null) {
                    new Handler(Looper.getMainLooper()).post(onDone);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "moveAndClick interrupted", e);
            }
        }).start();
    }

    public void connectToDevice(BluetoothDevice device) {
        if (hidDevice != null && isRegistered) {
            hidDevice.connect(device);
        }
    }
}
