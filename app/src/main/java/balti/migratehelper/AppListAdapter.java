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
import static balti.migratehelper.GetJsonFromData.PERM_CHECK;


/**
 * Created by sayantan on 8/10/17.
 */

public class AppListAdapter extends BaseAdapter {
    Vector<JSONObject> appList;
    LayoutInflater layoutInflater;
    Context context;


    /*String apkName;
    String dataName;*/
    //boolean perm;

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
            String label = appList.get(i).getString("app_name") + " [" + appList.get(i).getString("version") + "]";
            appName.setText(label);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ImageView icon = view.findViewById(R.id.appIcon);
        try {
            new SetAppIcon(icon).execute(appList.get(i).getString("icon"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final CheckBox app, data, permissions;

        app = view.findViewById(R.id.appCheckbox);
        try {
            app.setChecked(appList.get(i).getBoolean(APP_CHECK));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        data = view.findViewById(R.id.dataCheckbox);
        try {
            data.setChecked(appList.get(i).getBoolean(DATA_CHECK));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        permissions = view.findViewById(R.id.permissionsCheckbox);
        try {
            permissions.setChecked(appList.get(i).getBoolean(PERM_CHECK));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final String apkName[] = new String[]{"NULL"};
        final String dataName[] = new String[]{"NULL"};
        final boolean perm[] = new boolean[]{false};

        try {
            perm[0] = appList.get(i).getBoolean("permissions");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            apkName[0] = appList.get(i).getString("apk");
            dataName[0]  = appList.get(i).getString("data");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (apkName[0].equals("NULL")){
            app.setChecked(false);
            app.setEnabled(false);
        }
        if (dataName[0].equals("NULL")){
            data.setChecked(false);
            data.setEnabled(false);
        }

        if (data.isChecked() && !apkName[0].equals("NULL")) {
            app.setChecked(true);
            app.setEnabled(false);
        }
        else if (!apkName[0].equals("NULL")) app.setEnabled(true);

        permissions.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    appList.get(i).put(PERM_CHECK, isChecked);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                onCheck.onCheck(appList);
            }
        });

        try {
            if (!perm[0] || !(app.isChecked() || data.isChecked())) {
                permissions.setChecked(false);
                permissions.setEnabled(false);
            }
            else if (perm[0]) permissions.setEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        app.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                try {
                    appList.get(i).put(APP_CHECK, b);

                    if (perm[0]) {
                        permissions.setEnabled(b || data.isChecked());
                        if (!(b || data.isChecked()))
                            permissions.setChecked(false);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                onCheck.onCheck(appList);
            }
        });

        data.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                try {
                    appList.get(i).put(DATA_CHECK, b);

                    if (perm[0]) {
                        permissions.setEnabled(b || app.isChecked());
                        if (!(b || app.isChecked()))
                            permissions.setChecked(false);
                    }

                    if (b && !apkName[0].equals("NULL")) {
                        app.setChecked(true);
                        app.setEnabled(false);
                    } else if (!apkName[0].equals("NULL")) {
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
                    if (appList.get(i).getBoolean(APP_CHECK) && appList.get(i).getBoolean(DATA_CHECK) && appList.get(i).getBoolean(PERM_CHECK)){
                            data.setChecked(false);
                            app.setChecked(false);
                            permissions.setChecked(false);
                            permissions.setEnabled(false);
                    }
                    else {

                        if (!apkName[0].equals("NULL")) {
                            app.setChecked(true);
                        }
                        if (!dataName[0].equals("NULL")) {
                            data.setChecked(true);
                        }
                        if (perm[0] && (app.isChecked() || data.isChecked())) {
                            permissions.setEnabled(true);
                            permissions.setChecked(true);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        if (apkName[0].equals("NULL"))
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

    void checkAllPermissions(boolean check)
    {
        for (int i = 0; i < appList.size(); i++)
        {
            try {
                if (appList.get(i).getBoolean("permissions")) {
                    if (check && (appList.get(i).getBoolean(DATA_CHECK) || appList.get(i).getBoolean(APP_CHECK)))
                        appList.get(i).put(PERM_CHECK, true);
                    else appList.get(i).put(PERM_CHECK, false);
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
