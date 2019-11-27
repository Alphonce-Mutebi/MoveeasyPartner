package com.blume.moveeasypartner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.blume.moveeasypartner.directionhelpers.FetchURL;
import com.blume.moveeasypartner.directionhelpers.TaskLoadedCallback;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import com.google.android.gms.maps.model.Marker;import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener, TaskLoadedCallback {

    private static final int REQUEST_LOCATION_PERMISSION = 99;
    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation, pickupLocation, destinationLocation;
    LocationRequest mLocationRequest;
    Marker currentUserLocationMarker, destinationMarker;
    Polyline currentPolyline;
    LatLng destinationLatLng, pickupLatLng;
    private MarkerOptions place1;
    private String customerId = "";
    String pickupPlace, destinationPlace, Price;
    float toDestination[] = new float[10];

    Switch driverAvailable;
    TextView nameView, phoneView, amtView, destView;

    Button dropoffButton;

    //coordinator layout from map activity
    CoordinatorLayout coordinatorLayout;

    //Dialog
    Dialog myDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        //initialize dialog
        myDialog = new Dialog(this);

        //finding dropoff button
        dropoffButton = findViewById(R.id.dropoffButton);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        driverAvailable = findViewById(R.id.availabilitySwitch);

        coordinatorLayout = findViewById(R.id.maplayout);

        driverAvailable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(driverAvailable.isChecked()) {
                    getAssignedCustomer();


                    Toast.makeText(MapsActivity.this, "Getting your requests...", Toast.LENGTH_LONG).show();
                }
                else{
                    String userId = FirebaseAuth.getInstance().getUid();
                    DatabaseReference driverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Driver Availability");

                    GeoFire geoFire = new GeoFire(driverAvailabilityRef);
                    geoFire.removeLocation(userId);
                    Toast.makeText(MapsActivity.this, "Status changed to unavailable", Toast.LENGTH_LONG).show();
                }
            }
        });


        getAssignedCustomer();

        dropoffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordRide();
                endRide();
            }
        });

    }

    private void getAssignedCustomer() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Drivers").child(driverId).child("customerRequest").child("customerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists()) {
                    customerId = dataSnapshot.getValue().toString();
                    getAssignedCustomerPickupLocation();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    Marker pickupMarker;
    private DatabaseReference assignedCustomerPickupLocationRef;
    ValueEventListener assignedCustomerPickupLocationRefListener;
    private void getAssignedCustomerPickupLocation() {

        assignedCustomerPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("CustomerRequest").child(customerId).child("l");
        assignedCustomerPickupLocationRefListener = assignedCustomerPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && !customerId.equals("")){

                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if(map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    pickupLatLng = new LatLng(locationLat, locationLng);
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("pickup location"));
                    Toast.makeText(MapsActivity.this, "New Request, Head to the pick-up location", Toast.LENGTH_LONG).show();
                    onLocationChanged(mLastLocation);

                    //Checking whether driver is near the pickup location and prompting pickup item
                    pickupLocation = new Location("");
                    pickupLocation.setLatitude(pickupLatLng.latitude);
                    pickupLocation.setLongitude(pickupLatLng.longitude);
                    float distance = mLastLocation.distanceTo(pickupLocation);

                    if (distance<500){
                        Toast.makeText(MapsActivity.this, "You have arrived at pick up location.", Toast.LENGTH_LONG).show();

                        getAssignedCustomerDestination();

                    }else{
                        Toast.makeText(MapsActivity.this, "Drive to the pickup location", Toast.LENGTH_LONG).show();
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }


    private DatabaseReference assignedCustomerDestinationLocationRef;
    ValueEventListener assignedCustomerDestinationLocationRefListener;
    private void getAssignedCustomerDestination(){
        assignedCustomerDestinationLocationRef = FirebaseDatabase.getInstance().getReference().child("RequestDestination").child(customerId).child("l");
        assignedCustomerDestinationLocationRefListener = assignedCustomerDestinationLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && !customerId.equals("")) {

                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    destinationLatLng = new LatLng(locationLat, locationLng);


                    DatabaseReference amountRef = FirebaseDatabase.getInstance().getReference().child("RequestDestination").child(customerId);
                    amountRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()) {
                                //dialog
                                Button btnConfirmpickup;

                                myDialog.setContentView(R.layout.custompopup);

                                //Finding pop up components
                                btnConfirmpickup = (Button) myDialog.findViewById(R.id.pickupButton);
                                amtView = myDialog.findViewById(R.id.amountvalue);
                                nameView = myDialog.findViewById(R.id.nameValue);
                                phoneView = myDialog.findViewById(R.id.phoneValue);
                                destView = myDialog.findViewById(R.id.destinationValue);

                                if(dataSnapshot.child("Price") != null){
                                    String price = dataSnapshot.child("Price").getValue().toString();
                                    amtView.setText(price);
                                    Price = price;
                                }
                                if(dataSnapshot.child("Destination") != null){
                                    String place = dataSnapshot.child("Destination").getValue().toString();
                                    String[] places = place.split(",");
                                    destView.setText(places[0]);
                                    destinationPlace = places[0];
                                }

                                if(dataSnapshot.child("Pickup") != null){
                                    String place = dataSnapshot.child("Pickup").getValue().toString();
                                    String[] places = place.split(",");
                                    pickupPlace = places[0];
                                }

                                getAssignedCustomerInfo();

                                btnConfirmpickup.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        //Setting destination Location
                                        destinationLocation = new Location("");
                                        destinationLocation.setLongitude(destinationLatLng.latitude);
                                        destinationLocation.setLongitude(destinationLatLng.longitude);

                                        //Adding destination marker and drawing polyline
                                        destinationMarker = mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destination location"));
                                        new FetchURL(MapsActivity.this).execute(getUrl(pickupMarker.getPosition(), destinationMarker.getPosition(), "driving"), "driving");
                                        Toast.makeText(MapsActivity.this, "Drive to the destination...", Toast.LENGTH_LONG).show();
                                        //Camera to zoom out
                                        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

                                       //setting confirm dropoff button to visible
                                        dropoffButton.setVisibility(View.VISIBLE);


                                        myDialog.dismiss();
                                    }
                                });
                                myDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                                myDialog.show(); // review error for submitting another request


                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });



    }
    DatabaseReference mCustomerDatabase;
    ValueEventListener mCustomerDatabaseRefListener;
    private void getAssignedCustomerInfo(){
        mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(customerId);
        mCustomerDatabaseRefListener = mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    if(dataSnapshot.child("Username") != null){
                        nameView.setText(dataSnapshot.child("Username").getValue().toString());
                    }
                    if(dataSnapshot.child("Phone") != null){
                        phoneView.setText(dataSnapshot.child("Phone").getValue().toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void endRide(){
        erasePolylines();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Drivers").child(userId).child("customerRequest");
        driverRef.removeValue();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(customerId);
        customerId="";

        DatabaseReference destinationRef = FirebaseDatabase.getInstance().getReference("RequestDestination");
        destinationRef.removeValue();


        if(pickupMarker != null){
            pickupMarker.remove();
        }
        if(destinationMarker != null){
            destinationMarker.remove();
        }
        if (assignedCustomerDestinationLocationRefListener != null){
            assignedCustomerDestinationLocationRef.removeEventListener(assignedCustomerPickupLocationRefListener);
        }
        if (assignedCustomerPickupLocationRefListener != null){
            assignedCustomerPickupLocationRef.removeEventListener(assignedCustomerPickupLocationRefListener);
        }
        if ( mCustomerDatabaseRefListener != null){
            mCustomerDatabase.removeEventListener(mCustomerDatabaseRefListener);
        }

        nameView.setText("");
        phoneView.setText("");
        amtView.setText("");
        dropoffButton.setVisibility(View.INVISIBLE);

    }

    private void erasePolylines() {
        currentPolyline.remove();
    }

    private void recordRide(){
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Drivers").child(userId).child("history");
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child(customerId).child("history");
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("History");
        String requestId = historyRef.push().getKey();
        driverRef.child(requestId).setValue(true);
        customerRef.child(requestId).setValue(true);

        //getting ride Distance
        Location.distanceBetween(pickupLatLng.latitude, pickupLatLng.longitude, destinationLatLng.latitude, destinationLatLng.longitude, toDestination);

        HashMap map = new HashMap();
        map.put("driver", userId);
        map.put("customer", customerId);
        map.put("price", Price);
        map.put("distance", toDestination[0]);
        map.put("timestamp", getCurrentTimestamp());
        map.put("pickup", pickupPlace);
        map.put("destination", destinationPlace);
        map.put("location/from/lat", pickupLatLng.latitude);
        map.put("location/from/lng", pickupLatLng.longitude);
        map.put("location/to/lat", destinationLatLng.latitude);
        map.put("location/to/lng", destinationLatLng.longitude);
        historyRef.child(requestId).updateChildren(map);

        onLocationChanged(mLastLocation);

    }

    private Long getCurrentTimestamp() {
        Long timestamp = System.currentTimeMillis()/1000;
        return timestamp;
    }


    /**
     *--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
     * Map specific functions start here
     */

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        enableMyLocation();
        buildGoogleApiClient();

    }

    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION:
                if (grantResults.length > 0
                        && grantResults[0]
                        == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation();
                    break;
                }
        }
    }
    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
        return url;
    }



    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;

        if(currentUserLocationMarker != null){
            currentUserLocationMarker.remove();
        }


        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        MarkerOptions uMarkerOptions = new MarkerOptions();
        uMarkerOptions.position(userLatLng);
        uMarkerOptions.title("Current Location");
        uMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));

        mMap.moveCamera(CameraUpdateFactory.newLatLng(userLatLng));
        mMap.animateCamera(CameraUpdateFactory.zoomBy(18));

        if(mGoogleApiClient != null){
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }

        String userId = FirebaseAuth.getInstance().getUid();
        DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("Driver Availability");
        DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("Drivers Working");
        GeoFire geoFireAvailable = new GeoFire(refAvailable);
        GeoFire geoFireWorking = new GeoFire(refWorking);

        switch (customerId){
            case "":
                geoFireWorking.removeLocation(userId);
                geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                break;

            default:
                geoFireAvailable.removeLocation(userId);
                geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                break;
        }

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, MapsActivity.this);
        }
        else{
            Toast.makeText(this, "Location Disabled!", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    /*
    @Override
    protected void onStop() {
        super.onStop();
        String userId = FirebaseAuth.getInstance().getUid();
        DatabaseReference driverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Driver Availability");

        GeoFire geoFire = new GeoFire(driverAvailabilityRef);
        geoFire.removeLocation(userId);

    }
    */
    @Override
    public void onTaskDone(Object... values) {
        if (currentPolyline != null)
            currentPolyline.remove();
        currentPolyline = mMap.addPolyline((PolylineOptions) values[0]);
    }


}