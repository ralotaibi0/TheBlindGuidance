package com.seniorproject.theblindguidance;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothDevice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    private Button connect;
    private TextView data;
    private Handler btHandler;
    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket = null;
    private ArrayAdapter<String> btArrayAdapter;
    private MediaPlayer MP;

    private StringBuilder sensorsData;
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final String address = "00:12:02:09:05:73";
    private final String name = "linvor";

    private ConnectThread btConnectedThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connect = (Button) findViewById(R.id.Connect);
        data = (TextView) findViewById(R.id.Data);
        data.setVisibility(View.INVISIBLE);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        sensorsData = new StringBuilder();

        //Search for the system in advance, in order to connect quickly.
        btAdapter.startDiscovery();
        registerReceiver(btReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));


        btHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == 0) {
                    String Message = (String) msg.obj;
                    sensorsData.append(Message);
                    int endOfMessage = sensorsData.lastIndexOf("*");
                    if (endOfMessage > 3) {
                        if (sensorsData.charAt(0) == '#') {
                            String actualData = sensorsData.substring(1, endOfMessage);
                            String[] sensors = actualData.split("!");
                            int rightSensor = Integer.parseInt(sensors[0]);
                            int leftSensor = Integer.parseInt(sensors[1]);
                            data.setText("Right Sensor : " + sensors[0] + " Left Sensor : " + sensors[1]);

                            if ((rightSensor < 70 && rightSensor > 2) && (leftSensor < 70 && leftSensor > 2)) {
                                playSound("stop");
                            } else {
                                if (rightSensor < 70 && rightSensor > 2) {
                                    playSound("slightlyleft");
                                } else if (leftSensor < 70 && leftSensor > 2) {
                                    playSound("slightlyright");
                                }

                            }
                        }
                        sensorsData.delete(0, sensorsData.length());
                    }
                }
                if (msg.what == 1) {
                    Toast.makeText(getBaseContext(), "connected.", Toast.LENGTH_LONG).show();
                    connect.setVisibility(View.INVISIBLE);
                    data.setVisibility(View.VISIBLE);
                    playSound("connected");
                }
            }

            ;

        };


        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectBT();
            }
        });
    }

    private void playSound(String key) {

        if (key.equals("connected")) {
            MP = MediaPlayer.create(MainActivity.this, R.raw.bg_connected);
        } else if (key.equals("notavailable")) {
            MP = MediaPlayer.create(MainActivity.this, R.raw.bg_notavailable);
        } else if (key.equals("plzwait")) {
            MP = MediaPlayer.create(MainActivity.this, R.raw.bg_plzwait);
        } else if (key.equals("stop")) {
            MP = MediaPlayer.create(MainActivity.this, R.raw.bg_stop);
        } else if (key.equals("slightlyleft")) {
            MP = MediaPlayer.create(MainActivity.this, R.raw.bg_slightlyleft);
        } else if (key.equals("slightlyright")) {
            MP = MediaPlayer.create(MainActivity.this, R.raw.bg_slightlyright);
        }

        if (!MP.isPlaying()){
            MP.start();
        }
    }

    private void connectBT() {
        boolean btFound = false;
        //ENABLE BT if its NOT ENABLED.
        if (!btAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT,1);
        }
        if (btAdapter.isDiscovering()) {
            playSound("plzwait");
            Toast.makeText(getBaseContext(), "Please Wait.", Toast.LENGTH_SHORT).show();
        } else {
            btAdapter.startDiscovery();
            registerReceiver(btReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        }
        for (int i = 0; i < btArrayAdapter.getCount(); i++) {
            String device = btArrayAdapter.getItem(i);
            if (device.equals(name + "\n" + address)) {
                btFound = true;
                //CONNECT
                connectToBTDevice();
                break;
            }
        }
        if (!btFound) {
            playSound("notavailable");
            Toast.makeText(getBaseContext(), "System is not available! please try again.", Toast.LENGTH_LONG).show();

        }

    }

    private void connectToBTDevice() {

        new Thread() {
            public void run() {
                boolean fail = false;

                BluetoothDevice device = btAdapter.getRemoteDevice(address);
                try {
                    btSocket = device.createInsecureRfcommSocketToServiceRecord(uuid);

                } catch (IOException e) {
                    fail = true;
                }
                try {
                    btSocket.connect();
                } catch (IOException e) {

                    try {
                        fail = true;
                        btSocket.close();
                    } catch (IOException e1) {
                    }
                }

                if (!fail) {
                    btConnectedThread = new ConnectThread(btSocket);
                    btConnectedThread.start();
                    btHandler.obtainMessage(1).sendToTarget();
                }

            }
        }.start();
    }

    final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                btArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                btArrayAdapter.notifyDataSetChanged();

            }
        }
    };

    public class ConnectThread extends Thread {
        private final BluetoothSocket Socket;
        private final InputStream in;
        private final OutputStream out; // we don't send data via bluetooth, we're only receiving.

        public ConnectThread(BluetoothSocket Socket) {
            this.Socket = Socket;
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                inputStream = Socket.getInputStream();
                outputStream = Socket.getOutputStream();
            } catch (IOException e) {

            }

            in = inputStream;
            out = outputStream;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = in.available();
                    if (bytes != 0) {
                        bytes = in.read(buffer);
                        String Message = new String(buffer, 0, bytes);
                        btHandler.obtainMessage(0, bytes, -1, Message).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}



