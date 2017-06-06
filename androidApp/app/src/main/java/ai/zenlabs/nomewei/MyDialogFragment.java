package ai.zenlabs.nomewei;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.app.DialogFragment;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 *
 * Created by pwoolvett on 6/3/17.
 */

public class MyDialogFragment extends DialogFragment {
    private NewNumberSource listener;

    interface NewNumberSource{
        void onWebSelected();
        void onCallLogSelected();
        void onContactSelected();
    }

    public MyDialogFragment setListener(NewNumberSource listener){
        this.listener = listener;
        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dialog, container, false);

        // Watch for button clicks.
        Button callLogBtnId = (Button)v.findViewById(R.id.callLogBtnId);
        callLogBtnId.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener.onCallLogSelected();
                dismiss();
            }
        });

        Button contactBtnId = (Button)v.findViewById(R.id.contactBtnId);
        contactBtnId.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener.onContactSelected();
                dismiss();
            }
        });

        Button webBtnId = (Button)v.findViewById(R.id.webBtnId);
        webBtnId.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener.onWebSelected();
                dismiss();
            }
        });

        return v;
    }

}
