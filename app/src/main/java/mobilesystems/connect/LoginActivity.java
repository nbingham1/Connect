package mobilesystems.connect;

import android.app.Activity;
//import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import mobilesystems.connect.utils.MovesAPI;
import mobilesystems.connect.utils.MovesAccess;

import mobilesystems.connect.utils.ServerAPI;
import mobilesystems.connect.utils.ServerAccess;

/**
 * Created by yellow on 11/5/14.
 */
public class LoginActivity extends ActionBarActivity implements MovesAccess,ServerAccess {

    private LoginButton facebookLogin;
    private TextView facebookStatus;
    private TextView movesStatus;
    private Session my_session;

    private UiLifecycleHelper uiHelper;
    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");

    public static String movesUserID = "";
    private String facebookUserID;
    private String facebookUserName;

    // Moves
    private static final int MOVES_AUTHORIZE = 1;

    MovesAPI.MovesAuth movesAuth = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        /*******************
         * Facebook Connect
         *******************/

        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(this, statusCallback);
        uiHelper.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        facebookStatus = (TextView) findViewById(R.id.user_name);
        facebookLogin = (LoginButton) findViewById(R.id.fb_login_button);
        facebookLogin.setReadPermissions(Arrays.asList("user_friends", "user_likes"));
        facebookLogin.setUserInfoChangedCallback(new UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) {
                if (user != null) {
                    facebookStatus.setText("Hello, " + user.getName());
                    facebookUserID = user.getId();
                    facebookUserName =  user.getName();
                } else {
                    facebookStatus.setText("Facebook not connected.");
                }
            }
        });

        movesStatus = (TextView) findViewById(R.id.moves_status);
        movesStatus.setText("Moves not connected.");


        /*try { // if hash key doesn't work!
            PackageInfo info = getPackageManager().getPackageInfo(
                    "mobilesystems.connect",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.i("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }*/

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private Session.StatusCallback statusCallback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state,
                         Exception exception) {
            if (state.isOpened()) {
                Log.i("FacebookSampleActivity", "Facebook session opened");

                my_session = session;
                if(!movesUserID.equals("")){
                    requestFacebookFriends(my_session);
                    requestFacebookInterests(my_session);
                }
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

                        new ServerAPI(LoginActivity.this).execute("friends", movesUserID, facebookUserName, facebookUserID, response.toString().substring(response.toString().indexOf("={") + 1, response.toString().indexOf("}, e")));
                    }
                }
        ).executeAsync();
    }

    private void requestFacebookInterests(Session session) {
        Log.i("HH", "FI");
        new Request(
                session,
                "/me/likes",
                null,
                HttpMethod.GET,
                new Request.Callback() {
                    public void onCompleted(Response response) {
                        Log.i("LL", response.toString().substring(response.toString().indexOf("={")+1, response.toString().indexOf("}, e")));

                        new ServerAPI(LoginActivity.this).execute("interests", movesUserID, response.toString().substring(response.toString().indexOf("={") + 1, response.toString().indexOf("}, e")));
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
    protected void onStop() {
        super.onStop();
        uiHelper.onStop();
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
            case MOVES_AUTHORIZE:
                Uri resultUri = data.getData();
                if (resultCode == RESULT_OK)
                    new MovesAPI(this).execute("access", resultUri.toString());
                else
                    movesStatus.setText("Moves Denied Access");
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
            movesAuth = gson.fromJson(token, MovesAPI.MovesAuth.class);
            movesUserID = movesAuth.user_id;
            if(my_session != null) {
                requestFacebookFriends(my_session);
                requestFacebookInterests(my_session);
            }

            movesStatus.setText("Moves Authenticated");



            (new ServerAPI(this)).execute("auth", movesAuth.access_token);

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

    /*******************************************************
     * Method called after the MovesAPI has been executed.
     * See MovesAPI:java
     *******************************************************/
    public void doReceiveServer(String cmd, String token) {
        /*Boolean hh = false;
        String test = "";
        Log.d("WTF", token);
        JsonElement jelement = new JsonParser().parse(token);
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
        view.setText(test);*/
        Toast.makeText(LoginActivity.this, token, Toast.LENGTH_LONG).show();
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
                .appendQueryParameter("redirect_uri", MovesAPI.URL_REDIRECT)
                .appendQueryParameter("scope", "location activity")
                .appendQueryParameter("state", String.valueOf(SystemClock.uptimeMillis())).build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivityForResult(intent, MOVES_AUTHORIZE);
        } catch (ActivityNotFoundException e) {
            movesStatus.setText("Please install Moves.");
        }
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
            case android.R.id.home:
                Intent resultIntent = new Intent();
                if (movesAuth != null) {
                    Gson gson = new Gson();
                    resultIntent.putExtra("movesAuth", gson.toJson(movesAuth));
                }
                if (facebookUserID != null)
                    resultIntent.putExtra("facebookUserID", facebookUserID);
                if (facebookUserName != null)
                    resultIntent.putExtra("facebookUserName", facebookUserName);

                Log.d("ConnectLogin", resultIntent.getExtras().toString());

                setResult(RESULT_OK, resultIntent);
                finish();
            return true;

        }

        return super.onOptionsItemSelected(item);
    }
}
