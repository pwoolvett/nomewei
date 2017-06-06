package ai.zenlabs.nomewei;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import ai.zenlabs.nomewei.utils.ContactUtils;

import static android.content.Intent.EXTRA_ALLOW_MULTIPLE;


//import android.provider.ContactsContract;

public class MainActivity extends AppCompatActivity
        implements Loader.OnLoadCanceledListener<Cursor>,
        Loader.OnLoadCompleteListener<Cursor>,
        MyDialogFragment.NewNumberSource,
        WebFetcher.WebStatusMonitor,
        NumberBlacklister.BlacklistMonitor {

    //region----------------  Class Constants  ------------------------

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_CALL_LOG_LAUNCH = 2;
    private static final int REQUEST_COMBINED_PERMISSION = 3;
    private static final int REQUEST_PICK_CONTACT = 4;

    private static final String PERMISSION_READ_CALL_LOG = Manifest.permission.READ_CALL_LOG;
    private static final String PERMISSION_INTERNET = Manifest.permission.INTERNET;
    private static final String PERMISSION_CONTACTS = Manifest.permission.WRITE_CONTACTS;
    private static final String[] permsToRequest = {PERMISSION_READ_CALL_LOG, PERMISSION_CONTACTS, PERMISSION_INTERNET};

    private static final String CURRENT_CHECKPOINT = "CURRENT_CHECKPOINT";

    private static final String BLACKLIST_URL = "https://raw.githubusercontent.com/pwoolvett/nomewei/master/blacklist";//endregion


    //region----------------  Member Variables  ------------------------

    private int currentCheckpoint;//endregion


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initDbsAndLogic();
        initViews();
        initVariables();
    }

    private void initDbsAndLogic() {
        handlePermissions();
        recoverVariables();
    }

    private void recoverVariables() {
        currentCheckpoint = getPreferences(Context.MODE_PRIVATE).getInt(CURRENT_CHECKPOINT,0);
    }

    private void handlePermissions() {
        String[] required = {"","", ""};

        int howMany=0;
        for (String permission: permsToRequest) {
            if(  PackageManager.PERMISSION_DENIED  ==  ActivityCompat.checkSelfPermission(getApplicationContext(), permission)  ){
                required[-1 + ++howMany] = permission;
            }
        }

        if (howMany!=0){
            ActivityCompat.requestPermissions(this, Arrays.copyOf(required,howMany), REQUEST_COMBINED_PERMISSION);
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


    //region----------------  Activity Result Handling  ------------------------

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
        if (data!=null){
            List<String> newNumber = new ArrayList<>();
            newNumber.add( ContactUtils.getNumberFromIntent(getApplicationContext(), data) );
            updateBlackListContact(newNumber);
        }

    }

    private void handleRequestCallLogLaunch(int resultCode, Intent data) {
        Log.d(TAG, "handleRequestCallLogLaunch() called with: resultCode = [" + resultCode + "], data = [" + data + "]");
        // TODO: 6/4/17 retrieve numberss and call blacklistupdater
    }//endregion

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
            if (  PackageManager.PERMISSION_DENIED  ==  grantResult[pos]  ){
                Log.w(TAG, "handleRequestReadCallLog: permission denied for " + permission);
                morePermissionsRequired = true;
            }
        }

        if (morePermissionsRequired) handlePermissions();
    }


    //region----------------  Call Log Handling  ------------------------

    @Override
    public void onCallLogSelected() {
        ContactUtils.requestCallLog(getApplicationContext(), this);
    }

    @Override
    public void onContactSelected() {
        pickContact();
    }

    @Override
    public void onLoadCanceled(Loader<Cursor> loader) {
    }


    @Override
    public void onLoadComplete(Loader<Cursor> loader, Cursor c) {
        switch (loader.getId()){
            case ContactUtils.ID_CURSOR_LOADER_CALL_LOG:
                handleResultReadCallLog(/*loader,*/ c);
                break;
            /*case ContactUtils.ID_CURSOR_LOADER_GET_NUMBERS:
                handleResultPickContact(c);
                break;*/
            default:
                Log.wtf(TAG, "onLoadComplete: i didn't ask for this");
        }
    }

    /*private void handleResultPickContact(Cursor contactsCursor) {
        //HashMap<String,String> selectedContacts= new HashMap<>();
        List<String> numbers = new ArrayList<>();

        if (contactsCursor.moveToFirst()) {
            do {
                String contactId = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                String contactName = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                boolean hasPhone = "1".equalsIgnoreCase(contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)));
                if (hasPhone) {
   //                 Cursor phones = getContentResolver().query(
  //                          ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
 //                           ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
//                            null, null);
                    Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},

                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " +
                                    ContactsContract.CommonDataKinds.Phone.TYPE + " = " +
                                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,

                            new String[]{contactId},
                            null);

                    if (phones.moveToFirst()) {
                        do {
                            String cNumber = phones.getString(phones.getColumnIndex("data1"));
                            numbers.add(cNumber);
                        } while (phones.moveToNext());
                    }
                } else {
                    Log.d(TAG, "handleResultPickContact: selected ocntact has no phone");
                }
            } while (contactsCursor.moveToNext());
        }

        //updateBlackListContact(newNumbers);
    }*/


    /*Cursor cursorPhone = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},

            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " +
                    ContactsContract.CommonDataKinds.Phone.TYPE + " = " +
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,

            new String[]{contactID},
            null);

        if (cursorPhone.moveToFirst()) {
        contactNumber = cursorPhone.getString(cursorPhone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));*/



    private void handleResultReadCallLog(Cursor c) {
        List<ContactUtils.LogSummary> callLogNumbers;
        if (c.moveToFirst()){
            callLogNumbers = new ArrayList<>();
            String num;
            String name;
            String duration;
            long date;
            int type;

            do{
                num      = c.getString(c.getColumnIndex(CallLog.Calls.NUMBER));// for  number
                name     = c.getString(c.getColumnIndex(CallLog.Calls.CACHED_NAME));// for name
                duration = c.getString(c.getColumnIndex(CallLog.Calls.DURATION));// for duration
                date     = c.getLong(c.getColumnIndex(CallLog.Calls.DATE));// for duration
                type     = Integer.parseInt(c.getString(c.getColumnIndex(CallLog.Calls.TYPE)));// for call type, Incoming or out going
                callLogNumbers.add(new ContactUtils.LogSummary(num,name,duration,date,type));
            }while(c.moveToNext());

            if( !callLogNumbers.isEmpty() ){
                displayLogDialog(callLogNumbers);
            }else{
                Log.wtf(TAG, "handleResultReadCallLog: cursor.movetofirst true, but log empty");
            }
        }else{
            Log.wtf(TAG, "handleResultReadCallLog: cursor.movetofirst is false");
        }
    }

    private void displayLogDialog(final List<ContactUtils.LogSummary> callLogNumbers) {
        final String[] dummyNames = {"","","","","","",""};
        //final List<Integer> selectedItems = new ArrayList<>();
        final LinkedList<String> numbers = new LinkedList<>();
        final HashMap<String,String> selectedValues = new HashMap<>();

        int ctr=-1;
        for (ContactUtils.LogSummary summary:callLogNumbers){
            dummyNames[++ctr] = summary.toString();
            numbers.add(summary.getNumber());
            if (ctr==6){
                break;
            }
        }

        final String[] names = Arrays.copyOf(dummyNames,ctr-1);

        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setTitle("WHICH_CALLS_TITLE");//.setMessage("WHICH_CALLS_MESSAGE")

        // 3. popullate list
        builder.setMultiChoiceItems(names, null,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which,
                                        boolean isChecked) {
                        if (isChecked) {
                            // If the user checked the item, add it to the selected items
                            selectedValues.put(names[which], numbers.get(which));
                        } else if (selectedValues.containsKey(names[which])) {
                            // Else, if the item is already in the array, remove it
                            selectedValues.remove(names[which]);
                        }
                    }
        });

        // Add the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                List<String> newNumbers = new ArrayList<>();
                for (String key:selectedValues.keySet()){
                    newNumbers.add(selectedValues.get(key));
                }

                updateBlackListContact(newNumbers);
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });


        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        dialog.show();

        /*
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        new SelectFromLogDialog().setList(callLogNumbers).show(ft, "LogDalog");
        */
    }


    void launchCallLog() {
        Intent showCallLog = new Intent();
        showCallLog.setAction(Intent.ACTION_VIEW);
        showCallLog.setType(CallLog.Calls.CONTENT_TYPE);
        startActivityForResult(showCallLog, REQUEST_CALL_LOG_LAUNCH);
    }//endregion


    //region----------------  Web Download Handling  ------------------------

    @Override
    public void onWebSelected() {
        new WebFetcher(getApplicationContext(), this, currentCheckpoint).execute(BLACKLIST_URL);
    }

    @Override
    public void onStringsReady(List<String> newNumbers, int newChechpoint) {
        updateLastCheckpoint(newChechpoint);
        updateBlackListContact(newNumbers);
        Log.d(TAG, "onStringsReady() called with: numbers = [" + newNumbers + "]");
    }

    private void updateBlackListContact(List<String> newNumbers) {
        (new NumberBlacklister(getApplicationContext(), this, newNumbers)).execute();
    }

    private void updateLastCheckpoint(int newChechpoint) {
        currentCheckpoint = newChechpoint;
        // TODO: 6/4/17 also show summary on background
    }

    @Override
    public void onLineRead(float percentage) {

    }

    /**
     * Contact Updated Listener
     */
    @Override
    public void onBlacklistUpdated() {
        // TODO: 6/3/17
    }//endregion


    void pickContact(){
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(contactPickerIntent, REQUEST_PICK_CONTACT);
    }

    @Override
    protected void onPause() {
        super.onPause();
        storeVariables();
    }

    private void storeVariables() {
        SharedPreferences.Editor ed = getPreferences(MODE_PRIVATE).edit();
        ed.putInt(CURRENT_CHECKPOINT, currentCheckpoint);
        ed.apply();
    }
}
