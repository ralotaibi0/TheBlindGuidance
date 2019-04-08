package com.seniorproject.theblindguidance;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothDevice;


//Google
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.Places;
import com.google.android.gms.common.api.ApiException;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.model.DirectionsResult;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //View
    private Button connect;
    private Button mapClick;
    private TextView dataView;

    //BT
    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket = null;
    private ArrayAdapter<String> btArrayAdapter;
    private ConnectThread btConnectedThread;
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final String address = "00:12:02:09:05:73";
    private final String name = "linvor";

    //MediaPlayer
    private MediaPlayer MP;

    //Sensors
    private long rightTime = 0, leftTime = 0, stopTime = 0, currentTime = 0;
    private StringBuilder sensorsData;

    //Maps
    private boolean GPSPermission = false;
    private FusedLocationProviderClient FLPClient;
    private GoogleMap map;
    private Location loc;
    private PlacesClient placesClient = null;
    private List<PlaceLikelihood> nearbyPlaces;
    private GeoApiContext GAContext = null;////////////


    //Text-To-Speech and Speech-To-Text
    private TextToSpeech TTS;
    private Intent STTIntent;


    //Other data
    private Handler myHandler;
    private Handler updateLocHandler = new Handler();
    private Runnable updateLocRunnable;
    private String messageType = "";
    private int NOPlaces = 0;
    private int NOPlacesOffered = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // prepare the variables.
        connect = (Button) findViewById(R.id.Connect);
        dataView = (TextView) findViewById(R.id.Data);
        mapClick = (Button) findViewById(R.id.mapClick);
        dataView.setText("Blind Guidance System is Unavailable.");
        dataView.setTextColor(Color.RED);
        sensorsData = new StringBuilder();
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        FLPClient = LocationServices.getFusedLocationProviderClient(this);
        Places.initialize(getApplicationContext(), getString(R.string.places_API));
        placesClient = Places.createClient(this);


        // Search for bluetooth device and nearby places after 10 seconds of launching the application.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                connectBT();
                getNearbyPlaces();
            }
        }, 10000);


        // Initialize the map
        initGoogleMap();

        // Initialize Text-To-Speech
        initTTS();

        // Initialize Speech-To-Text
        initSTT();


        // OnClickListeners
        mapClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nearbyPlaces != null) {
                    myHandler.obtainMessage(2, null).sendToTarget();
                } else {
                    getNearbyPlaces();
                }
            }
        });
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connect.getText().equals("Disconnect")) {
                    DisconnectBT();
                } else {
                    connectBT();
                }
            }
        });


        myHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == 0) { // Receiving messages from sensors
                    String Message = (String) msg.obj;
                    sensorsData.append(Message);
                    int endOfMessage = sensorsData.lastIndexOf("*");
                    if (endOfMessage > 3) {
                        if ((sensorsData.charAt(0) == '#') && (sensorsData.length() < 10)) {
                            String actualData = sensorsData.substring(1, endOfMessage);
                            String[] sensors = actualData.split("!");
                            int rightSensor = Integer.parseInt(sensors[0]);
                            int leftSensor = Integer.parseInt(sensors[1]);
                            currentTime = System.currentTimeMillis();
                            if ((leftTime <= currentTime) && (rightTime <= currentTime) && (stopTime <= currentTime)) {
                                if ((rightSensor < 70 && rightSensor > 2) && (leftSensor < 70 && leftSensor > 2)) {
                                    stopTime = System.currentTimeMillis() + 5000;
                                    playSound("stop");
                                } else {
                                    if (rightSensor < 70 && rightSensor > 2) {
                                        leftTime = System.currentTimeMillis() + 5000;
                                        playSound("slightlyleft");
                                    } else if (leftSensor < 70 && leftSensor > 2) {
                                        rightTime = System.currentTimeMillis() + 5000;
                                        playSound("slightlyright");
                                    }

                                }
                            }
                        }
                        sensorsData.delete(0, sensorsData.length());
                    }
                }

                if (msg.what == 1) { // when connection to bluetooth device is established.
                    playSound("connected");
                    connect.setText("Disconnect");
                    dataView.setText("Connected to The Blind Guidance System.");
                    dataView.setTextColor(Color.BLACK);
                }

                if (msg.what == 2) { // offer nearby places
                    if (nearbyPlaces != null) {
                        speak("There are some nearby places, do you want to hear their names?", "nearbyPlaces", true);
                    } else {
                        speak("There are no nearby places. ", "none", false);
                    }
                }


            }


        };

        updateUserLocation();

    }

    final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName() + "\n" + device.getAddress();
                btArrayAdapter.add(deviceName);
                btArrayAdapter.notifyDataSetChanged();
                if (deviceName.equals(name + "\n" + address)) {
                    dataView.setText("Blind Guidance System is Available.");
                    dataView.setTextColor(Color.GREEN);
                    connectToBTDevice();
                    return;
                }
                dataView.setText("Blind Guidance System is Unavailable.");
                dataView.setTextColor(Color.RED);
            }
        }
    };

    private void initGoogleMap() {
        //Check GPS Permissions
        checkGPS();
        //Load the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                map = googleMap;
                updateLocation();
                getDeviceLocation();
            }
        });
        if (GAContext == null) {
            GAContext = new GeoApiContext.Builder()
                    .apiKey(getString(R.string.google_API))
                    .build();
        }
    }

    private void initTTS() {
        TTS = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = TTS.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {

                } else {
                    TTS.setSpeechRate((float) 0.8);
                    TTS.setPitch((float) 1.2);
                    speak("Hello", "none", false);
                }
            }
        });
    }

    private void initSTT() {
        checkVoicePermission();
        STTIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        STTIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        STTIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
    }

    private void startTrip(com.google.maps.model.LatLng destination) {
        Uri googleMapsIntentUri = Uri.parse("google.navigation:q=" + destination.lat + "," + destination.lng + "&mode=w");
        Intent googleMapIntent = new Intent(Intent.ACTION_VIEW, googleMapsIntentUri);
        googleMapIntent.setPackage("com.google.android.apps.maps");
        if (googleMapIntent.resolveActivity(getPackageManager()) != null)
            startActivity(googleMapIntent);
        else
            speak("you need to install google maps in order to get directions.", "none", false);
    }

    private void calculateDirections(com.google.maps.model.LatLng destination) {

        if (loc != null) {
            DirectionsApiRequest directions = new DirectionsApiRequest(GAContext);
            directions.origin(new com.google.maps.model.LatLng(loc.getLatitude(), loc.getLongitude()));
            directions.alternatives(true);

            directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
                @Override
                public void onResult(DirectionsResult result) {
                    speak("its going to take you " + result.routes[0].legs[0].duration.inSeconds + " Seconds", "none", false);
                    startTrip(destination);
                }

                @Override
                public void onFailure(Throwable e) {
                    Log.e("calculatedirections", e.getMessage());
                }
            });
        }
    }

    private void sayPlacesNames() {
        if (NOPlaces > NOPlacesOffered) {
            speak(nearbyPlaces.get(NOPlacesOffered).getPlace().getName(), "none", false);
            speak("do you want to go there?", "offerPlace", true);
        } else {
            NOPlacesOffered = 0;
        }
    }

    private void getNearbyPlaces() {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            FindCurrentPlaceRequest request = FindCurrentPlaceRequest.builder(placeFields).build();
            placesClient.findCurrentPlace(request).addOnSuccessListener(((response) -> {
                nearbyPlaces = response.getPlaceLikelihoods();
                NOPlaces = nearbyPlaces.size();
                myHandler.obtainMessage(2, null).sendToTarget();

            })).addOnFailureListener((exception) -> {
                if (exception instanceof ApiException) {
                    ApiException apiException = (ApiException) exception;
                    Log.e("nearbyplaces", "Place not found: " + apiException.getStatusCode());
                    speak("please check your internet connection, if its working properly, if it is, , then there are no places near your location", "none", false);
                    speak("if you want to try to search for nearby places, just click on the screen.", "none", false);
                }
            });
        } else {
            getLocationPermission();
        }

    }

    private void updateUserLocation() {
        updateLocHandler.postDelayed(updateLocRunnable = new Runnable() {
            @Override
            public void run() {
                getDeviceLocation();
                updateLocHandler.postDelayed(updateLocRunnable, 7000);
            }
        }, 7000);
    }

    private void updateLocation() {
        if (map == null) {
            return;
        }
        try {
            if (GPSPermission) {
                map.setMyLocationEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
                loc = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
        try {
            if (GPSPermission) {
                Task locationResult = FLPClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            loc = (Location) task.getResult();
                            moveCamera(new LatLng(loc.getLatitude(), loc.getLongitude()), 15);
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                        } else {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 2));
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void moveCamera(LatLng coordinates, int zoom) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates, zoom));
    }

    private void checkGPS() {
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //Check if GPS is enabled or not
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent enableGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(enableGPS, 10);
        } else {
            GPSPermission = true;
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            GPSPermission = true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 10);
        }
    }

    private void checkVoicePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                finish();
            }
        }
    }

    private void connectBT() {
        // Enable bluetooth if its not enabled
        if (!btAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, 1);
        }
        if (!btAdapter.isDiscovering()) {
            registerReceiver(btReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            btAdapter.startDiscovery();
        }
    }

    private void DisconnectBT() {
        if (btConnectedThread.isAlive()) {
            btConnectedThread = null;
            dataView.setText("Disconnected.");
            connect.setText("Connect");
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                    myHandler.obtainMessage(1).sendToTarget();
                }

            }
        }.start();
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

        if ((MP != null) && !MP.isPlaying()) {
            MP.start();
        }
    }

    private void speak(String text, String type, boolean userExpectedToAnswer) {
        while (TTS.isSpeaking()) {

        }
        TTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        if (userExpectedToAnswer) {
            while (TTS.isSpeaking()) {

            }
            listen(type);
        }
    }

    private void listen(String type) {
        messageType = type;
        if (STTIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(STTIntent, 50);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 10: {
                if (!GPSPermission) {
                    getLocationPermission();
                }
                break;
            }
            case 50: {
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> r = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (r != null) {
                        if (messageType.equals("nearbyPlaces")) {
                            if (r.get(0).contains("yes")) {
                                speak("Okay.", "none", false);
                                speak("if you want directions, just say yes, if you want to hear the next location, say no, and if you want to stop me from asking you just say stop.", "none", false);
                                sayPlacesNames();
                            } else if (r.get(0).contains("no")) {
                                speak("Okay, if you want to go to one of the nearby places, just click on the screen.", "none", false);
                            } else {
                                speak("please say yes or no.", "nearbyPlaces", true);
                            }
                        } else if (messageType.equals("offerPlace")) {
                            if (r.get(0).contains("yes")) {
                                speak("Okay.", "none", false);
                                com.google.maps.model.LatLng tempLoc = new com.google.maps.model.LatLng(nearbyPlaces.get(NOPlacesOffered).getPlace().getLatLng().latitude, nearbyPlaces.get(NOPlacesOffered).getPlace().getLatLng().longitude);
                                calculateDirections(tempLoc);
                            } else if (r.get(0).contains("no")) {
                                speak("Okay.", "none", false);
                                NOPlacesOffered++;
                                sayPlacesNames();
                            } else if (r.get(0).contains("stop")) {
                                NOPlacesOffered = 0;
                                speak("Okay.", "none", false);
                            } else {
                                speak("please say yes or no, or stop.", "offerPlace", true);
                            }
                        }
                    }
                    break;
                }

                break;
            }
        }
        updateLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        GPSPermission = false;
        switch (requestCode) {
            case 10: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    GPSPermission = true;
                }
            }
        }
    }

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
                        myHandler.obtainMessage(0, bytes, -1, Message).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }


}






