package mobilesystems.connect.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import mobilesystems.connect.LoginActivity;

/**
 * Created by Oliver on 11/30/2014.
 */
public class AlarmReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent)
    {
        SendLocation(context);
        Log.d("HH", "LOCATION SENT");
    }

    HttpRequestResult listener = new HttpRequestResult() {
        @Override
        public void HttpRequestResult(String result) {


            Log.d("HH", "Should have been sent");

        }
    };

    /*******************************************************
     * Function to send the current location to the server
     ******************************************************/

    private static final String URL_LONGLAT =  "http://connect.sol-union.com/move.py";

    public void SendLocation(Context context)
    {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        double LATITUDE = location.getLatitude();
        double LONGITUDE = location.getLongitude();
        List<ValuePair> list = new ArrayList<ValuePair>();
        ValuePair pair1 = new ValuePair("lat", LATITUDE+"");
        ValuePair pair2 = new ValuePair("lon", LONGITUDE+"");
        ValuePair pair3 = new ValuePair("user", LoginActivity.userID);

        list.add(pair1);
        list.add(pair2);
        list.add(pair3);

        MakeHTTPRequest("POST2", URL_LONGLAT, list, listener, context);
    }

    /**********************************************+
     * Global HTTP Requests Functions
     *********************************************/
    public boolean isInternetConnected(Context context)
    {
        ConnectivityManager connMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

    public void MakeHTTPRequest(String METHOD, String url, List<ValuePair> param, HttpRequestResult listener, Context context)
    {
        HttpRequests new_request = new HttpRequests(listener);
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (isInternetConnected(context))
        {
            if(METHOD.equals("POST2"))
            {
                new_request.MakeRequest(METHOD, url, param);
            }
        }
        //Important!! After making the request, the listener automatically executes the HttpResults method.
        else
            Log.d("Error", "No network connection available.");
    }
}