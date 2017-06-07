package ai.zenlabs.nomewei.utils;

import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.AnimRes;
import android.support.annotation.BoolRes;
import android.support.annotation.ColorInt;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * Created by pwoolvett on 6/4/17.
 */

public class ContactUtils {
    private static final String TAG = ContactUtils.class.getSimpleName();

    public static final int ID_CURSOR_LOADER_CALL_LOG = 55;
    public static final int ID_CURSOR_LOADER_GET_NUMBERS = 66;


    private static boolean contactExists(Context context, Uri lookupUri){
        String[] mPhoneNumberProjection = {ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.NUMBER, ContactsContract.PhoneLookup.DISPLAY_NAME};
        // TODO: 6/4/17 what happens if there is more than one contact which contain the same number?
        Cursor cur = context.getContentResolver().query(lookupUri, mPhoneNumberProjection, null, null, null);
        try {
            if (cur.moveToFirst()) {
                return true;
            }
        } finally {
            if (cur != null)
                cur.close();
        }
        return false;
    }

    public static void addNumberToContact(Context context, int contactUniqueId, String number) {

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValue(ContactsContract.Data.RAW_CONTACT_ID, contactUniqueId)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_OTHER)
                .build()
        );

        // Update
        try{
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public static void updateContactForwardCallsToVoiceMail(Context context, int contactUniqueId, boolean forceVoiceMail) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValue(ContactsContract.Data.RAW_CONTACT_ID, contactUniqueId)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.PhoneLookup.SEND_TO_VOICEMAIL, forceVoiceMail?1:0)
                .build()
        );

        // Update
        try{
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static int getContactId(Context context, String name) {
        int ret = -1;

        List<Integer> rets = null;
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(name.trim()));
        Cursor mapContact = context.getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup._ID}, null, null, null);
        if (  (mapContact!=null)  ){
            if (mapContact.moveToFirst()){
                rets = new ArrayList<>();
                do{
                    rets.add(Integer.parseInt(  mapContact.getString(mapContact.getColumnIndex(ContactsContract.Contacts._ID)))  );
                }while(mapContact.moveToNext());
            }
            mapContact.close();
            if (  (rets==null) || (rets.size()!=1)  ){
                Log.wtf(TAG, "getContactId: cuack");
            }else{
                ret = rets.get(0);
            }
        }
        return ret;
    }

    /*public static int getContactId(Context context, String name) {
        Cursor c = getContactCursor(context, name);
        List<String> ret = null;
        if (c.moveToFirst()){
            ret = new ArrayList<>();
            int numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            do{
                ret.add( c.getString(numberIndex) );
            }while(c.moveToNext());
            c.close();
        }

        if (  (ret==null) || (ret.size()!=1)  ){
            Log.wtf(TAG, "getContactId: ret size !=1");
            return -1;
        }else{
            return Integer.parseInt(ret.get(0));
        }
    }*/

    public static List<String> getPhoneNumbers(Context context, String name) {
        List<String> ret = null;
        Cursor c  = getContactCursor(context, name);
        if (c.moveToFirst()) {
            ret = new ArrayList<>();
            int numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            do{
                ret.add( c.getString(numberIndex) );
            }while(c.moveToNext());
        }
        c.close();
        return ret;
    }

    public static Cursor getContactCursor(Context context, String contactName){
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" = '" + contactName +"'";
        String[] projection = new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor c = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, null, null);
        return c;
    }

    /*
    public static boolean contactExistsv2(Context context, Loader.OnLoadCompleteListener<Cursor> listener) {

        Uri allContacts = Uri.parse("content://call_log/calls");
        CursorLoader c = new CursorLoader(context, allContacts, null, null, null, null);
        c.registerListener(ID_CURSOR_LOADER_CALL_LOG, listener);
        c.startLoading();
    }

    public static boolean contactExistv3(final Context context){
        boolean exist = false;
        Thread contactLoader = new Thread(new Runnable() {
            @Override
            public void run() {
                contactExistsv2(context, this);
            }
            public void onLoadComplete(Loader<Cursor> loader, Cursor c) {

            }
        });
        contactLoader.start();
        try {
            contactLoader.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        return exist;
    }
    */

    public static String getNumberFromIntent(Context context, Intent data) {
        Uri contactUri = data.getData();
        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor cursor = context.getContentResolver().query(contactUri,
                projection,
                null, null, null);
        // If the cursor returned is valid, get the phone number
        if (cursor != null && cursor.moveToFirst()) {
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            String number = cursor.getString(numberIndex);
            return number;
        }
        return null;
    }

    public static void requestCallLog(Context context, Loader.OnLoadCompleteListener<Cursor> listener) {
        Uri allCalls = Uri.parse("content://call_log/calls");
        CursorLoader c = new CursorLoader(context, allCalls, null, null, null, null);
        c.registerListener(ID_CURSOR_LOADER_CALL_LOG, listener);
        c.startLoading();
    }

    /*
    static class ContactChecker extends AsyncTask<String,Void,Boolean> implements Loader.OnLoadCompleteListener<Cursor> {
        private static final int ID_CURSOR_LOADER_GET_ALL_CONTACTS = 11;
        WeakReference<Context> mContext;
        boolean loadComplete = false;
        boolean exist = false;


        ContactChecker(Context context){
            mContext = new WeakReference<>(context);
        }
        @Override
        protected Boolean doInBackground(String... params) {

            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    Uri allContacts = Uri.parse("content://call_log/calls");
                    CursorLoader c = new CursorLoader(getContext(), allContacts, null, null, null, null);
                    c.registerListener(ID_CURSOR_LOADER_GET_ALL_CONTACTS, ContactChecker.this);
                    c.startLoading();
                }
            });
            th.start();

            try {
                th.join(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            exist =
            return exist;
        }

        private Context getContext(){
            return mContext.get();
        }

        @Override
        public void onLoadComplete(Loader<Cursor> loader, Cursor data) {

        }
    }
    */

    /**
     *
     *
     */
//    /** Call log type for incoming calls. */
//    public static final int INCOMING_TYPE = 1;
//    /** Call log type for outgoing calls. */
//    public static final int OUTGOING_TYPE = 2;
//    /** Call log type for missed calls. */
//    public static final int MISSED_TYPE = 3;
//    /** Call log type for voicemails. */
//    public static final int VOICEMAIL_TYPE = 4;
//    /** Call log type for calls rejected by direct user action. */
//    public static final int REJECTED_TYPE = 5;
//    /** Call log type for calls blocked automatically. */
//    public static final int BLOCKED_TYPE = 6;
    public static class LogSummary{

        String num;
        String name;
        String duration;
        String date;
        String time;
        int type;

        @SuppressLint("SimpleDateFormat")
        public LogSummary(String num, String name, String duration, long date, int type){
            this.num = num;
            this.name = name;
            this.duration = duration;
            Date aDate = new Date(date);
            this.date   = new SimpleDateFormat("dd/MM/yyyy").format(aDate);
            this.time =  new SimpleDateFormat("HH:mm").format(aDate);
            this.type = type;
        }

        public String toString(){
//            return num+" (" + date + " )";
            String res = "";
            if (  (name!=null) && (!name.equals(""))){
                res+=name +"\n";
            }

            if (  (num!=null) && (!num.equals(""))){
                res+=num+"\n";
            }

            if (  (date!=null) && (!date.equals(""))){
                res+=" (" + date +" - ";
            }

            if (  (time!=null) && (!time.equals(""))){
                res+=time;
            }
            res+=")";

            return res;

        }

        public String getNumber() {
            return num;
        }
    }
}
