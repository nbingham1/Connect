package mobilesystems.connect.utils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import mobilesystems.connect.LoginActivity;
import mobilesystems.connect.R;

/**
 * Created by Oliver on 11/30/2014.
 */
public class AlarmReceiver extends BroadcastReceiver
{

    public static Activity activity;


    @Override
    public void onReceive(Context context, Intent intent)
    {

        SendLocation(context);
        Log.d("HH", "LOCATION SENT");
    }

    HttpRequestResult listener = new HttpRequestResult() {
        @Override
        public void HttpRequestResult(String result) {

            Boolean hh = false;
            String test = "";
            Log.d("WTF", result);
            JsonElement jelement = new JsonParser().parse(result);
            JsonArray jarray = jelement.getAsJsonArray();
            for(JsonElement element : jarray)
            {
                hh = true;
                Log.d("Recommendations: ", element.toString().replace("\"", ""));
                test +=  element.toString().replace("\"", "")+"\n";
            }

            if(!hh)
            {
               test = "No recommendations";
            }
            TextView view = (TextView) activity.findViewById(R.id.rec);
            view.setText(test);
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