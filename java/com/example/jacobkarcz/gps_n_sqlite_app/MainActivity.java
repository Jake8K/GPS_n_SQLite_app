package com.example.jacobkarcz.gps_n_sqlite_app;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


/* [START Main Activity Class] */
public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback  {

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private TextView mLatText;
    private TextView mLonText;
    private Location mLastLocation;
    private LocationListener mLocationListener;
    private static final int LOCATION_PERMISSON_RESULT = 17;
    MapView mapView;
    private GoogleMap mMap;

    SQLiteExample mSQLiteExample;
    Button mSQLSubmitButton;
    Cursor mSQLCursor;
    SimpleCursorAdapter mSQLCursorAdapter;
    private static final String TAG = "SQLActivity";
    SQLiteDatabase mSQLDB;

    private  String lat_strg = "44.5657";
    private  String lon_strg = "-123.2789";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_id);
        mapFragment.getMapAsync(this);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(5000);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    //mLatText.setText(String.valueOf(location.getLatitude()));
                    //mLonText.setText(String.valueOf(location.getLongitude()));
                    lat_strg = String.valueOf(location.getLatitude());
                    lon_strg = String.valueOf(location.getLongitude());
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Location Acquisition Failed").setTitle("Failure");
                    builder.setPositiveButton("gotcha", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //nothing
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        };


        mSQLiteExample = new SQLiteExample(this);
        mSQLDB = mSQLiteExample.getWritableDatabase();

        mSQLSubmitButton = (Button) findViewById(R.id.sql_add_row_button);
        mSQLSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSQLDB != null) {
                    EditText editText = (EditText) findViewById(R.id.sql_text_input);
                    String textBoxContents = editText.getText().toString();
                    editText.setText("");

                    updateLocation(); //

                    ContentValues vals = new ContentValues();
                    vals.put(DBContract.DemoTable.COLUMN_NAME_NOTE, textBoxContents);
                    vals.put(DBContract.DemoTable.COLUMN_NAME_LAT, lat_strg);
                    vals.put(DBContract.DemoTable.COLUMN_NAME_LON, lon_strg);
                    mSQLDB.insert(DBContract.DemoTable.TABLE_NAME, null, vals);
                    populateTable();

                    // https://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);


                } else {
                    Log.d(TAG, "Unable to access database for writing.");
                }
            }
        });
        populateTable();

        setupUI(findViewById(R.id.activity_main));

    }

    //hide keyboard when not entering a note
    //https://stackoverflow.com/questions/4165414/how-to-hide-soft-keyboard-on-android-after-clicking-outside-edittext
    public void setupUI(View view) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }

    /*MAP*/
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in OSU and move the camera.
        LatLng osu = new LatLng(44.5657, -123.2789);
        float zoom = 13;
        mMap.addMarker(new MarkerOptions().position(osu).title("Oregon State University"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(osu));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(osu, zoom));

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            Log.d(TAG, "map is using location");

        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //mLatText.setText("onConnect");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onConnected is requesting permissions");
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSON_RESULT);
            //mLonText.setText("Lacking Permissions");
            return;
        }
        updateLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Dialog errDialog = GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), 0);
        errDialog.show();
        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        if(requestCode == LOCATION_PERMISSON_RESULT){
            if(grantResults.length > 0){
                updateLocation();
                Log.d(TAG, "OnRequestPermissionResult() updating location");
            }
            else {
                //mLatText = (TextView) findViewById(R.id.lat_output);
                //mLonText = (TextView) findViewById(R.id.lon_output);
                lat_strg = "44.5657";
                lon_strg = "-123.2789";
            }
        }

    }

    private void updateLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "in updateLocation()... checking permissions");
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        try {
            if(mLastLocation != null){
                //mLonText.setText(String.valueOf(mLastLocation.getLongitude()));
                //mLatText.setText(String.valueOf(mLastLocation.getLatitude()));
                lat_strg = String.valueOf(mLastLocation.getLatitude());
                lon_strg = String.valueOf(mLastLocation.getLongitude());
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                Log.d(TAG, "updateLocation() lat-lon: " + lat_strg +"-"+ lon_strg);
            } else {
                //mMap.setMyLocationEnabled(false);
                //mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastLocation = null;
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,mLocationListener);
                Log.d(TAG, "updateLocation() is requesting a location update permissions");
            }
        }
        catch (SecurityException e) {
            Log.e("Exception %s",  e.getMessage());
        }
    }

    private void populateTable(){
        if(mSQLDB != null){
            try {
                if(mSQLCursorAdapter != null && mSQLCursorAdapter.getCursor() != null){
                    if(!mSQLCursorAdapter.getCursor().isClosed()){
                        mSQLCursorAdapter.getCursor().close();
                    }
                }
                mSQLCursor = mSQLDB.query(DBContract.DemoTable.TABLE_NAME,
                        new String[]{DBContract.DemoTable._ID,  DBContract.DemoTable.COLUMN_NAME_LAT,
                                DBContract.DemoTable.COLUMN_NAME_LON, DBContract.DemoTable.COLUMN_NAME_NOTE},
                        null, null, null, null, null);

                ListView SQLListView = (ListView) findViewById(R.id.sql_list_view);

                mSQLCursorAdapter = new SimpleCursorAdapter(this,
                        R.layout.sql_note,
                        mSQLCursor,
                        new String[]{DBContract.DemoTable.COLUMN_NAME_LAT, DBContract.DemoTable.COLUMN_NAME_LON, DBContract.DemoTable.COLUMN_NAME_NOTE},
                        new int[]{R.id.lat_output, R.id.lon_output, R.id.note_output},
                        0);
                SQLListView.setAdapter(mSQLCursorAdapter);
            } catch (Exception e) {
                Log.d(TAG, "Error loading data from database");
            }
        }
    }


    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

}
/* [END Main Activity Class] */

