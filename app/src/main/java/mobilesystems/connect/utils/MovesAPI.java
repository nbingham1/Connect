package mobilesystems.connect.utils;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import mobilesystems.connect.LoginActivity;

/**
 * Created by Oliver on 11/10/2014.
 */
public class MovesAPI extends AsyncTask<String, Void, String> {

    //Acces token received from Moves API
    public static class MovesAuth
    {
        MovesAuth() {}

        public String access_token;
        public String token_type;
        public long expires_in;
        public String refresh_token;
        public String user_id;
    }

    public static final String CLIENT_ID = "BHJJXLewp3VFBhgOY1T7NVlyXGsOtMF1";
    public static final String URL_REDIRECT = "http://connect.sol-union.com/auth.py";
    public static final String URL_AUTH =  "https://api.moves-app.com/oauth/v1";
    public static final String URL_API =  "https://api.moves-app.com/api/1.1";

    private MovesAccess activity;
    //Command user requested
    private String cmd;

    public MovesAPI(MovesAccess activity)
    {
        this.cmd = "";
        this.activity = activity;
    }

    protected String doInBackground(String... cmds)
    {
        cmd = cmds[0];

        String url = URL_API;
        if (cmds[0].equalsIgnoreCase("access")) {
            url = cmds[1];
        } else if (cmds[0].equalsIgnoreCase("refresh")) {
            url = URL_REDIRECT + "?refresh_token=" + cmds[1];
        } else if (cmds[0].equalsIgnoreCase("validate")) {
            url = URL_AUTH + "/tokeninfo?access_token=" + cmds[1];
        } else if (cmds[0].equalsIgnoreCase("profile")) {
            url += "/user/profile?access_token=" + cmds[1];
        } else if (cmds[0].equalsIgnoreCase("support")) {
            url += "/activities?access_tokens=" + cmds[1];
        } else {
            url += "/user/" + cmds[0] + "/daily";
            if (cmds[1].equalsIgnoreCase("date")) {
                url += "/" + cmds[3] + "?access_token=" + cmds[2];
            } else if (cmds[1].equalsIgnoreCase("range")) {
                url += "?access_token=" + cmds[2] + "&from=" + cmds[3] + "&to=" + cmds[4];
            } else if (cmds[1].equalsIgnoreCase("count")) {
                url += "?access_token=" + cmds[2] + "&pastDays=" + cmds[3];
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
    protected void onPostExecute(String result)
    {
        activity.doReceiveMoves(cmd, result);
    }
}
