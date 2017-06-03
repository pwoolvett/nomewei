package ai.zenlabs.nomewei;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by pwoolvett on 6/3/17.
 */

public class WebFetcher extends AsyncTask<String,Float,Void> {

    private WebStatusMonitor listener;
    private List<String> numberList;

    interface WebStatusMonitor{
        void onStringsReady(List<String> numbers);
    }

    WebFetcher(WebStatusMonitor listener){
        this.listener = listener;
    }

    @Override
    protected void onProgressUpdate(Float... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected Void doInBackground(String... params) {
        String urlString = params[0];
        numberList = getTextFromWeb(urlString);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        listener.onStringsReady(numberList);
        cleanUp();
    }

    private void cleanUp() {
        listener=null;
        numberList = null;
        System.gc();
    }


    private List<String> getTextFromWeb(String urlString){
        URLConnection feedUrl;
        List<String> placeAddress = new ArrayList<>();

        try{
            feedUrl = new URL(urlString).openConnection();
            InputStream is = feedUrl.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line = null;

            while ((line = reader.readLine()) != null) // read line by line
            {
                placeAddress.add(line); // add line to list
            }
            is.close(); // close input stream

            return placeAddress; // return whatever you need
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }
}
