package balti.migratehelper;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import static balti.migratehelper.GetJsonFromData.APP_CHECK;
import static balti.migratehelper.GetJsonFromData.DATA_CHECK;


/**
 * Created by sayantan on 8/10/17.
 */

public class AppListAdapter extends BaseAdapter {
    Vector<JSONObject> appList;
    LayoutInflater layoutInflater;
    Context context;


    String apkName;
    String dataName;

    OnCheck onCheck;

    AppListAdapter(Context context, Vector<JSONObject> appList)
    {
        this.context = context;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        onCheck = (OnCheck)context;
        this.appList = sortByAppName(appList);
    }

    @Override
    public int getCount() {
        return appList.size();
    }

    @Override
    public Object getItem(int i) {
        return appList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return getCount();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {

        view = layoutInflater.inflate(R.layout.app_item, null);

        TextView appName = view.findViewById(R.id.appName);
        try {
            appName.setText(appList.get(i).getString("app_name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ImageView icon = view.findViewById(R.id.appIcon);
        try {
            new SetAppIcon(icon).execute(appList.get(i).getString("icon"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final CheckBox app, data;


        app = view.findViewById(R.id.appCheckbox);
        try {
            app.setChecked(appList.get(i).getBoolean(APP_CHECK));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        app.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                try {
                    appList.get(i).put(APP_CHECK, b);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                onCheck.onCheck(appList);
            }
        });


        data = view.findViewById(R.id.dataCheckbox);
        try {
            data.setChecked(appList.get(i).getBoolean(DATA_CHECK));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        apkName = "NULL";
        dataName = "NULL";

        try {
            apkName = appList.get(i).getString("apk");
            dataName  = appList.get(i).getString("data");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (apkName.equals("NULL")){
            app.setChecked(false);
            app.setEnabled(false);
        }
        if (dataName.equals("NULL")){
            data.setChecked(false);
            data.setEnabled(false);
        }

        if (data.isChecked()) {
            app.setChecked(true);
            app.setEnabled(false);
        }
        else app.setEnabled(true);

        data.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                try {
                    appList.get(i).put(DATA_CHECK, b);
                    if (b)
                    {
                        app.setChecked(true);
                        app.setEnabled(false);
                    }
                    else {
                        app.setEnabled(true);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                onCheck.onCheck(appList);
            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    if (appList.get(i).getBoolean(APP_CHECK) && appList.get(i).getBoolean(DATA_CHECK)){
                            data.setChecked(false);
                            app.setChecked(false);
                    }
                    else {

                        if (!apkName.equals("NULL")) {
                            app.setChecked(true);
                        }
                        if (!dataName.equals("NULL")) {
                            data.setChecked(true);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        if (apkName.equals("NULL"))
            appName.setTextColor(Color.RED);

        return view;
    }

    void checkAllApp(boolean check)
    {
        for (int i = 0; i < appList.size(); i++)
        {
                try {
                    if (!appList.get(i).getString("apk").equals("NULL")) {
                        appList.get(i).put(APP_CHECK, check);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
        }
    }

    void checkAllData(boolean check)
    {
        for (int i = 0; i < appList.size(); i++)
        {
            try {
                if (!appList.get(i).getString("data").equals("NULL")) {
                    appList.get(i).put(DATA_CHECK, check);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    Vector<JSONObject> sortByAppName(Vector<JSONObject> appList){
        Vector<JSONObject> sortedAppList = appList;
        Collections.sort(sortedAppList, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject o1, JSONObject o2) {
                try {
                    return String.CASE_INSENSITIVE_ORDER.compare(o1.getString("app_name"), o2.getString("app_name"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    return 0;
                }
            }
        });
        return sortedAppList;
    }

}
