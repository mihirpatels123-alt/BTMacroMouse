package com.btmacromouse;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1001;
    private static final int REQUEST_EDIT_BUTTON = 1002;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothHidService hidService;
    private ButtonStorage storage;

    private TextView statusText;
    private Button connectButton;
    private RecyclerView recyclerView;
    private MacroButtonAdapter adapter;
    private List<MacroButton> buttons;
    private View progressOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        connectButton = findViewById(R.id.connect_button);
        recyclerView = findViewById(R.id.recycler_view);
        progressOverlay = findViewById(R.id.progress_overlay);
        FloatingActionButton fab = findViewById(R.id.fab_add);

        storage = new ButtonStorage(this);
        buttons = storage.loadButtons();

        adapter = new MacroButtonAdapter(buttons, new MacroButtonAdapter.OnButtonClickListener() {
            @Override
            public void onMacroClick(int position) {
                executeMacro(buttons.get(position));
            }

            @Override
            public void onEditClick(int position) {
                Intent intent = new Intent(MainActivity.this, EditButtonActivity.class);
                intent.putExtra("position", position);
                MacroButton b = buttons.get(position);
                intent.putExtra("name", b.getName());
                intent.putExtra("x", b.getTargetX());
                intent.putExtra("y", b.getTargetY());
                intent.putExtra("color", b.getColor());
                startActivityForResult(intent, REQUEST_EDIT_BUTTON);
            }

            @Override
            public void onDeleteClick(int position) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Button")
                        .setMessage("Delete \"" + buttons.get(position).getName() + "\"?")
                        .setPositiveButton("Delete", (d, w) -> {
                            buttons.remove(position);
                            adapter.notifyItemRemoved(position);
                            storage.saveButtons(buttons);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        connectButton.setOnClickListener(v -> showDevicePicker());

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditButtonActivity.class);
            intent.putExtra("position", -1);
            intent.putExtra("name", "New Button");
            intent.putExtra("x", 960);
            intent.putExtra("y", 540);
            intent.putExtra("color", Color.parseColor("#1E88E5"));
            startActivityForResult(intent, REQUEST_EDIT_BUTTON);
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        hidService = new BluetoothHidService(this);
        hidService.setListener(new BluetoothHidService.ConnectionListener() {
            @Override
            public void onConnected(BluetoothDevice device) {
                String name = ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                        ? device.getName() : device.getAddress();
                statusText.setText("✅ Connected to " + name);
                statusText.setTextColor(Color.parseColor("#43A047"));
                connectButton.setText("Disconnect / Change");
                adapter.setConnected(true);
                storage.saveDeviceAddress(device.getAddress());
            }

            @Override
            public void onDisconnected(BluetoothDevice device) {
                statusText.setText("🔴 Disconnected. Tap 'Connect to TV'");
                statusText.setTextColor(Color.parseColor("#E53935"));
                connectButton.setText("Connect to TV");
                adapter.setConnected(false);
            }

            @Override
            public void onReady() {
                statusText.setText("🟡 Ready — tap 'Connect to TV'");
                // Auto reconnect last device
                String lastAddr = storage.getSavedDeviceAddress();
                if (lastAddr != null) {
                    BluetoothDevice dev = bluetoothAdapter.getRemoteDevice(lastAddr);
                    if (dev != null) hidService.connectToDevice(dev);
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });

        requestPermissionsAndInit();
    }

    private void requestPermissionsAndInit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE
            }, REQUEST_PERMISSIONS);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            hidService.initialize(bluetoothAdapter);
        }
    }

    private void showDevicePicker() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<BluetoothDevice> paired = bluetoothAdapter.getBondedDevices();
        if (paired.isEmpty()) {
            Toast.makeText(this, "No paired devices found. Pair your TV first in Settings > Bluetooth", Toast.LENGTH_LONG).show();
            return;
        }

        BluetoothDevice[] devices = paired.toArray(new BluetoothDevice[0]);
        String[] names = new String[devices.length];
        for (int i = 0; i < devices.length; i++) {
            try {
                names[i] = devices[i].getName() + "\n" + devices[i].getAddress();
            } catch (SecurityException e) {
                names[i] = devices[i].getAddress();
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select your TV")
                .setItems(names, (dialog, which) -> {
                    hidService.connectToDevice(devices[which]);
                    statusText.setText("🔄 Connecting to " + names[which].split("\n")[0] + "...");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeMacro(MacroButton btn) {
        if (!hidService.isConnected()) {
            Toast.makeText(this, "Not connected to TV!", Toast.LENGTH_SHORT).show();
            return;
        }
        progressOverlay.setVisibility(View.VISIBLE);
        TextView progressText = progressOverlay.findViewById(R.id.progress_text);
        progressText.setText("Moving to: " + btn.getName() + "...");

        hidService.moveAndClick(btn.getTargetX(), btn.getTargetY(), () -> {
            progressOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "✅ Clicked: " + btn.getName(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_BUTTON && resultCode == RESULT_OK && data != null) {
            int position = data.getIntExtra("position", -1);
            String name = data.getStringExtra("name");
            int x = data.getIntExtra("x", 0);
            int y = data.getIntExtra("y", 0);
            int color = data.getIntExtra("color", Color.BLUE);

            MacroButton btn = new MacroButton(name, x, y, color);
            if (position == -1) {
                buttons.add(btn);
                adapter.notifyItemInserted(buttons.size() - 1);
            } else {
                buttons.set(position, btn);
                adapter.notifyItemChanged(position);
            }
            storage.saveButtons(buttons);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hidService != null) hidService.release(bluetoothAdapter);
    }
}
