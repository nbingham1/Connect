package mobilesystems.connect.utils;

/**
 * Created by Oliver on 10/23/2014.
 */

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Oliver on 9/15/2014.
 */
public class HttpRequests extends AsyncTask<String, Void, String> {

    private HttpRequestResult listener;

    private String METHOD;
    private String url;
    private String param;
    private List<ValuePair> params;
    private String token;
    private Bitmap bm;

    public HttpRequests(HttpRequestResult listener){
        this.listener=listener;
    }

    @Override
    protected String doInBackground(String... urls) {

        // params comes from the execute() call: params[0] is the url.
        try {
            if(METHOD.equals("GET"))
                return downloadUrl(this.url);
            else if(METHOD.equals("POST"))
                return sendPost(url,param);
            else if(METHOD.equals("POST2"))
                return sendPostMult(url, token, params);
            else if(METHOD.equals("GETAUTH"))
                return sendGetAuth(url, token);
        } catch (Exception e) {
            e.printStackTrace();
            return "Unable to retrieve web page. URL may be invalid."+e;
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        listener.HttpRequestResult(result);
    }

    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        // Only display the first 1000 characters of the retrieved
        // web page content.
        //int len = 1000;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod(METHOD);
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            is = conn.getInputStream();

            // Convert the InputStream into a string
            //String contentAsString = readIt(is, len);
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            return total.toString();

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private String sendPost(String myurl, String param) throws Exception {

        URL url = new URL(myurl);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();

        conn.setDoOutput(true);
        try{
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(param);
            writer.flush();
            String line;
            String line2 = "";
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = reader.readLine()) != null) {
                line2+=line;
                System.out.println(line);
            }
            writer.close();
            reader.close();
            return line2;
        }catch (Exception e)
        {
            int err_code = conn.getResponseCode();
            return err_code+"";
        }
    }

    public String sendGetAuth(String getURL, String token)
    {
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet(getURL);
            //get.setHeader("Content-Type", "application/x-zip");
            get.addHeader("Authorization", "Bearer " + token);
            Log.d("TOKEN", token);
            HttpResponse responseGet = client.execute(get);
            HttpEntity resEntityGet = responseGet.getEntity();
            BufferedReader reader = new BufferedReader(new InputStreamReader(resEntityGet.getContent(), "UTF-8"));
            String sResponse = reader.readLine();
            return sResponse;

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }

    }


    public String sendPostMult(String myurl, String token, List<ValuePair> params) throws Exception {

        try {

            Log.d("HEL,)", "H");
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(myurl);
            post.addHeader("Authorization", "Bearer " + token);

            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(params.size());
            for (ValuePair param : params) {
                nameValuePairs.add(new BasicNameValuePair(param.getValue(), param.getName()));
                Log.d(param.getValue(), param.getName());
            }

            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = client.execute(post);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            String sResponse = reader.readLine();
            return sResponse;

        }catch(Exception e)
        {
            e.printStackTrace();
            return "ERROR";
        }
    }

    public String sendPost(String myurl, List<ValuePair> params) throws Exception {

        try {

            Log.d("HEL,)", "H");
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(myurl);


            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(params.size());
            for (ValuePair param : params) {
                nameValuePairs.add(new BasicNameValuePair(param.getValue(), param.getName()));
                Log.d(param.getValue(), param.getName());
            }

            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = client.execute(post);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            String sResponse = reader.readLine();
            return sResponse;

        }catch(Exception e)
        {
            e.printStackTrace();
            return "ERROR";
        }
    }

    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        return stream.toByteArray();
    }

    public void MakeRequest(String METHOD, String url)
    {
        this.METHOD = METHOD;
        this.url = url;
        execute(METHOD);
    }

    public void MakeRequest(String METHOD, String url, String param)
    {
        this.url = url;
        this.METHOD = METHOD;
        this.param = param;
        execute("POST");
    }

    public void MakeRequest(String METHOD, String url, List<ValuePair> params)
    {
        this.url = url;
        this.METHOD = METHOD;
        this.params = params;
        execute(METHOD);
    }

    public void MultiPart(String url, String token, Bitmap bm)
    {
        this.url = url;
        this.METHOD = "MULTIPART";
        this.token = token;
        this.bm = bm;
        execute("MULTIPART");
    }
}