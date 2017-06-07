package ai.zenlabs.nomewei;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by pwoolvett on 6/3/17.
 * TODO: 6/3/17 implement file versioning so that the connection is interrupted when download
 * TODO:    reaches the specified mark, eg, a pattern like checkpoint_x_
 */

class WebFetcher extends AsyncTask<String,Integer,Void> {

    private static final int ID_NOTIFICATION_PROGRESS_UPDATE = 33;

    private WebStatusMonitor       listener      = null;
    private List<String>           numberList    = null;
    //private int                    oldCheckpoint = -1;
    //private int                    newCheckpoint = -1;
    private WeakReference<Context> mContext      = null;

    private NotificationCompat.Builder mBuilder       = null;
    private NotificationManager        mNotifyManager = null;

    interface WebStatusMonitor{
        void onStringsReady(List<String> numbers);//, int checkpoint);
        void onLineRead(float percentage);
    }

    WebFetcher(Context context, WebStatusMonitor listener){//}, int checkpoint){
        mContext = new WeakReference<>(context);
        this.listener = listener;
        //oldCheckpoint = checkpoint;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mNotifyManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(getContext());
        mBuilder.setContentTitle("Blacklist Update")
                .setContentText("Download in progress")
                .setSmallIcon(R.drawable.ic_sync_black_24dp);

    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        listener.onLineRead(values[0]);
        mBuilder.setProgress(100, values[0], false);
        // Displays the progress bar for the first tim
        mNotifyManager.notify(ID_NOTIFICATION_PROGRESS_UPDATE, mBuilder.build());

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
        mBuilder.setContentText("Download complete")
                // Removes the progress bar
                .setProgress(0,0,false);
        mNotifyManager.notify(ID_NOTIFICATION_PROGRESS_UPDATE, mBuilder.build());
        listener.onStringsReady(numberList);//, newCheckpoint);
        cleanUp();
    }

    private void cleanUp() {
        listener        = null;
        numberList      = null;
        mContext        = null;
        mBuilder        = null;
        mNotifyManager  = null;
        System.gc();
    }


    private List<String> getTextFromWeb(String urlString){
        URLConnection feedUrl;
        List<String> placeAddress = new ArrayList<>();
        try{
            int currentBytes = 0;
            feedUrl = new URL(urlString).openConnection();
            float sizeInBytes = (float)feedUrl.getContentLength();

            InputStream is = feedUrl.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;

            while ((line = reader.readLine()) != null) {// read line by line
                currentBytes += line.length();
                if (line.contains("checkpoint")){
                    // TODO: 6/6/17 impelent checkpoint handling to avoid downloading extra numbers
                    /*
                    int currentCheckpoint = parseCheckpoint(line);
                    publishProgress((int) (currentBytes*100/sizeInBytes) );
                    if (currentCheckpoint>newCheckpoint){ // update the checkpoint to the new value
                        newCheckpoint = currentCheckpoint;
                    }

                    if (currentCheckpoint<=oldCheckpoint){// we've reached the lastcheckpoint, no need to keep downloading
                        is.close();
                        return placeAddress;
                    }
                    */
                }else{
                    placeAddress.add(line); // add line to list
                }

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

    /**
     * Obtain the checkpoint identifier available in the current line.
     * In order to do this, the identifier is a counter of the commits pushed to the URL.
     * This identifier is encoded in lines of the form "checkpoint_x_\n", where x would be the
     * identifier. The parsint, then, consists of retrieving the integer between the underscores.
     *
     * @param line the string containing the checkpoint identifier
     * @return the found checkpoint identfier (or 0 if there were issues)
     */
    private int parseCheckpoint(String line) {
        int lastUnderscorePosition = line.lastIndexOf("_");
        if (lastUnderscorePosition==-1) return 0;
        int secondLastUnderscorePosition = line.substring(0,lastUnderscorePosition).lastIndexOf("_");
        if (secondLastUnderscorePosition==-1) return 0;
        return Integer.parseInt(line.substring(secondLastUnderscorePosition+1,lastUnderscorePosition));
    }

    private Context getContext() {
        return mContext.get();
    }

    @SuppressWarnings("unused")
    Activity getActivity() {
        return (Activity) getContext();
    }

}
