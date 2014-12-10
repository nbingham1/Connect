package mobilesystems.connect.utils;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class ServerAPI extends AsyncTask<String, Void, String> {
    private static final String URL_UPDATE =  "http://connect.sol-union.com/update.py";
    private static final String URL_MOVE =  "http://connect.sol-union.com/move.py";
    private static final String URL_FRIENDS =  "http://connect.sol-union.com/friends.py";
    private static final String URL_INTERESTS =  "http://connect.sol-union.com/interests.py";
    private static final String URL_AUTH =  "http://connect.sol-union.com/auth.py";

    private ServerAccess activity;

    //Command user requested
    String cmd;

    public ServerAPI(ServerAccess activity)
    {
        this.cmd = "";
        this.activity = activity;
    }

    protected String doInBackground(String... cmds) {
        cmd = cmds[0];

        String url = "";
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();

        if (cmds[0].equalsIgnoreCase("auth")) {
            url = URL_AUTH;
            parameters.add(new BasicNameValuePair(cmds[1], "code"));
        } else if (cmds[0].equalsIgnoreCase("refresh")) {
            url = URL_AUTH;
            parameters.add(new BasicNameValuePair(cmds[1], "refresh_token"));
        }
        else if (cmds[0].equalsIgnoreCase("update")) {
            url = URL_UPDATE;
            parameters.add(new BasicNameValuePair(cmds[1], "user"));
        }
        else if (cmds[0].equalsIgnoreCase("move"))
        {
            url = URL_MOVE + "?user=" + cmds[1];
            parameters.add(new BasicNameValuePair(cmds[1], "user"));
            if (cmds.length >= 4) {
                parameters.add(new BasicNameValuePair(cmds[2], "lat"));
                parameters.add(new BasicNameValuePair(cmds[3], "lon"));
            }
        }
        else if (cmds[0].equalsIgnoreCase("friends"))
        {
            url = URL_FRIENDS;
            parameters.add(new BasicNameValuePair(cmds[1], "user"));
            parameters.add(new BasicNameValuePair(cmds[2], "facebook"));
            parameters.add(new BasicNameValuePair(cmds[3], "name"));
            parameters.add(new BasicNameValuePair(cmds[4], "json"));
        }
        else if (cmds[0].equalsIgnoreCase("interests"))
        {
//            return null;
            url = URL_INTERESTS;
            parameters.add(new BasicNameValuePair(cmds[1], "user"));
            parameters.add(new BasicNameValuePair(cmds[2], "json"));
        }

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);
        StringBuilder builder = new StringBuilder();

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(parameters));
            HttpResponse response = httpClient.execute(httpPost);

            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();

            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                    Log.d("Connect ServerAPI", line);
                }
                Log.i("result", builder.toString());
                return builder.toString();
            } else
                Log.e("ServerAPI", "HTTP failed with status code " + Integer.toString(statusCode));
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
        activity.doReceiveServer(cmd, result);
    }

}
