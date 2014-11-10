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

/**
 * Created by yellow on 11/5/14.
 */
public class LoginActivity extends Activity {
    private LoginButton loginBtn;
    private TextView userName;

    private UiLifecycleHelper uiHelper;
    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");

    // Moves

    private static final String TAG = "Connect";
    private static final String CLIENT_ID = "BHJJXLewp3VFBhgOY1T7NVlyXGsOtMF1";
    private static final String REDIRECT_URI = "http://www.sol-union.com/moves/auth.php";
    private static final int REQUEST_AUTHORIZE = 1;

    public class MovesAccessToken {
        MovesAccessToken() {}

        String access_token;
        String token_type;
        long expires_in;
        String refresh_token;
        String user_id;
    }

    MovesAccessToken accessToken = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
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

//        try { // if hash key doesn't work!
//            PackageInfo info = getPackageManager().getPackageInfo(
//                    "mobilesystems.connect",
//                    PackageManager.GET_SIGNATURES);
//            for (Signature signature : info.signatures) {
//                MessageDigest md = MessageDigest.getInstance("SHA");
//                md.update(signature.toByteArray());
//                Log.i("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
//            }
//        } catch (PackageManager.NameNotFoundException e) {
//
//        } catch (NoSuchAlgorithmException e) {
//
//        }
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

    public boolean checkPermissions() {
        Session s = Session.getActiveSession();
        if (s != null) {
            return s.getPermissions().contains("publish_actions");
        } else
            return false;
    }

    public void requestPermissions() {
        Session s = Session.getActiveSession();
        if (s != null)
            s.requestNewPublishPermissions(new Session.NewPermissionsRequest(
                    this, PERMISSIONS));
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

        switch (requestCode) {
            case REQUEST_AUTHORIZE:
                Uri resultUri = data.getData();
                if (resultCode == RESULT_OK)
                    new MovesAPI().execute("access", resultUri.toString());
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

    public class MovesAPI extends AsyncTask<String, Void, String> {
        String cmd = "";

        protected String doInBackground(String... cmds) {
            cmd = cmds[0];

            String url = "https://api.moves-app.com/api/1.1";
            if (cmds[0].equalsIgnoreCase("access")) {
                url = cmds[1];
            } else if (cmds[0].equalsIgnoreCase("refresh")) {
                url = REDIRECT_URI + "?refresh_token=" + cmds[1];
            } else if (cmds[0].equalsIgnoreCase("validate")) {
                url = "https://api.moves-app.com/oauth/v1/tokeninfo?access_token=" + cmds[1];
            } else if (cmds[0].equalsIgnoreCase("profile")) {
                url += "/user/profile";
            } else if (cmds[0].equalsIgnoreCase("support")) {
                url += "/activities";
            } else {
                url += "/user/" + cmds[0] + "/daily";
                if (cmds[1].equalsIgnoreCase("date")) {
                    url += "/" + cmds[2];
                } else if (cmds[1].equalsIgnoreCase("range")) {
                    url += "?from=" + cmds[2] + "&to=" + cmds[3];
                } else if (cmds[1].equalsIgnoreCase("count")) {
                    url += "?pastDays=" + cmds[2];
                }
            }

            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
            StringBuilder builder = new StringBuilder();

            try {
                // defaultHttpClient
                HttpResponse response = httpClient.execute(httpGet);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == 200) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(content));
                    String line;
                    while ((line = reader.readLine()) != null)
                        builder.append(line);
                    return builder.toString();
                } else
                    Log.e("MovesAPI", "HTTP failed with status code " + Integer.toString(statusCode));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            LoginActivity.this.doReceiveMoves(cmd, result);
        }
    }

    private void doReceiveMoves(String cmd, String token) {
        if (cmd.equalsIgnoreCase("access")) {
            Gson gson = new Gson();
            accessToken = gson.fromJson(token, MovesAccessToken.class);
            Toast.makeText(LoginActivity.this, "Moves Authenticated", Toast.LENGTH_SHORT).show();
        } else if (cmd.equalsIgnoreCase("refresh")) {

        } else if (cmd.equalsIgnoreCase("validate")) {

        } else if (cmd.equalsIgnoreCase("profile")) {

        } else if (cmd.equalsIgnoreCase("support")) {

        } else if (cmd.equalsIgnoreCase("summary")) {

        } else if (cmd.equalsIgnoreCase("activities")) {

        } else if (cmd.equalsIgnoreCase("places")) {

        } else if (cmd.equalsIgnoreCase("storyline")) {

        } else {
            Toast.makeText(LoginActivity.this, "Unrecognized Moves command '" + cmd + "'", Toast.LENGTH_SHORT).show();
        }
    }

    public void connectMoves(View view) {
        Uri uri = new Uri.Builder()
                .scheme("moves")
                .authority("app")
                .path("/authorize")
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("redirect_uri", REDIRECT_URI)
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