/* [START SQLiteExample Class] */
class SQLiteExample extends SQLiteOpenHelper {

    public SQLiteExample(Context context) {
        super(context, DBContract.DemoTable.DB_NAME, null, DBContract.DemoTable.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DBContract.DemoTable.SQL_CREATE_DEMO_TABLE);

        ContentValues testValues = new ContentValues();
        testValues.put(DBContract.DemoTable.COLUMN_NAME_LAT, "Latitude");
        testValues.put(DBContract.DemoTable.COLUMN_NAME_LON, "Longitude");
        testValues.put(DBContract.DemoTable.COLUMN_NAME_NOTE, "Notes");
        db.insert(DBContract.DemoTable.TABLE_NAME, null, testValues);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DBContract.DemoTable.SQL_DROP_DEMO_TABLE);
        onCreate(db);
    }
}
/* [END SQLiteExample Class] */

/* [START Contract Class] */
final class DBContract {
    private DBContract(){};

    public final class DemoTable implements BaseColumns {
        public static final String DB_NAME = "geo_tag_notes";
        public static final String TABLE_NAME = "GeoTagNotes";
        public static final String COLUMN_NAME_NOTE = "note";
        public static final String COLUMN_NAME_LAT = "lat";
        public static final String COLUMN_NAME_LON = "lon";
        public static final int DB_VERSION = 1;


        public static final String SQL_CREATE_DEMO_TABLE = "CREATE TABLE " +
                DemoTable.TABLE_NAME + "(" + DemoTable._ID + " INTEGER PRIMARY KEY NOT NULL," +
                DemoTable.COLUMN_NAME_LON + " VARCHAR(255)," +
                DemoTable.COLUMN_NAME_LAT + " VARCHAR(255)," +
                DemoTable.COLUMN_NAME_NOTE + " VARCHAR(255)) ;" ;

        public static final String SQL_TEST_DEMO_TABLE_INSERT = "INSERT INTO " + TABLE_NAME +
                " (" + COLUMN_NAME_LAT + "," + COLUMN_NAME_LON + "," + COLUMN_NAME_NOTE + ") VALUES ('XXX.xxx', 'YYY.yyy' 'Test Note');";

        public  static final String SQL_DROP_DEMO_TABLE = "DROP TABLE IF EXISTS " + DemoTable.TABLE_NAME;
    }
}
/* [END Contract Class] */
