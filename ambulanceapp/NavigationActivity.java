package me.ajaybala.ambulanceapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.geofire.util.GeoUtils;
import com.fxn.stash.Stash;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.ajaybala.ambulanceapp.models.Signal;
import me.ajaybala.ambulanceapp.utils.Helper;
import me.ajaybala.ambulanceapp.R;

public class NavigationActivity  extends FragmentActivity implements OnMapReadyCallback, TextToSpeech.OnInitListener {

    LatLng origin, destination;
    int totalTime, totalDistance;
    List<LatLng> markers = new ArrayList<>();
    private GoogleMap mMap;
    RelativeLayout rootRL;
    private BitmapDescriptor currentLocationIcon;
    private Marker currentLocationMarker;

    ImageView increaseIV,decreaseIV, alertIconIV, playPauseIV;
    TextView titleTV,valueTV, alertTextTV;
    LinearLayout alertLL;

    List<Signal> signals = new ArrayList<>();
    LatLng currentPos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        getDirectionsData();
        initMap();
        initUI();
        initGeoFire();
        initTTS();
        valueTV.setText(speed*3+" Km/hr");
        increaseIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speed+=5;
                valueTV.setText(speed*3+" Km/hr");
            }
        });

        decreaseIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(speed-5>0){
                    speed-=5;
                    valueTV.setText(speed*3+" Km/hr");
                }
            }
        });

        playPauseIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    if(isPlaying){
                        isPlaying=false;
                        playPauseIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_black_24dp));
                        valueAnimator.pause();
                    }else{
                        isPlaying=true;
                        playPauseIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_black_24dp));
                        valueAnimator.resume();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

    }

    void initUI(){
        currentLocationIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_ambulance_pin);
        rootRL = findViewById(R.id.rootRL);
        increaseIV = findViewById(R.id.increaseIV);
        decreaseIV = findViewById(R.id.decreaseIV);
        titleTV = findViewById(R.id.titleTV);
        valueTV = findViewById(R.id.valueTV);
        alertLL = findViewById(R.id.alertLL);
        alertIconIV = findViewById(R.id.alertIconIV);
        alertTextTV = findViewById(R.id.alertTextTV);
        playPauseIV = findViewById(R.id.playPauseIV);
        ViewGroup layout = (ViewGroup) findViewById(R.id.rootRL);
        LayoutTransition layoutTransition = layout.getLayoutTransition();
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
    }

    void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    void getDirectionsData(){
        origin = (LatLng) Stash.getObject("origin",LatLng.class);
        destination = (LatLng) Stash.getObject("destination",LatLng.class);
        totalDistance = Stash.getInt("distance");
        totalTime = Stash.getInt("travelTime");
        markers.addAll(Stash.<LatLng>getArrayList("markers",LatLng.class));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //mMap.setPadding(0,0,0, (int) (rootRL.getHeight()/1.2f));
        mMap.setBuildingsEnabled(false);
        mMap.setTrafficEnabled(false);
        /*CustomInfoWindowAdapter adapter = new CustomInfoWindowAdapter(NavigationActivity.this);
        mMap.setInfoWindowAdapter(adapter);*/
        try {
            /*boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.map_style));

            if (!success) {
                Log.e("hello", "Style parsing failed.");
            }*/
        } catch (Resources.NotFoundException e) {
            Log.e("hello", "Can't find style. Error: ", e);
        }

        drawRoute();
        animateMarker();

    }

    int index=0,next=0;
    LatLng newPos;
    long dura_speed=1000;
    long speed=15;
    float zoomLevel=18;
    double distanceTravelled=0;
    void animateMarker(){
        currentLocationMarker = mMap.addMarker(new MarkerOptions().icon(currentLocationIcon).flat(true).position(markers.get(0)));
        final float bearing = (float) Helper.getBearing(origin,markers.get(1));
        currentLocationMarker.setRotation(bearing);
        animateCamera(markers.get(0),zoomLevel, bearing);
        startSimulation();
    }

    boolean isPlaying = true;
    ValueAnimator valueAnimator;
    float prevbearingValue = 0;
    boolean flag=true;
    float currentBearing = 0;
    void startSimulation(){
        try {
            final LatLng startPosition, endPosition;
            if (index+1 < markers.size()) {
                next = index + 1;
                startPosition = markers.get(index);
                endPosition = markers.get(next);
                System.out.println("Distance " + Helper.distance(startPosition.latitude, startPosition.longitude, endPosition.latitude, endPosition.longitude));
                double distance = Helper.distance(startPosition.latitude, startPosition.longitude, endPosition.latitude, endPosition.longitude);
                distanceTravelled+=distance;
                dura_speed = (long) Math.ceil((distance*1000 / speed)*1000);
                System.out.println("Time 1 " + distance*1000 / speed);
                System.out.println("Time 2 " + dura_speed);
                valueAnimator = ValueAnimator.ofFloat(0, 1);
                valueAnimator.setDuration(dura_speed);
                valueAnimator.setInterpolator(new LinearInterpolator());
                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        double v = animation.getAnimatedFraction();
                        double lng = v * endPosition.longitude + (1 - v) * startPosition.longitude;
                        double lat = v * endPosition.latitude + (1 - v) * startPosition.latitude;
                        newPos = new LatLng(lat, lng);
                        currentPos = newPos;
                        currentLocationMarker.setPosition(newPos);
                        currentLocationMarker.setAnchor(0.5f, 0.5f);
                        checkSignals();
                        final float newBearingValue = (float) Helper.getBearing(startPosition, newPos);
                        currentBearing = newBearingValue;
                        if (newBearingValue != 0.0) {
                            if (prevbearingValue != newBearingValue) {
                                if (flag) {
                                    flag = false;
                                    updateBearingValues(newBearingValue);
                                }
                                currentLocationMarker.setRotation(newBearingValue);
                                moveCamera(newPos, zoomLevel, prevbearingValue);
                            } else {
                                moveCamera(newPos, zoomLevel, newBearingValue);
                            }
                        }
                    }
                });
                valueAnimator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        index++;
                        System.out.println("Distance travelled "+distanceTravelled*1000);
                        if(distanceTravelled*1000>300){
                            distanceTravelled=0;
                            geoQuery.setCenter(new GeoLocation(markers.get(index).latitude,markers.get(index).longitude));
                        }

                        startSimulation();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                valueAnimator.start();

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    void updateBearingValues(float newBearingValue){
        final ValueAnimator valueAnimator = ValueAnimator.ofFloat(prevbearingValue, newBearingValue);
        valueAnimator.setDuration(500);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                flag=true;
            }
        });
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                prevbearingValue= (float) animation.getAnimatedValue();
            }
        });
        valueAnimator.start();
    }



    private void animateCamera(@NonNull LatLng latLng,float zoom,float bearing) {
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(getCameraPositionWithBearing(latLng,zoom,bearing)));
    }
    private void moveCamera(@NonNull LatLng latLng,float zoom,float bearing) {
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(getCameraPositionWithBearing(latLng,zoom,bearing)));
    }

    @NonNull
    private CameraPosition getCameraPositionWithBearing(LatLng latLng,float zoom,float bearing) {
        return new CameraPosition.Builder().target(latLng).bearing(bearing).zoom(zoom).build();
    }

    void drawRoute(){
        PolylineOptions lineOptions = null;
        lineOptions = new PolylineOptions();
        lineOptions.addAll(markers);
        lineOptions.width(12);
        lineOptions.color(getResources().getColor(R.color.colorAccent));

        // Drawing polyline in the Google Map
        if(lineOptions != null) {
            mMap.addPolyline(lineOptions);
        }
    }

    DatabaseReference ref;
    DatabaseReference ref1;
    GeoFire geoFire;
    GeoQuery geoQuery;
    String eastKey = "-M1tUyAXjlhCVbQ4n1fb";
    String westKey = "-M1tUAeg91h6iaiBJL-7";
    String northKey = "-M1tT_YJAEzktUcx6FMn";
    String southKey = "-M1tULfL4iMb2gVJDW5g";
    private void initGeoFire(){
        ref = FirebaseDatabase.getInstance().getReference("signals");
        ref1 = FirebaseDatabase.getInstance().getReference();
        ref1.child("testing").setValue("abcd");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                System.out.println("database testing "+dataSnapshot.exists());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        geoFire = new GeoFire(ref);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(markers.get(0).latitude, markers.get(0).longitude), 0.5);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                System.out.println(String.format("Key %s entered the search area at [%f,%f]", key, location.latitude, location.longitude));
                //if(PolyUtil.isLocationOnPath(new LatLng(location.latitude,location.longitude),markers,true,20)) {
                    addsignalToMap(key, location);
                //}
            }

            @Override
            public void onKeyExited(String key) {
                System.out.println(String.format("Key %s is no longer in the search area", key));
                removesignalFromMap(key);
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                System.out.println(String.format("Key %s moved within the search area to [%f,%f]", key, location.latitude, location.longitude));
            }

            @Override
            public void onGeoQueryReady() {
                System.out.println("All initial data has been loaded and events have been fired!");
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                System.err.println("There was an error with this query: " + error);
            }
        });
    }

    void addsignalToMap(String key,GeoLocation location){
        ref.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                System.out.println("testing inside direction request "+currentBearing);
                Signal signal = new Signal();
                signal.setKey(key);
                signal.setGeoLocation(location);
                Signal north = new Signal();
                Signal south = new Signal();
                Signal west = new Signal();
                Signal east = new Signal();
                north.setSignal_value(dataSnapshot.child(northKey).child("signal_value").getValue(Integer.class));
                south.setSignal_value(dataSnapshot.child(southKey).child("signal_value").getValue(Integer.class));
                west.setSignal_value(dataSnapshot.child(westKey).child("signal_value").getValue(Integer.class));
                east.setSignal_value(dataSnapshot.child(eastKey).child("signal_value").getValue(Integer.class));
                north.setDirection(dataSnapshot.child(northKey).child("direction").getValue(String.class));
                south.setDirection(dataSnapshot.child(southKey).child("direction").getValue(String.class));
                west.setDirection(dataSnapshot.child(westKey).child("direction").getValue(String.class));
                east.setDirection(dataSnapshot.child(eastKey).child("direction").getValue(String.class));
                north.setGeoLocation(new GeoLocation((double)dataSnapshot.child(northKey).child("l").child("0").getValue(),(double)dataSnapshot.child(northKey).child("l").child("1").getValue()));
                south.setGeoLocation(new GeoLocation((double)dataSnapshot.child(southKey).child("l").child("0").getValue(),(double)dataSnapshot.child(southKey).child("l").child("1").getValue()));
                west.setGeoLocation(new GeoLocation((double)dataSnapshot.child(westKey).child("l").child("0").getValue(),(double)dataSnapshot.child(westKey).child("l").child("1").getValue()));
                east.setGeoLocation(new GeoLocation((double)dataSnapshot.child(eastKey).child("l").child("0").getValue(),(double)dataSnapshot.child(eastKey).child("l").child("1").getValue()));
                north.setMarker(setSignalIcons(north));
                south.setMarker(setSignalIcons(south));
                west.setMarker(setSignalIcons(west));
                east.setMarker(setSignalIcons(east));
                signal.setNorth(north);
                signal.setSouth(south);
                signal.setWest(west);
                signal.setEast(east);
                signals.add(signal);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    Marker setSignalIcons(Signal signal){
        Marker marker = mMap.addMarker(
                new MarkerOptions()
                        .title("Signal")
                        .position(new LatLng(signal.getGeoLocation().latitude,signal.getGeoLocation().longitude)));
        System.out.println("signal testing value "+signal.getSignal_value());
        switch (signal.getSignal_value()){
            case 0:{
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                break;
            }
            case 1:{
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_orange));
                break;
            }
            case 2:
            case 3: {
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_green));
                break;
            }
        }
        return marker;
    }

    void removesignalFromMap(String key){
        for(int i=0;i<removedSignals.size();i++){
            if(removedSignals.get(i).getKey().equals(key)){
                //removedSignals.get(i).getMarker().remove();
                removedSignals.remove(i);
                //mMap.clear();
                break;
            }
        }
    }

    Signal northSignal;
    Signal southSignal;
    Signal eastSignal;
    Signal westSignal;
    String direction = "E";
    Signal getNearestsignal(){
        Signal nearest = null;
        int nearestIndex = 0;
        try{
            nearest = signals.get(0);
            double nearestDistance = GeoUtils.distance(nearest.getGeoLocation(),new GeoLocation(currentPos.latitude,currentPos.longitude));
            nearestDistance = Math.abs(nearestDistance);
            for(int i=0;i<signals.size();i++){
                double distance = GeoUtils.distance(signals.get(i).getGeoLocation(),new GeoLocation(currentPos.latitude,currentPos.longitude));
                distance = Math.abs(distance);
                if(distance<nearestDistance&&!signals.get(i).isAlerted()){
                    nearest = signals.get(i);
                    nearestIndex = i;
                }
            }

            return nearest;
        }catch (Exception e){
            //e.printStackTrace();
        }
        return null;
    }

    double signalDistanceTemp=0;
    boolean isDecreasing=false;
    double signalAlertThreshold=200;
    Signal nearestsignal;
    void checkSignals(){
        System.out.println("current bearing "+currentBearing);
        try {
            if (nearestsignal !=null) {
                double distance = GeoUtils.distance(nearestsignal.getGeoLocation(), new GeoLocation(currentPos.latitude, currentPos.longitude));
                distance = Math.abs(distance);
                /*System.out.println("nearest signal search area " + nearestsignal.getKey());
                System.out.println("nearest signal search area distance " + distance);
                System.out.println("nearest signal search area alert boolean " + isAlertOver);*/
                if (signalDistanceTemp > distance) {
                    isDecreasing = true;
                } else {
                    isDecreasing = false;
                }
                signalDistanceTemp = distance;
                if (distance < signalAlertThreshold) {
                    ref.child(nearestsignal.getKey()).child("mode").setValue(1);
                    if (distance < 100) {
                        switch (direction){
                            case "N":{
                                ref.child(nearestsignal.getKey()).child(northKey).child("signal_value").setValue(2);
                                nearestsignal.getNorth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_green));
                                break;
                            }
                            case "S":{
                                ref.child(nearestsignal.getKey()).child(southKey).child("signal_value").setValue(2);
                                nearestsignal.getSouth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_green));
                                break;
                            }
                            case "W":{
                                ref.child(nearestsignal.getKey()).child(westKey).child("signal_value").setValue(2);
                                nearestsignal.getWest().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_green));
                                break;
                            }
                            case "E":{
                                ref.child(nearestsignal.getKey()).child(eastKey).child("signal_value").setValue(2);
                                nearestsignal.getEast().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_green));
                                break;
                            }
                        }

                        isAlertOver = true;
                        tts.stop();
                        alertLL.setVisibility(View.GONE);
                        removesignal(nearestsignal.getKey());

                    } else {
                        if (isAlertOver&&isDecreasing) {

                            //signals.get(nearestIndex).setAlerted(true);
                            //nearestsignal.getMarker().showInfoWindow();

                            switch (direction){
                                case "N":{
                                    if(nearestsignal.getSouth().getSignal_value()==2){
                                        ref.child(nearestsignal.getKey()).child(southKey).child("signal_value").setValue(1);
                                        nearestsignal.getSouth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_orange));
                                    }else{
                                        ref.child(nearestsignal.getKey()).child(southKey).child("signal_value").setValue(0);
                                        nearestsignal.getSouth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                    }
                                    if(nearestsignal.getWest().getSignal_value()==2){
                                        ref.child(nearestsignal.getKey()).child(westKey).child("signal_value").setValue(1);
                                        nearestsignal.getWest().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_orange));
                                    }else{
                                        ref.child(nearestsignal.getKey()).child(westKey).child("signal_value").setValue(0);
                                        nearestsignal.getWest().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                    }
                                    if(nearestsignal.getEast().getSignal_value()==2){
                                        ref.child(nearestsignal.getKey()).child(eastKey).child("signal_value").setValue(1);
                                        nearestsignal.getEast().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_orange));
                                    }else{
                                        ref.child(nearestsignal.getKey()).child(eastKey).child("signal_value").setValue(0);
                                        nearestsignal.getEast().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                    }
                                    break;
                                }
                                case "S":{
                                    if(nearestsignal.getNorth().getSignal_value()==2){
                                        ref.child(nearestsignal.getKey()).child(northKey).child("signal_value").setValue(1);
                                        nearestsignal.getNorth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_orange));
                                    }else{
                                        ref.child(nearestsignal.getKey()).child(northKey).child("signal_value").setValue(0);
                                        nearestsignal.getNorth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                    }
                                    if(nearestsignal.getWest().getSignal_value()==2){
                                        ref.child(nearestsignal.getKey()).child(westKey).child("signal_value").setValue(1);
                                        nearestsignal.getWest().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_orange));
                                    }else{
                                        ref.child(nearestsignal.getKey()).child(westKey).child("signal_value").setValue(0);
                                        nearestsignal.getWest().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                    }
                                    if(nearestsignal.getEast().getSignal_value()==2){
                                        ref.child(nearestsignal.getKey()).child(eastKey).child("signal_value").setValue(1);
                                        nearestsignal.getEast().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_orange));
                                    }else{
                                        ref.child(nearestsignal.getKey()).child(eastKey).child("signal_value").setValue(0);
                                        nearestsignal.getEast().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                    }
                                    break;
                                }
                                case "W":{
                                    if(nearestsignal.getNorth().getSignal_value()==2){
                                        ref.child(nearestsignal.getKey()).child(northKey).child("signal_value").setValue(1);
                                        nearestsignal.getNorth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_orange));
                                    }else{
                                        ref.child(nearestsignal.getKey()).child(northKey).child("signal_value").setValue(0);
                                        nearestsignal.getNorth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                    }
                                    if(nearestsignal.getSouth().getSignal_value()==2){
                                        ref.child(nearestsignal.getKey()).child(southKey).child("signal_value").setValue(1);
                                        nearestsignal.getSouth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_orange));
                                    }else{
                                        ref.child(nearestsignal.getKey()).child(southKey).child("signal_value").setValue(0);
                                        nearestsignal.getSouth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                    }
                                    if(nearestsignal.getEast().getSignal_value()==2){
                                        ref.child(nearestsignal.getKey()).child(eastKey).child("signal_value").setValue(1);
                                        nearestsignal.getEast().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_orange));
                                    }else{
                                        ref.child(nearestsignal.getKey()).child(eastKey).child("signal_value").setValue(0);
                                        nearestsignal.getEast().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                    }
                                    break;
                                }
                                case "E":{
                                    if(nearestsignal.getNorth().getSignal_value()==2){
                                        ref.child(nearestsignal.getKey()).child(northKey).child("signal_value").setValue(1);
                                        nearestsignal.getNorth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_orange));
                                    }else{
                                        ref.child(nearestsignal.getKey()).child(northKey).child("signal_value").setValue(0);
                                        nearestsignal.getNorth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                    }
                                    if(nearestsignal.getSouth().getSignal_value()==2){
                                        ref.child(nearestsignal.getKey()).child(southKey).child("signal_value").setValue(1);
                                        nearestsignal.getSouth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_orange));
                                    }else{
                                        ref.child(nearestsignal.getKey()).child(southKey).child("signal_value").setValue(0);
                                        nearestsignal.getSouth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                    }
                                    if(nearestsignal.getWest().getSignal_value()==2){
                                        ref.child(nearestsignal.getKey()).child(westKey).child("signal_value").setValue(1);
                                        nearestsignal.getWest().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_orange));
                                    }else{
                                        ref.child(nearestsignal.getKey()).child(westKey).child("signal_value").setValue(0);
                                        nearestsignal.getWest().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                    }
                                    break;
                                }
                            }
                            alertLL.setVisibility(View.VISIBLE);
                            isAlertOver = false;
                            //alertTextTV.setText("Caution! Signal in " + (int) signalAlertThreshold + " m");
                            //voiceAlert("Caution! Signal in " + (int) signalAlertThreshold + " meters");
                            alertTextTV.setText("Turning signal to green");
                            //voiceAlert("Turning signal to green");
                        }else{
                            if(distance<120){
                                switch (direction){
                                    case "N":{
                                        ref.child(nearestsignal.getKey()).child(southKey).child("signal_value").setValue(3);
                                        ref.child(nearestsignal.getKey()).child(westKey).child("signal_value").setValue(3);
                                        ref.child(nearestsignal.getKey()).child(eastKey).child("signal_value").setValue(3);
                                        nearestsignal.getSouth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                        nearestsignal.getWest().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                        nearestsignal.getEast().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                        break;
                                    }
                                    case "S":{
                                        ref.child(nearestsignal.getKey()).child(northKey).child("signal_value").setValue(3);
                                        ref.child(nearestsignal.getKey()).child(westKey).child("signal_value").setValue(3);
                                        ref.child(nearestsignal.getKey()).child(eastKey).child("signal_value").setValue(3);
                                        nearestsignal.getNorth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                        nearestsignal.getWest().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                        nearestsignal.getEast().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                        break;
                                    }
                                    case "W":{
                                        ref.child(nearestsignal.getKey()).child(northKey).child("signal_value").setValue(3);
                                        ref.child(nearestsignal.getKey()).child(southKey).child("signal_value").setValue(3);
                                        ref.child(nearestsignal.getKey()).child(eastKey).child("signal_value").setValue(3);
                                        nearestsignal.getNorth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                        nearestsignal.getSouth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                        nearestsignal.getEast().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                        break;
                                    }
                                    case "E":{
                                        ref.child(nearestsignal.getKey()).child(northKey).child("signal_value").setValue(3);
                                        ref.child(nearestsignal.getKey()).child(southKey).child("signal_value").setValue(3);
                                        ref.child(nearestsignal.getKey()).child(westKey).child("signal_value").setValue(3);
                                        nearestsignal.getNorth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                        nearestsignal.getSouth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                        nearestsignal.getWest().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    alertLL.setVisibility(View.GONE);
                }
            } else {
                nearestsignal = getNearestsignal();
                if(currentBearing>=80&&currentBearing<=100){
                    direction = "E";
                }
                alertLL.setVisibility(View.GONE);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    boolean isAlertOver=true;
    void voiceAlert(String text){
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
                final String keyword = s;
                isAlertOver=false;
            }

            @Override
            public void onDone(String s) { }

            @Override
            public void onError(String s) {}
        });
        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "");
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "Dummy String");
    }

    TextToSpeech tts;
    void initTTS(){
        tts = new TextToSpeech(this, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(getApplicationContext(), "TTS Language not supported", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(getApplicationContext(), "Init TTS failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    List<Signal> removedSignals = new ArrayList<>();
    boolean signalRemoved = false;
    void removesignal(String key){
        if(signalRemoved==false) {
            signalRemoved=true;
            try {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ref.child(key).child(northKey).child("signal_value").setValue(0);
                            ref.child(key).child(southKey).child("signal_value").setValue(0);
                            ref.child(key).child(eastKey).child("signal_value").setValue(1);
                            nearestsignal.getNorth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                            nearestsignal.getSouth().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                            nearestsignal.getEast().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_orange));
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        ref.child(key).child(eastKey).child("signal_value").setValue(0);
                                        ref.child(key).child(westKey).child("signal_value").setValue(2);
                                        ref.child(key).child("mode").setValue(0);
                                        nearestsignal.getWest().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_green));
                                        nearestsignal.getEast().getMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_traffic_light_red));
                                        nearestsignal = null;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, 3000);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {

                                }
                            }, 4000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }, 15000);

                for (int i = 0; i < signals.size(); i++) {
                    if (signals.get(i).getKey().equals(key)) {
                        removedSignals.add(signals.get(i));
                        signals.remove(i);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
