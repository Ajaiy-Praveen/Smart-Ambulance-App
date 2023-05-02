package me.ajaybala.ambulanceapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.fxn.stash.Stash;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import me.ajaybala.ambulanceapp.utils.Helper;
import me.ajaybala.ambulanceapp.R;

public class NavigationActivityBackup extends FragmentActivity implements OnMapReadyCallback{

    LatLng origin, destination;
    int totalTime, totalDistance;
    List<LatLng> markers = new ArrayList<>();
    private GoogleMap mMap;
    RelativeLayout rootRL;
    private BitmapDescriptor currentLocationIcon;
    private LocationManager locationManager;
    private Marker currentLocationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        getDirectionsData();
        initMap();
        initUI();

    }

    void initUI(){
        currentLocationIcon = BitmapDescriptorFactory.fromResource(R.drawable.navigation_icon);
        rootRL = findViewById(R.id.rootRL);
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
        try {
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.map_style));

            if (!success) {
                Log.e("hello", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("hello", "Can't find style. Error: ", e);
        }

        drawRoute();
        animateMarker();

    }

    Handler handler;
    int index=0,next=0;
    LatLng newPos;
    long dura_speed=1000;
    long speed=30;
    void animateMarker(){
        currentLocationMarker = mMap.addMarker(new MarkerOptions().icon(currentLocationIcon).flat(true).position(markers.get(0)));
        final float bearing = (float) Helper.getBearing(origin,markers.get(1));
        currentLocationMarker.setRotation(bearing);
        animateCamera(markers.get(0),19, bearing);
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    final LatLng startPosition, endPosition;
                    if (index < markers.size() - 1) {
                        handler.postDelayed(this, dura_speed);
                        index++;
                        next = index + 1;

                        startPosition = markers.get(index);
                        endPosition = markers.get(next);
                        System.out.println("Distance " + Helper.distance(startPosition.latitude, startPosition.longitude, endPosition.latitude, endPosition.longitude));
                        double distance = Helper.distance(startPosition.latitude, startPosition.longitude, endPosition.latitude, endPosition.longitude);
                        dura_speed = (long) Math.ceil((distance*1000 / speed)*1000);
                        System.out.println("Time 1 " + distance*1000 / speed);
                        System.out.println("Time 2 " + dura_speed);
                        final ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
                        valueAnimator.setDuration(dura_speed);
                        valueAnimator.setInterpolator(new LinearInterpolator());
                        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                double v = animation.getAnimatedFraction();
                                double lng = v * endPosition.longitude + (1 - v) * startPosition.longitude;
                                double lat = v * endPosition.latitude + (1 - v) * startPosition.latitude;
                                newPos = new LatLng(lat, lng);
                                currentLocationMarker.setPosition(newPos);
                                currentLocationMarker.setAnchor(0.5f, 0.5f);
                                final float newBearingValue = (float) Helper.getBearing(startPosition, newPos);
                                if (newBearingValue != 0.0) {
                                    if (prevbearingValue != newBearingValue) {
                                        if (flag) {
                                            flag = false;
                                            updateBearingValues(newBearingValue);
                                        }
                                        currentLocationMarker.setRotation(prevbearingValue);
                                        moveCamera(newPos, 19, prevbearingValue);
                                    } else {
                                        moveCamera(newPos, 19, newBearingValue);
                                    }
                                }
                            }
                        });
                        valueAnimator.start();

                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        },dura_speed-dura_speed);

    }

    float prevbearingValue = 0;
    boolean flag=true;

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
        return new CameraPosition.Builder().target(latLng).bearing(bearing).zoom(zoom).tilt(45).build();
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

}
