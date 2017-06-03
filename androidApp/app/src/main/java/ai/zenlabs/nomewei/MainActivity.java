package ai.zenlabs.nomewei;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.HashSet;
import java.util.List;

//import android.provider.ContactsContract;

public class MainActivity extends AppCompatActivity implements Loader.OnLoadCanceledListener<Cursor>, Loader.OnLoadCompleteListener<Cursor>,MyDialogFragment.NewNumberSource, WebFetcher.WebStatusMonitor {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int    REQUEST_CREATE_CONTACT      = 1;
    private static final int    REQUEST_CALL_LOG_LAUNCH     = 2;
    private static final int    REQUEST_COMBINED_PERMISSION = 3;
    private static final int    REQUEST_PICK_CONTACT        = 4;
    private static final int    ID_CURSOR_LOADER            = 55;

    private static final String    PERMISSION_READ_CALL_LOG = Manifest.permission.READ_CALL_LOG;
    private static final String    PERMISSION_INTERNET      = Manifest.permission.INTERNET;
    private static final String[]  permsToRequest = {PERMISSION_READ_CALL_LOG,PERMISSION_INTERNET};

    private static HashSet<String> grantedPermissions = new HashSet<>();

    private static final String BLACKLIST_URL = "https://raw.githubusercontent.com/pwoolvett/nomewei/master/blacklist";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initDbsAndLogic();
        initViews();
        initVariables();
    }

    private void initDbsAndLogic() {
        handlePermissions();
    }

    private void handlePermissions() {
        String[] required = {"",""};

        int howManu=-1;
        for (String permission: permsToRequest) {
            if (!grantedPermissions.contains(permission)){
                required[++howManu] = permission;
            }
        }

        ActivityCompat.requestPermissions(this, required, REQUEST_COMBINED_PERMISSION);

    }

    private void initVariables() {

    }

    private void initViews() {
        setContentView(R.layout.activity_main);
        initToolbar();
        initFab();
    }

    private void initFab() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleFabClick();
            }
        });
    }

    private void handleFabClick() {
        //pickContact();
        //launchCallLog();
        displayDialog();
        //requestCallLog();
        //createOrUpdateBlackList();
    }


    void displayDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        new MyDialogFragment().setListener(this).show(ft, "dialog");
    }

    private void createOrUpdateBlackList() {
        Intent contactIntent = new Intent(ContactsContract.Intents.Insert.ACTION);
        contactIntent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

        contactIntent
                .putExtra(ContactsContract.Intents.Insert.NAME, "NO CONTESTAR")
                .putExtra(ContactsContract.Intents.Insert.PHONE, "+56912345678");

        startActivityForResult(contactIntent, REQUEST_CREATE_CONTACT);
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch(id){
            case R.id.action_settings:
                Log.d(TAG, "onOptionsItemSelected: yaay setings selected");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case REQUEST_CALL_LOG_LAUNCH:
                handleRequestCallLogLaunch(resultCode, data);
                break;
            case REQUEST_PICK_CONTACT:
                handleRequestPickContact(resultCode,data);
                break;
        }
    }

    private void handleRequestPickContact(int resultCode, Intent data) {
        Log.d(TAG, "handleRequestPickContact() called with: resultCode = [" + resultCode + "], data = [" + data + "]");
    }

    private void handleRequestCallLogLaunch(int resultCode, Intent data) {
        Log.d(TAG, "handleRequestCallLogLaunch() called with: resultCode = [" + resultCode + "], data = [" + data + "]");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_COMBINED_PERMISSION:
                handleRequestReadCallLog(permissions, grantResults);
        }
    }

    private void handleRequestReadCallLog(String[] permissions, int[] grantResult) {
        boolean morePermissionsRequired = false;
        int pos=-1;
        for(String permission:permissions){
            pos++;
            if (grantResult[pos]==PackageManager.PERMISSION_GRANTED){
                grantedPermissions.add(permission);
            }else{
                Log.w(TAG, "handleRequestReadCallLog: permission denied for " + permission);
                morePermissionsRequired = true;
            }
        }

        if (morePermissionsRequired) handlePermissions();
    }

    @Override
    public void onWebSelected() {
        new WebFetcher(this).execute(BLACKLIST_URL);
    }

    @Override
    public void onStringsReady(List<String> numbers) {
        Log.d(TAG, "onStringsReady() called with: numbers = [" + numbers + "]");
    }

    @Override
    public void onCallLogSelected() {
        requestCallLog();
    }

    private void requestCallLog() {
        Uri allCalls = Uri.parse("content://call_log/calls");
        CursorLoader c = new CursorLoader(getApplicationContext(), allCalls, null, null, null, null);
        c.registerListener(ID_CURSOR_LOADER, this);
        c.startLoading();
    }

    @Override
    public void onLoadCanceled(Loader<Cursor> loader) {}


    @Override    @SuppressWarnings("unused")
    public void onLoadComplete(Loader<Cursor> loader, Cursor c) {
        if (loader.getId()== ID_CURSOR_LOADER){
            while(c.moveToNext()){
                String num= c.getString(c.getColumnIndex(CallLog.Calls.NUMBER));// for  number
                String name= c.getString(c.getColumnIndex(CallLog.Calls.CACHED_NAME));// for name
                String duration = c.getString(c.getColumnIndex(CallLog.Calls.DURATION));// for duration
                int type = Integer.parseInt(c.getString(c.getColumnIndex(CallLog.Calls.TYPE)));// for call type, Incoming or out going
                Log.d(TAG, "onLoadComplete: done");
            }
        }else{
            Log.wtf(TAG, "onLoadComplete: not a registered cursor id");
        }
    }


    void launchCallLog(){
        Intent showCallLog = new Intent();
        showCallLog.setAction(Intent.ACTION_VIEW);
        showCallLog.setType(CallLog.Calls.CONTENT_TYPE);
        startActivityForResult(showCallLog, REQUEST_CALL_LOG_LAUNCH);
    }

    void pickContact(){
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
    }
}
