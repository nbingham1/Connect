package mobilesystems.connect;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.SyncStateContract;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BackgroundLocationService extends Service implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener {

    IBinder mBinder = new LocalBinder();
    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    // Flag that indicates if a request is underway.
    private boolean mInProgress;
    private Boolean servicesAvailable = false;
    public class LocalBinder extends Binder {
        public BackgroundLocationService getServerInstance() {
            return BackgroundLocationService.this;
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();
        mInProgress = false;
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(1000*60*5);
        mLocationRequest.setFastestInterval(1000*60*2);
        servicesAvailable = servicesConnected();
        mLocationClient = new LocationClient(this, this, this);
    }
    private boolean servicesConnected() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        } else {
            return false;
        }
    }

    public int onStartCommand (Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if(!servicesAvailable || mLocationClient.isConnected() || mInProgress)
            return START_STICKY;
        setUpLocationClientIfNeeded();
        if(!mLocationClient.isConnected() || !mLocationClient.isConnecting() && !mInProgress)
        {
            Log.d("Connect Location", DateFormat.getDateTimeInstance().format(new Date()) + ": Started");
            mInProgress = true;
            mLocationClient.connect();
        }
        return START_STICKY;
    }

    private void setUpLocationClientIfNeeded()
    {
        if(mLocationClient == null)
            mLocationClient = new LocationClient(this, this, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        String msg = Double.toString(location.getLatitude()) + "," + Double.toString(location.getLongitude());
        Intent intent = new Intent("com.Connect.StatusActivity$LocationListener");
        intent.putExtra(LocationClient.KEY_LOCATION_CHANGED, location);
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy(){
        mInProgress = false;
        if(servicesAvailable && mLocationClient != null) {
            mLocationClient.removeLocationUpdates(this);
            mLocationClient = null;
        }
        Log.d("Connect Location", DateFormat.getDateTimeInstance().format(new Date()) + ": Stopped");
        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Intent intent = new Intent(this, StatusActivity.LocationReceiver.class);
        PendingIntent locationIntent = PendingIntent.getBroadcast(getApplicationContext(), 14872, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        mLocationClient.requestLocationUpdates(mLocationRequest, locationIntent);

        mLocationClient.requestLocationUpdates(mLocationRequest, this);
        Log.d("Connect Location", DateFormat.getDateTimeInstance().format(new Date()) + ": Connected");
    }

    @Override
    public void onDisconnected() {
        mInProgress = false;
        mLocationClient = null;
        Log.d("Connect Location", DateFormat.getDateTimeInstance().format(new Date()) + ": Disconnected");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mInProgress = false;

        if (connectionResult.hasResolution()) {

        } else {

        }
    }
}