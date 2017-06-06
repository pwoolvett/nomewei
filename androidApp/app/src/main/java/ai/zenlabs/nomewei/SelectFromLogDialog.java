package ai.zenlabs.nomewei;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;

import ai.zenlabs.nomewei.utils.ContactUtils;

/**
 *
 * Created by pwoolvett on 6/3/17.
 */

public class SelectFromLogDialog extends DialogFragment {
    List<ContactUtils.LogSummary> callLogNumbers;

    public SelectFromLogDialog(){

    }

    public SelectFromLogDialog setList(List<ContactUtils.LogSummary> callLogNumbers) {
        this.callLogNumbers = callLogNumbers;
        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View vv = inflater.inflate(R.layout.call_log_dialog, container, false);

        ListView listView = (ListView)vv.findViewById(R.id.listViewId);
        listView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        return vv;
    }

}
