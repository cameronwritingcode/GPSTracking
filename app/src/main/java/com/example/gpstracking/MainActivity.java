package com.example.gpstracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import android.Manifest;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

public class MainActivity extends AppCompatActivity {


    public static final int DEFAULT_UPDATE_INTERVAL = 30;
    public static final int FASTEST_UPDATE_INTERVAL = 5;
    public static final int PERMISSIONS_FINE_LOCATION = 99;

    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address, tv_wayPointCounts;

    Button btn_newWayPoint, btn_showWayPointList;

    Location currentLocation;

    List<Location> savedLocations;



    Switch sw_locationupdates, sw_gps;

    //Config file for FusedLocationProvider Client. Changes behavior
    LocationRequest locationRequest;

    LocationCallback locationCallback;

    //Google API for location services. The core of this app
    FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_speed = findViewById(R.id.tv_speed);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        tv_address = findViewById(R.id.tv_address);
        tv_wayPointCounts = findViewById(R.id.tv_countOfCrumbs);

        sw_gps = findViewById(R.id.sw_gps);
        sw_locationupdates = findViewById(R.id.sw_locationsupdates);

        locationRequest = LocationRequest.create();

        locationRequest.setInterval(DEFAULT_UPDATE_INTERVAL * 30);
        locationRequest.setFastestInterval(1000 * FASTEST_UPDATE_INTERVAL);

        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        btn_newWayPoint = findViewById(R.id.btn_newWayPoints);
        btn_showWayPointList = findViewById(R.id.btn_showWayPointList);

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                //save the location
                updateUIValues(locationResult.getLastLocation());
            }
        };

        /*
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult ) {
                if( locationResult == null ) return;

                for( Location location : locationResult.getLocations() ) {
                    if( location != null )
                    {

                    }
                }
            }
        }

         */

        btn_showWayPointList.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v )
            {
                Intent i = new Intent(MainActivity.this, ShowSavedLocationsList.class);
                startActivity(i);
            }
         });

        btn_newWayPoint.setOnClickListener( new View.OnClickListener() {
                public void onClick( View v ) {
                    //get the gps location

                    //add to the list of saved locations

                    MyApplication myApplication = (MyApplication) getApplicationContext();
                    savedLocations = myApplication.getMyLocations();
                    savedLocations.add( currentLocation );

            }
        });

        sw_gps.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (sw_gps.isChecked()) {
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    tv_sensor.setText("Using GPS Sensors ");
                } else {
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    tv_sensor.setText("Using Towers + WIFI ");
                }
            }
        });

        sw_locationupdates.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sw_locationupdates.isChecked()) {
                    startLocationUpdates();
                } else {
                    stopLocationUpdates();
                }
            }
        }));

        updateGPS();

    } //end of onCreate method

    private void startLocationUpdates() {
        tv_updates.setText("Location is being tracked");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        updateGPS();
    }

    private void stopLocationUpdates() {
        tv_updates.setText("Location is not being tracked");
        tv_lat.setText("Not tracking location");
        tv_lon.setText("Not tracking location");
        tv_address.setText("Not tracking location");
        tv_accuracy.setText("Not tracking location");
        tv_speed.setText("Not tracking location");
        tv_altitude.setText("Not tracking location");
        tv_sensor.setText("Not tracking location");

        fusedLocationProviderClient.removeLocationUpdates(locationCallback);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch( requestCode ) {
            case PERMISSIONS_FINE_LOCATION:;
            if( grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                updateGPS();
            }
            else {
                Toast.makeText( this, "This app requires permission to be granted in order to work properly", Toast.LENGTH_SHORT ).show();
                finish();
            }
        }
    }

    private void updateGPS() {
        //get permission to track user's GPS
        //get current location from fused client
        //update the UI - set the properties in text view items

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient( MainActivity.this );

        if( ActivityCompat.checkSelfPermission ( this, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED  ) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    //we got permissions. Put values of location into the UI

                    if( location != null )
                    {
                        updateUIValues( location );
                        currentLocation = location;

                    }


                }
            });
        }
        else {
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
                requestPermissions( new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, PERMISSIONS_FINE_LOCATION );
            }
        }
    }

    private void updateUIValues(Location location) {

        //update all of the text view objects with a new location.
        tv_lat.setText( String.valueOf(location.getLatitude()));
        tv_lon.setText( String.valueOf( location.getLongitude()));
        tv_accuracy.setText( String.valueOf( location.getAccuracy()));

        if( location.hasAltitude() )
        {
            tv_altitude.setText( String.valueOf( location.getAltitude()));
        }
        else {
            tv_altitude.setText( "Altitude not available" );
        }

        if( location.hasSpeed() )
        {
            tv_speed.setText( String.valueOf( location.getSpeed()));
        }
        else {
            tv_speed.setText( "Speed not avialable" );
        }

        Geocoder geocoder = new Geocoder( MainActivity.this );

        try {
            List<Address> addresses = geocoder.getFromLocation( location.getLatitude(), location.getLongitude(), 1 );
            tv_address.setText( addresses.get(0).getAddressLine(0));
        }
        catch( Exception e )
        {
            tv_address.setText( "No address found" );
        }

        MyApplication myApplication = (MyApplication) getApplicationContext();
        savedLocations = myApplication.getMyLocations();

        tv_wayPointCounts.setText( Integer.toString(savedLocations.size()));

    }


}