package me.ajaybala.ambulanceapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fxn.stash.Stash;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.ajaybala.ambulanceapp.adapters.PlaceDetailsAdapter;
import me.ajaybala.ambulanceapp.models.PlaceDetail;
import me.ajaybala.ambulanceapp.utils.LatLngInterpolator;
import me.ajaybala.ambulanceapp.utils.MarkerAnimation;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private static final String TAG = "MapsActivity";
    //Views
    FloatingActionButton myPosFab;
    LinearLayout searchBarLL;
    EditText searchET;
    TextView enterDestTV;
    ImageView backIV;
    LinearLayout panelLL, bottomSheetLL, containerLL, hsvLL;
    RelativeLayout rootRL;
    TextView placeTV1, placeTV2, distanceTV, startTV, directionsTV, closeTV;

    boolean panelOpen=false;

    private GoogleMap mMap;
    private ArrayList permissionsToRequest;
    private ArrayList permissionsRejected = new ArrayList();
    private ArrayList permissions = new ArrayList();

    private final static int ALL_PERMISSIONS_RESULT = 101;
    private LocationManager locationManager;
    private Marker currentLocationMarker;
    private Marker destinationMarker;
    private Location currentLocation;
    private boolean firstTimeFlag = true;
    PlacesClient placesClient;

    //Autocomplete
    ArrayList<PlaceDetail> predictions = new ArrayList<>();
    RecyclerView searchResultsRV;
    PlaceDetailsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        initMap();
        initUI();
        initLocationStuff();
        //testDirections();

    }

    void testDirections(){
        startActivity(new Intent(getApplicationContext(),NavigationActivity.class));
    }


    BitmapDescriptor currentLocationIcon;
    void initUI() {
        closeKeyboard();
        rootRL = findViewById(R.id.rootRL);
        currentLocationIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_user_pin);
        myPosFab = findViewById(R.id.myPosFAB);
        searchBarLL = findViewById(R.id.searchBarLL);
        searchET = findViewById(R.id.searchET);
        enterDestTV = findViewById(R.id.enterDestTV);
        backIV = findViewById(R.id.backIV);
        panelLL = findViewById(R.id.panelLL);
        bottomSheetLL = findViewById(R.id.bottomSheetLL);
        containerLL = findViewById(R.id.containerLL);
        hsvLL = findViewById(R.id.hsvLL);
        placeTV1 = findViewById(R.id.placeTV1);
        placeTV2 = findViewById(R.id.placeTV2);
        distanceTV = findViewById(R.id.distanceTV);
        startTV = findViewById(R.id.startTV);
        directionsTV = findViewById(R.id.directionsTV);
        closeTV = findViewById(R.id.closeTV);
        searchResultsRV = findViewById(R.id.searchResultRV);
        adapter = new PlaceDetailsAdapter(getApplicationContext(), predictions, new PlaceDetailsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(PlaceDetail item) {
                getPlace(item.getPlaceId(),item);
            }
        });
        searchResultsRV.setLayoutManager(new LinearLayoutManager(getApplicationContext(),RecyclerView.VERTICAL,false));
        searchResultsRV.setAdapter(adapter);

        searchBarLL.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        bottomSheetLL.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        hsvLL.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        myPosFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLocationMarker!=null) {
                    animateCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 17);

                }
            }


        });

        backIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(panelOpen){
                    closePanel();
                }else{
                    openPanel();
                }
            }
        });

        enterDestTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(panelOpen){
                    closePanel();
                }else{
                    openPanel();
                }
            }
        });

        searchET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    autocompletePlaces(searchET.getText().toString());
                    searchET.clearFocus();
                    closeKeyboard();
                    return true;
                }
                return false;
            }
        });
    }

    void openPanel(){
        panelOpen=true;
        searchET.setVisibility(View.VISIBLE);
        enterDestTV.setVisibility(View.GONE);
        myPosFab.hide();
        backIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_arrow_left));
        panelLL.setAlpha(0);
        panelLL.setVisibility(View.VISIBLE);
        panelLL.animate().alpha(1).setDuration(500).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        }).start();

        searchET.requestFocus();
        openKeyboard(searchET);

    }

    void closePanel(){
        panelOpen=false;
        searchET.setVisibility(View.GONE);
        enterDestTV.setVisibility(View.VISIBLE);
        myPosFab.show();
        backIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_search));
        panelLL.animate().alpha(0).setDuration(500).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                panelLL.setVisibility(View.GONE);
            }
        }).start();
        searchET.setText("");
        predictions.clear();
        adapter.notifyDataSetChanged();

        closeKeyboard();

    }

    AutocompleteSessionToken token;
    void autocompletePlaces(String query){

        token = AutocompleteSessionToken.newInstance();

        // Use the builder to create a FindAutocompletePredictionsRequest.
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                // Call either setLocationBias() OR setLocationRestriction().
                //.setLocationBias(bounds)
                //.setLocationRestriction(bounds)
                .setCountry("IN")
                .setTypeFilter(TypeFilter.ESTABLISHMENT)
                .setSessionToken(token)
                .setQuery(query)
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener(new OnSuccessListener<FindAutocompletePredictionsResponse>() {
            @Override
            public void onSuccess(FindAutocompletePredictionsResponse response) {
                predictions.clear();
                for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                    /*Log.i("hello", prediction.getPlaceId());
                    Log.i("hello", prediction.getPrimaryText(null).toString());
                    Toast.makeText(MapsActivity.this, prediction.getPrimaryText(null) + "-" + prediction.getSecondaryText(null), Toast.LENGTH_SHORT).show();*/
                    predictions.add(new PlaceDetail(prediction.getPrimaryText(null).toString(),prediction.getSecondaryText(null).toString(),prediction.getPlaceId(),false));
                }
                adapter.notifyDataSetChanged();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
                if (e instanceof ApiException) {
                    ApiException apiException = (ApiException) e;
                    Log.e("Hello", "Place not found: " + apiException.getStatusCode());
                }
            }
        });
    }

    Place currentPlace;
    void getPlace(final String placeId, final PlaceDetail placeDetail){
        // Specify the fields to return.
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
        // Construct a request object, passing the place ID and fields array.
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);
        placesClient.fetchPlace(request).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
            @Override
            public void onSuccess(FetchPlaceResponse response) {
                Place place = response.getPlace();
                Log.i("Hello", "Place found: " + place.getName()+"\nPlace lat lng: "+place.getLatLng());
                currentPlace = place;
                setupDestinationUI(place,placeDetail);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ApiException) {
                    ApiException apiException = (ApiException) e;
                    int statusCode = apiException.getStatusCode();
                    // Handle error with given status code.
                    Log.e("hello", "Place not found: " + e.getMessage());
                    Toast.makeText(getApplicationContext(),"Error occurred "+statusCode,Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {

            }
        });

    }

    private void animateCamera(@NonNull LatLng latLng,float zoom) {
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(getCameraPositionWithBearing(latLng,zoom)));
    }

    @NonNull
    private CameraPosition getCameraPositionWithBearing(LatLng latLng,float zoom) {
        return new CameraPosition.Builder().target(latLng).zoom(zoom).build();
    }
    boolean developer_move=false;
    private void showMarker(@NonNull Location currentLocation) {
        LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        if (currentLocationMarker == null) {
            currentLocationMarker = mMap.addMarker(new MarkerOptions().icon(currentLocationIcon).position(latLng));
        }
        else {
            developer_move = true;
            MarkerAnimation.animateMarkerToGB(currentLocationMarker, latLng, new LatLngInterpolator.Spherical(),2000);
        }
    }

    void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    void initLocationStuff() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        // get GPS status
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (gpsEnabled) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(ACCESS_FINE_LOCATION);
                permissions.add(ACCESS_COARSE_LOCATION);
                permissionsToRequest = findUnAskedPermissions(permissions);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (permissionsToRequest.size() > 0)
                        requestPermissions((String[]) permissionsToRequest.toArray(new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
                }
            }else{
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 2, this);
            }

        }else{
            showSettingsAlert();
        }

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }

        // Create a new Places client instance.
        placesClient = Places.createClient(this);
    }


    private ArrayList findUnAskedPermissions(ArrayList wanted) {
        ArrayList result = new ArrayList();
        for (Object perm : wanted) {
            if (!hasPermission((String) perm)) {
                result.add(perm);
            }
        }
        return result;
    }

    private boolean hasPermission(String permission) {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (Object perms : permissionsToRequest) {
                    if (!hasPermission((String) perms)) {
                        permissionsRejected.add(perms);
                    }
                }
                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale((String) permissionsRejected.get(0))) {
                            showMessageOKCancel("These permissions are mandatory for the application. Please allow access.",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions((String[]) permissionsRejected.toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    });
                            return;
                        }
                    }
                }
                break;
        }

    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MapsActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }


    @Override
    public void onLocationChanged(Location location) {
        //Toast.makeText(getApplicationContext(),"Lat: "+location.getLatitude()+" Lng: "+location.getLongitude(),Toast.LENGTH_LONG).show();
        currentLocation = location;
        if (firstTimeFlag && mMap != null) {
            animateCamera(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()),17);
            firstTimeFlag = false;
        }
        showMarker(currentLocation);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapsActivity.this);
        alertDialog.setTitle("GPS is not Enabled!");
        alertDialog.setMessage("Do you want to turn on GPS?");
        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });
        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

    //Hides Keyboard
    private void closeKeyboard() {
        View view = findViewById(android.R.id.content);
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    //Hides Keyboard
    private void openKeyboard(EditText editText) {
        View view = findViewById(android.R.id.content);
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public void onBackPressed() {
        if(panelOpen){
            closePanel();
        }else{
            super.onBackPressed();
            finish();
        }
    }

    void setupDestinationUI(final Place place, PlaceDetail placeDetail){
        closePanel();
        searchBarLL.setVisibility(View.GONE);
        if(destinationMarker==null){
            destinationMarker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker()).position(place.getLatLng()));
            destinationMarker.setTitle(placeDetail.getPrimaryText());
        }else{
            destinationMarker.remove();
            destinationMarker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker()).position(place.getLatLng()));
            destinationMarker.setTitle(placeDetail.getPrimaryText());

        }
        animateCamera(place.getLatLng(),15);

        containerLL.setVisibility(View.VISIBLE);
        placeTV1.setText(placeDetail.getPrimaryText());
        placeTV2.setText(placeDetail.getSecondaryText());
        startTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testDirections();
            }
        });
        directionsTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                directionsTV.setVisibility(View.GONE);
                startTV.setVisibility(View.VISIBLE);
                LatLng source = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());

                LatLng destination = place.getLatLng();
                getDirections(source,destination,getString(R.string.google_maps_key));
            }


        });
        closeTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                containerLL.setVisibility(View.GONE);
                directionsTV.setVisibility(View.VISIBLE);
                startTV.setVisibility(View.GONE);
                distanceTV.setVisibility(View.INVISIBLE);
                searchBarLL.setVisibility(View.VISIBLE);
                mMap.clear();
                currentLocationMarker=null;
                showMarker(currentLocation);
                animateCamera(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()),17);
            }
        });


    }


    private void getDirections(LatLng origin, LatLng destination, String api_key)
    {
        Stash.put("origin",origin);
        Stash.put("destination",destination);
        //Move camera to origin
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 15));


        //Forming an URL string which will return JSON as a result.
        String originString = "origin=" + origin.latitude + "," + origin.longitude;
        String destinationString = "destination=" + destination.latitude + "," + destination.longitude;

        //IF THIS GENERATES ERROR, HARD CODE API KEY INTO URL.
        String url = "https://maps.googleapis.com/maps/api/directions/json?"+ originString + "&" + destinationString + "&key=" + api_key;


        //Run the URL formed in above step and wait for result.
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute(url);
    }

    private String downloadUrl(String url) throws IOException
    {
        String data = "";
        InputStream inputStream = null;
        HttpURLConnection urlConnection = null;

        try
        {
            URL actualURL = new URL(url);
            urlConnection = (HttpURLConnection)actualURL.openConnection();
            urlConnection.connect();

            inputStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuffer sb = new StringBuffer();

            String line = "";
            while((line = br.readLine()) != null)
            {
                sb.append(line);
            }

            data = sb.toString();

            br.close();
        }
        catch (Exception e)
        {
            Log.d("EXCEPTION DOWNLADING", e.toString());
        }
        finally {
            inputStream.close();
            urlConnection.disconnect();
        }

        return data;
    }

    private class DownloadTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... strings) {
            String data = "";

            try
            {
                data = downloadUrl(strings[0]);
            }
            catch (Exception e)
            {
                Log.d("ASYNC TASK", e.toString());
            }

            return data;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //Toast.makeText(showPreviousOrder.this, s, Toast.LENGTH_LONG).show();

            int totalDistance = 0;
            int totalTravelTime = 0;
            System.out.println("hello -> "+s);
            try {
                JSONObject parentMain = new JSONObject(s);
                JSONArray legs = parentMain.getJSONArray("routes").getJSONObject(0).getJSONArray("legs");
                List<LatLng> markers = new ArrayList<>();
                for(int i = 0; i < legs.length(); i++)
                {
                    JSONArray steps = legs.getJSONObject(i).getJSONArray("steps");
                    JSONObject distance = legs.getJSONObject(i).getJSONObject("distance");
                    JSONObject duration = legs.getJSONObject(i).getJSONObject("duration");

                    totalDistance += Integer.parseInt(distance.getString("value"));
                    totalTravelTime += Integer.parseInt(duration.getString("value"));

                    for(int j = 0; j < steps.length(); j++)
                    {
                        JSONObject polyline = steps.getJSONObject(j).getJSONObject("polyline");
                        markers.addAll(PolyUtil.decode(polyline.getString("points")));
                    }
                }
                PolylineOptions lineOptions = null;
                lineOptions = new PolylineOptions();
                lineOptions.addAll(markers);
                Stash.put("markers",markers);
                lineOptions.width(10);
                lineOptions.color(getResources().getColor(R.color.colorAccent));

                // Drawing polyline in the Google Map
                if(lineOptions != null) {
                    mMap.addPolyline(lineOptions);
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    builder.include(currentLocationMarker.getPosition());
                    builder.include(destinationMarker.getPosition());
                    LatLngBounds bounds = builder.build();
                    int padding = 200; // offset from edges of the map in pixels
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                    mMap.animateCamera(cu);
                }
                else {
                    Log.d("hello","Error drawing poly lines");
                }
            } catch (JSONException e) {
                Toast.makeText(getApplicationContext(), "WELL I MESSED UP 2 !", Toast.LENGTH_LONG).show();
            }
            toastData(totalDistance, totalTravelTime);
        }

        //Simply displays a toast message containing total distance and total time required.
        public void toastData(int totalDistance, int totalTravelTime)
        {
            int km = 0, m = 0;
            String displayDistance = "";

            if(totalDistance < 1000)
            {
                displayDistance =  String.valueOf(totalDistance) + " m";
            }
            else
            {
                while(totalDistance >= 1000)
                {
                    km++;
                    totalDistance -= 1000;
                }
                m = totalDistance;
                if(m>500){
                    km++;
                    displayDistance = String.valueOf(km) + " km";
                }else{
                    displayDistance = String.valueOf(km) + " km";
                }

            }

            int min = 0, sec = 0;
            String displayTravelTime = "";
            if(totalDistance < 60)
                displayTravelTime = "1 minute";
            else
            {
                while(totalTravelTime >= 60)
                {
                    min++;
                    totalTravelTime -= 60;
                }
                sec = totalTravelTime;
                displayTravelTime = String.valueOf(min) + ":" + String.valueOf(sec) + " min";
            }

            //Toast.makeText(getApplicationContext(), "DISTANCE : " + displayDistance + "\nTIME REQUIRED : " + displayTravelTime, Toast.LENGTH_LONG).show();
            distanceTV.setText(displayDistance);
            distanceTV.setVisibility(View.VISIBLE);
            Stash.put("distance",totalDistance);
            Stash.put("travelTime",totalTravelTime);
        }
    }
}




