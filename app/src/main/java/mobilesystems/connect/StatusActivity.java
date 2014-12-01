package mobilesystems.connect;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import mobilesystems.connect.utils.MovesAPI;
import mobilesystems.connect.utils.ServerAPI;
import mobilesystems.connect.utils.ServerAccess;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class StatusActivity extends Activity implements ServerAccess {

    static final int LOGIN_ACTIVITY = 1;

    MovesAPI.MovesAuth movesAuth = null;
    TextView appStatus = null;
    BackgroundLocationService serve;
    Intent locationIntent = null;

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            BackgroundLocationService.LocalBinder b = (BackgroundLocationService.LocalBinder) binder;
            serve = b.getServerInstance();
        }

        public void onServiceDisconnected(ComponentName className) {
            serve = null;
        }
    };

    public static class LocationReceiver extends BroadcastReceiver {
        StatusActivity activity = null;

        LocationReceiver() {

        }

        LocationReceiver(StatusActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = (Location) intent.getExtras().get(LocationClient.KEY_LOCATION_CHANGED);

            if (location != null && activity != null && activity.movesAuth != null)
                new ServerAPI(activity).execute("move", activity.movesAuth.user_id, Double.toString(location.getLatitude()), Double.toString(location.getLongitude()));
        }
    }

    LocationReceiver mReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appStatus = (TextView) findViewById(R.id.application_status);
        setContentView(R.layout.activity_status);
    }

    @Override
    protected void onDestroy() {
        if (locationIntent != null) {
            stopService(locationIntent);
            unbindService(mConnection);
            locationIntent = null;
            mConnection = null;
        }

        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }

        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.status, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, LoginActivity.class);
                if (movesAuth != null) {
                    Gson gson = new Gson();
                    intent.putExtra("movesAuth", gson.toJson(movesAuth));
                }
                startActivityForResult(intent, LOGIN_ACTIVITY);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == LOGIN_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                Gson gson = new Gson();
                if (data.hasExtra("movesAuth"))
                    movesAuth = gson.fromJson(data.getStringExtra("movesAuth"), MovesAPI.MovesAuth.class);

                if (movesAuth != null && mReceiver == null && locationIntent == null) {
                    mReceiver = new LocationReceiver(this);
                    IntentFilter intentFilter = new IntentFilter("com.Connect.StatusActivity$LocationListener");
                    registerReceiver(mReceiver, intentFilter);

                    locationIntent = new Intent(this, BackgroundLocationService.class);
                    bindService(locationIntent, mConnection, Context.BIND_AUTO_CREATE);
                    startService(locationIntent);
                }
            }
        }
    }

    /*******************************************************
     * Method called after the MovesAPI has been executed.
     * See MovesAPI:java
     *******************************************************/
    public void doReceiveServer(String cmd, String token) {
        Log.d("Connect Status", "Received " + cmd + ": " + token);

        if (token.length() > 0) {
            JsonElement jelement = new JsonParser().parse(token);
            JsonArray jarray = jelement.getAsJsonArray();

            boolean recommendation_found = false;
            String message = "";
            for(JsonElement element : jarray)
            {
                recommendation_found = true;
                Log.d("Recommendations: ", element.toString().replace("\"", ""));
                message +=  element.toString().replace("\"", "")+"\n";
            }

            if(!recommendation_found)
                message = "No recommendations";

            TextView view = (TextView) findViewById(R.id.application_status);
            view.setText(message);
        }
    }
}
