package ai.zenlabs.nomewei;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import ai.zenlabs.nomewei.utils.ContactUtils;

/**
 *
 * Created by pwoolvett on 6/3/17.
 * TODO: 6/3/17 append all (new) available numbers to the blacklist contact
 */

class NumberBlacklister extends AsyncTask<Void,Void,Void> {
    private static final String TAG = NumberBlacklister.class.getSimpleName();

    private static final String BLACKLIST_CONTACT_NAME = "NO ME WEI";
    private static final int    REQUEST_CREATE_CONTACT = 1;
    private static final String LABEL_PHONE            = "NOMEWEI";
    private static final int    RAW_CONTACT_ID_DEFAULT = 69;


    private List<String> incomingNumbers    = null;
    private BlacklistMonitor listener       = null;
    private WeakReference<Context> mContext = null;
    private int contactId;

    //    private List<String> numberList;
//    private int oldCheckpoint;
//    private int newCheckpoint=-1;
//
    interface BlacklistMonitor{
        void onBlacklistUpdated();
    }

    NumberBlacklister(Context context, BlacklistMonitor listener, List<String> incomingNumbers){
        mContext = new WeakReference<>(context);
        this.listener = listener;
        this.incomingNumbers = incomingNumbers;
    }

    @Override
    protected Void doInBackground(Void... params) {
        //ContactUtils.getPhoneNumbers(getContext(), BLACKLIST_CONTACT_NAME);
        contactId = ContactUtils.getContactId(getContext(), BLACKLIST_CONTACT_NAME);
        // TODO: 6/4/17 make  contactExists return contact uri, and use it in contactContainsNumber
        if(contactId==-1){
            //Uri contactUri = createEmptyBlackListContact(getContext());
            Uri contactUri = createEmptyBlackListContact(getContext());
            contactId = Integer.parseInt(contactUri.getPath().substring(14)); // 14 marks the end of "/raw_contacts/", so we get the Id which comes next
        }
        List<String> numbers = ContactUtils.getPhoneNumbers(getContext(), BLACKLIST_CONTACT_NAME);

        for(String currentNumber:incomingNumbers){
            if (  (numbers==null) || ( !numbers.contains(currentNumber) )  ){
                addNumberToBlackListContact(currentNumber);
            }
        }
        return null;
    }

    private void addNumberToBlackListContact(String currentNumber) {
        ContactUtils.addNumberToContact(getContext(),contactId, currentNumber);
    }


    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        listener.onBlacklistUpdated();
        cleanUp();
    }

    private void cleanUp() {
        listener        = null;
        incomingNumbers = null;
        mContext        = null;
        System.gc();
    }

    private Uri createEmptyBlackListContact(Context context){
        ContentProviderResult[] res = null;
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        int rawContactInsertIndex = ops.size();

        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());

        //Display name/Contact name
        ops.add(ContentProviderOperation
                .newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Contacts.Data.RAW_CONTACT_ID,
                        rawContactInsertIndex)
                .withValue(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, BLACKLIST_CONTACT_NAME)
                .build());

//        ops.add(ContentProviderOperation.newInsert(ContactsContract.PhoneLookup.SEND_TO_VOICEMAIL)
////                .withValueBackReference(ContactsContract.Contacts.Data.RAW_CONTACT_ID,
////                        rawContactInsertIndex)
////                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
//                .withValue(ContactsContract.PhoneLookup.SEND_TO_VOICEMAIL, 1)
//                .build());

        try {
            res = context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            ContentValues values = new ContentValues();
            values.put(ContactsContract.PhoneLookup.SEND_TO_VOICEMAIL, 1);
            String[] kk = {BLACKLIST_CONTACT_NAME};
            context.getContentResolver().update(ContactsContract.Contacts.CONTENT_URI, values, ContactsContract.Contacts.DISPLAY_NAME + "= ?", kk);
        } catch (RemoteException | OperationApplicationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return res[0].uri;
    }

    /*private Uri createEmptyBlackListContact(){
        ContentValues values = new ContentValues();
        values.put(ContactsContract.Data.RAW_CONTACT_ID, RAW_CONTACT_ID_DEFAULT);
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, "1-800-GOOG-411");
        values.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM);
        values.put(ContactsContract.CommonDataKinds.Phone.LABEL, LABEL_PHONE);
        Uri dataUri = getContext().getContentResolver().insert(android.provider.ContactsContract.Data.CONTENT_URI, values);
        return dataUri;
    }*/

    private void createEmptyBlackListContactIntent() {
        Intent contactIntent = new Intent(ContactsContract.Intents.Insert.ACTION);
        contactIntent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

        contactIntent.putExtra(ContactsContract.Intents.Insert.NAME, BLACKLIST_CONTACT_NAME);

        getActivity().startActivityForResult(contactIntent, REQUEST_CREATE_CONTACT);
    }

    private void createBlackListContact(String phoneNumber) {
        Intent contactIntent = new Intent(ContactsContract.Intents.Insert.ACTION);
        contactIntent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

        contactIntent
                .putExtra(ContactsContract.Intents.Insert.NAME, BLACKLIST_CONTACT_NAME)
                .putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber);

        getActivity().startActivityForResult(contactIntent, REQUEST_CREATE_CONTACT);
    }

    /*

    public static void addNumberToContact(Context context, int contactRawId, String number) throws RemoteException, OperationApplicationException {
        addInfoToAddressBookContact(
                context,
                contactRawId,
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_OTHER,
                number
        );
    }
    */

    public static void addEmailToContact(Context context, int contactRawId, String email) throws RemoteException, OperationApplicationException {
        addInfoToAddressBookContact(
                context,
                contactRawId,
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.TYPE,
                ContactsContract.CommonDataKinds.Email.TYPE_OTHER,
                email
        );
    }

    public static void addURLToContact(Context context, int contactRawId, String url) throws RemoteException, OperationApplicationException {
        addInfoToAddressBookContact(
                context,
                contactRawId,
                ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Website.URL,
                ContactsContract.CommonDataKinds.Website.TYPE,
                ContactsContract.CommonDataKinds.Website.TYPE_OTHER,
                url
        );
    }

    private static void addInfoToAddressBookContact(Context context, int contactRawId, String mimeType, String whatToAdd, String typeKey, int type, String data) throws RemoteException, OperationApplicationException {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValue(ContactsContract.Data.RAW_CONTACT_ID, contactRawId)
                .withValue(ContactsContract.Data.MIMETYPE, mimeType)
                .withValue(whatToAdd, data)
                .withValue(typeKey, type)
                .build());
        ContentProviderResult[] result = context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        Log.i(TAG, "addInfoToAddressBookContact: "+ result.toString());
    }

    private Context getContext() {
        return mContext.get();
    }

    Activity getActivity() {
        return (Activity) getContext();
    }

}
