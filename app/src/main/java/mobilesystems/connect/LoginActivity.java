package mobilesystems.connect;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
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
import com.facebook.widget.LoginButton;
import com.facebook.widget.LoginButton.UserInfoChangedCallback;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;

import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

import mobilesystems.connect.utils.MovesAPI;
import mobilesystems.connect.utils.MovesAccess;

/**
 * Created by yellow on 11/5/14.
 */
public class LoginActivity extends Activity implements MovesAccess{
    private LoginButton loginBtn;
    private TextView userName;

    private UiLifecycleHelper uiHelper;
    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");

    // Moves
    private static final int REQUEST_AUTHORIZE = 1;
    MovesAPI.MovesAccessToken accessToken = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        /*******************
         * Facebook Connect
         *******************/

        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(this, statusCallback);
        uiHelper.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        userName = (TextView) findViewById(R.id.user_name);
        loginBtn = (LoginButton) findViewById(R.id.fb_login_button);
        loginBtn.setUserInfoChangedCallback(new UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) {
                if (user != null) {
                    userName.setText("Hello, " + user.getName());
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
            } else if (state.isClosed()) {
                Log.i("FacebookSampleActivity", "Facebook session closed");
            }
        }
    };

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

}
