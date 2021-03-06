import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;


public abstract class HttpJsonRequest<Result>{

    public static final int GET = 1;
    public static final int POST = 2;
    public static final int PUT = 3;

    private Integer type= null;
    private String jsonData= null;
    private String url;
    private boolean use_ssl = false;
    private Context context;

    public abstract Result onDone(JSONObject json_response, int status_code);
    public abstract Result onFail(int status_code);

    public HttpJsonRequest(String url, int type, String json_data) {
        this.url = url;
        this.type = type;
        this.jsonData = json_data;
    }

    public HttpJsonRequest(Context context, String url, int type, String json_data, boolean use_ssl) {
        this.url = url;
        this.type = type;
        this.jsonData = json_data;
        this.use_ssl = use_ssl;
        this.context = context;
    }

    public Result exec() throws Exception {
        if(type == null)
            throw new Exception("Error! Indicar el tipo de llamada(POST/GET)");

        if((jsonData == null) && (type == POST))
            throw new Exception("Error! Añadir cadena json con datos");

        URL url_conn = new URL(url);

        HttpURLConnection http_connection = null;
        HttpsURLConnection https_connection = null;

        if(use_ssl) {
            https_connection = (HttpsURLConnection) url_conn.openConnection();
            SSLSocketFactory factory = newSSLFactory(context);
            if(factory == null)
                throw new Exception("Error to create trust store usin self-signed certificate file");

            https_connection.setSSLSocketFactory(factory);
            https_connection.setHostnameVerifier(new NullHostNameVerifier());

            http_connection = https_connection;
        }
        else {
            http_connection = (HttpURLConnection) url_conn.openConnection();
        }

        if((http_connection == null) && (https_connection == null))
            throw new Exception("Faied to init HttpURLConnection");

        http_connection.setUseCaches(false);
        http_connection.setRequestProperty("Content-Type", "application/json");
        http_connection.setRequestProperty("charset", "utf-8");

        if(type == POST || type == PUT){
            if(type == POST) {
                //Application.debug("POST: " + url);
                http_connection.setRequestMethod("POST");
            }
            else if(type == PUT) {
                //Application.debug("PUT: " + url);
                http_connection.setRequestMethod("PUT");
            }

            //Application.debug("Json data: " + jsonData);

            DataOutputStream outputStream = new DataOutputStream(http_connection.getOutputStream());

            byte[] utf8JsonString = jsonData.getBytes("UTF8");
            outputStream.write(utf8JsonString);
            outputStream.flush();
            outputStream.close();
        }
        else if(type == GET){
            //Application.debug("GET: " + url);
            http_connection.setRequestMethod("GET");
        }
        else
            throw new Exception("Error! Unknown request type");

        try {
            http_connection.connect();
        } catch (Exception e) {
            e.printStackTrace();

            http_connection.disconnect();
            return this.onFail(408);
        }

        int statusCode = http_connection.getResponseCode();

        if((statusCode < 200) || (statusCode > 300)){
            http_connection.disconnect();

            return this.onFail(statusCode);
        }

        JSONObject json_data;
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(http_connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            json_data = new JSONObject(response.toString());
            //Application.debug("Response: " + response.toString());
        } catch (Exception e) {
            e.printStackTrace();

            http_connection.disconnect();
            return this.onFail(statusCode);
        }

        http_connection.disconnect();
        return this.onDone(json_data, statusCode);
    }

    private SSLSocketFactory newSSLFactory(Context context) {
        InputStream in = context.getResources().openRawResource(R.raw.keystore);

        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(in, "gestiayuda_456".toCharArray());
            in.close();

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), null);

            return ctx.getSocketFactory();

        } catch (KeyStoreException | IOException | KeyManagementException | NoSuchAlgorithmException | CertificateException e) {
            e.printStackTrace();
            return null;
        }
    }
}
