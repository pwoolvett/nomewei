package ai.zenlabs.nomewei;

import android.Manifest;
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
import java.util.logging.Handler;

//import android.provider.ContactsContract;

public class MainActivity extends AppCompatActivity implements Loader.OnLoadCanceledListener<Cursor>, Loader.OnLoadCompleteListener<Cursor> {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int CREATE_CONTACT_REQUEST = 1;
    private static final int CURSOR_LOADER_ID = 2;
    private static final int REQUEST_READ_CALL_LOG = 3;
    private static final String PERMISSION_READ_CALL_LOG = Manifest.permission.READ_CALL_LOG;
    private static HashSet<String> grantedPermissions = new HashSet<>();

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
        if (!grantedPermissions.contains(PERMISSION_READ_CALL_LOG)){
            ActivityCompat.requestPermissions(this, new String[] {PERMISSION_READ_CALL_LOG}, REQUEST_READ_CALL_LOG);
        }
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
        getRecents();
        //createOrUpdateBlackList();
    }

    private void getRecents() {
        Uri allCalls = Uri.parse("content://call_log/calls");
        //Cursor c = managedQuery(allCalls, null, null, null, null);

        CursorLoader c = new CursorLoader(getApplicationContext(), allCalls, null, null, null, null);
        c.registerListener(CURSOR_LOADER_ID, this);
        c.startLoading();
//        c.
//
//        String num= c.getString(c.getColumnIndex(CallLog.Calls.NUMBER));// for  number
//        String name= c.getString(c.getColumnIndex(CallLog.Calls.CACHED_NAME));// for name
//        String duration = c.getString(c.getColumnIndex(CallLog.Calls.DURATION));// for duration
//        int type = Integer.parseInt(c.getString(c.getColumnIndex(CallLog.Calls.TYPE)));// for call type, Incoming or out going
    }

    @Override
    public void onLoadCanceled(Loader<Cursor> loader) {}

    @Override
    public void onLoadComplete(Loader<Cursor> loader, Cursor c) {
        if (loader.getId()==CURSOR_LOADER_ID){
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

    private void createOrUpdateBlackList() {
        Intent contactIntent = new Intent(ContactsContract.Intents.Insert.ACTION);
        contactIntent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

        contactIntent
                .putExtra(ContactsContract.Intents.Insert.NAME, "NO CONTESTAR")
                .putExtra(ContactsContract.Intents.Insert.PHONE, "+56912345678");

        startActivityForResult(contactIntent, CREATE_CONTACT_REQUEST);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_READ_CALL_LOG:
                handleRequestReadCallLog(permissions[0], grantResults[0]);
        }
    }

    private void handleRequestReadCallLog(String permission, int grantResult) {
        if (grantResult==PackageManager.PERMISSION_GRANTED){
            grantedPermissions.add(permission);
        }else{
            Log.w(TAG, "handleRequestReadCallLog: permission denied");
            handlePermissions();
        }
    }
}
