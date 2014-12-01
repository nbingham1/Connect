package mobilesystems.connect;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.FriendPickerFragment;
import com.facebook.widget.LoginButton;
import com.facebook.widget.LoginButton.UserInfoChangedCallback;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;

import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpGet;

import mobilesystems.connect.utils.AlarmReceiver;
import mobilesystems.connect.utils.HttpRequestResult;
import mobilesystems.connect.utils.HttpRequests;
import mobilesystems.connect.utils.MovesAPI;
import mobilesystems.connect.utils.MovesAccess;

import mobilesystems.connect.utils.ValuePair;

/**
 * Created by yellow on 11/5/14.
 */
public class LoginActivity extends Activity implements MovesAccess{



    private static final String URL_FRIENDS =  "http://connect.sol-union.com/friends.py";
    private LoginButton loginBtn;
    private TextView userName;
    private Session my_session;

    private UiLifecycleHelper uiHelper;
    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");
    private String userID_FB;
    private String USER_NAME;

    // Moves
    private static final int REQUEST_AUTHORIZE = 1;
    MovesAPI.MovesAccessToken accessToken = null;

    public static String userID = "";

    private HttpRequestResult listener = new HttpRequestResult() {
        @Override
        public void HttpRequestResult(String result) {
          //  Log.d("HHA", result);

        }
    };





    @Override
    public void onCreate(Bundle savedInstanceState) {
        /*******************
         * Facebook Connect
         *******************/
        AlarmReceiver.activity = this;

        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(this, statusCallback);
        uiHelper.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        userName = (TextView) findViewById(R.id.user_name);
        loginBtn = (LoginButton) findViewById(R.id.fb_login_button);
        loginBtn.setReadPermissions(Arrays.asList("user_friends"));
        loginBtn.setUserInfoChangedCallback(new UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) {
                if (user != null) {
                    userName.setText("Hello, " + user.getName());
                    userID_FB = user.getId();
                    USER_NAME =  user.getName();
                } else {
                    userName.setText("You are not logged");
                }
            }
        });


        try { // if hash key doesn't work!
            PackageInfo info = getPackageManager().getPackageInfo(
                    "mobilesystems.connect",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.i("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }
    }

    private Session.StatusCallback statusCallback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state,
                         Exception exception) {
            if (state.isOpened()) {
                Log.i("FacebookSampleActivity", "Facebook session opened");

                my_session = session;
                if(!userID.equals(""))
                    requestFacebookFriends(session);
            } else if (state.isClosed()) {
                Log.i("FacebookSampleActivity", "Facebook session closed");
            }
        }
    };

    private void requestFacebookFriends(Session session) {
        Log.i("HH", "JH");
        new Request(
                session,
                "/me/friends",
                null,
                HttpMethod.GET,
                new Request.Callback() {
                    public void onCompleted(Response response) {



                        Log.i("KK", response.toString().substring(response.toString().indexOf("={")+1, response.toString().indexOf("}, e")));

                        List<ValuePair> list = new ArrayList<ValuePair>();
                        ValuePair pair1 = new ValuePair("user", userID);
                        ValuePair pair4 = new ValuePair("name", USER_NAME);
                        ValuePair pair3 = new ValuePair("facebook", userID_FB);
                        ValuePair pair2 = new ValuePair("json", response.toString().substring(response.toString().indexOf("={") + 1, response.toString().indexOf("}, e")));
                        list.add(pair1);
                        list.add(pair2);
                        list.add(pair4);
                        list.add(pair3);
                        MakeHTTPRequest("POST2", URL_FRIENDS, list, listener);
            /* handle the result */
                    }
                }
        ).executeAsync();
    }



    @Override
    public void onResume() {
        super.onResume();
        uiHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);

        /*******************************************************
         * Results for Intents...
         *
         * 1. REQUEST_AUTHORIZE: Moves API Request authorized
         *******************************************************/
        switch (requestCode) {
            case REQUEST_AUTHORIZE:
                Uri resultUri = data.getData();
                if (resultCode == RESULT_OK)
                    new MovesAPI(this).execute("access", resultUri.toString());
                else
                    Toast.makeText(this, "Moves denied access", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        uiHelper.onSaveInstanceState(savedState);
    }

    /*******************************************************
     * Method called after the MovesAPI has been executed.
     * See MovesAPI:java
     *******************************************************/
    public void doReceiveMoves(String cmd, String token) {
        if (cmd.equalsIgnoreCase("access")) {

            Gson gson = new Gson();
            accessToken = gson.fromJson(token, MovesAPI.MovesAccessToken.class);

            userID = accessToken.user_id;
            if(my_session != null)
                requestFacebookFriends(my_session);
            AlarmManager alarmManager=(AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis(),600000,
                    pendingIntent);

            Toast.makeText(LoginActivity.this, "Moves Authenticated", Toast.LENGTH_SHORT).show();
            new MovesAPI(this).execute("places", "count", accessToken.access_token, "2");
        } else if (cmd.equalsIgnoreCase("refresh")) {
            Toast.makeText(LoginActivity.this, token, Toast.LENGTH_LONG).show();
        } else if (cmd.equalsIgnoreCase("validate")) {
            Toast.makeText(LoginActivity.this, token, Toast.LENGTH_LONG).show();
        } else if (cmd.equalsIgnoreCase("profile")) {
            Toast.makeText(LoginActivity.this, token, Toast.LENGTH_LONG).show();
        } else if (cmd.equalsIgnoreCase("support")) {
            Toast.makeText(LoginActivity.this, token, Toast.LENGTH_LONG).show();
        } else if (cmd.equalsIgnoreCase("summary")) {
            Toast.makeText(LoginActivity.this, token, Toast.LENGTH_LONG).show();
        } else if (cmd.equalsIgnoreCase("activities")) {
            Toast.makeText(LoginActivity.this, token, Toast.LENGTH_LONG).show();
        } else if (cmd.equalsIgnoreCase("places")) {
            Toast.makeText(LoginActivity.this, token, Toast.LENGTH_LONG).show();
        } else if (cmd.equalsIgnoreCase("storyline")) {
            Toast.makeText(LoginActivity.this, token, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(LoginActivity.this, "Unrecognized Moves command '" + cmd + "'", Toast.LENGTH_SHORT).show();
        }
    }

    /***************************************
     * Function Called when the user clicks
     * the Connect to Moves Button
     ***************************************/
    public void connectMoves(View view) {

        Uri uri = new Uri.Builder()
                .scheme("moves")
                .authority("app")
                .path("/authorize")
                .appendQueryParameter("client_id", MovesAPI.CLIENT_ID)
                .appendQueryParameter("redirect_uri", MovesAPI.REDIRECT_URI)
                .appendQueryParameter("scope", "location activity")
                .appendQueryParameter("state", String.valueOf(SystemClock.uptimeMillis())).build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivityForResult(intent, REQUEST_AUTHORIZE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Please install the Moves app", Toast.LENGTH_SHORT).show();
        }
    }


    /**********************************************+
     * Global HTTP Requests Functions
     *********************************************/
    public boolean isInternetConnected()
    {
        ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

    public void MakeHTTPRequest(String METHOD, String url, List<ValuePair> param, HttpRequestResult listener)
    {
        HttpRequests new_request = new HttpRequests(listener);
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (isInternetConnected())
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
