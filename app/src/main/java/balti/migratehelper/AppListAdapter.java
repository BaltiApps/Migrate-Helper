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
import static balti.migratehelper.GetJsonFromData.IS_PERMISSIBLE;
import static balti.migratehelper.GetJsonFromData.PERM_CHECK;


/**
 * Created by sayantan on 8/10/17.
 */

public class AppListAdapter extends BaseAdapter {
    Vector<JSONObject> appList;
    LayoutInflater layoutInflater;
    Context context;


    OnCheck onCheck;

    AppListAdapter(Context context, Vector<JSONObject> appList) {
        this.context = context;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        onCheck = (OnCheck) context;
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

        final JSONObject appItem = appList.get(i);

        TextView appName = view.findViewById(R.id.appName);
        try {
            String label = appItem.getString("app_name") + " [" + appItem.getString("version") + "]";
            appName.setText(label);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ImageView icon = view.findViewById(R.id.appIcon);
        try {
            new SetAppIcon(icon).execute(appItem.getString("icon"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final CheckBox app, data, permissions;

        app = view.findViewById(R.id.appCheckbox);
        try {
            app.setChecked(appItem.getBoolean(APP_CHECK));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        data = view.findViewById(R.id.dataCheckbox);
        try {
            data.setChecked(appItem.getBoolean(DATA_CHECK));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        permissions = view.findViewById(R.id.permissionsCheckbox);
        try {
            permissions.setChecked(appItem.getBoolean(PERM_CHECK));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final String apkName[] = new String[]{"NULL"};
        final String dataName[] = new String[]{"NULL"};
        final boolean perm[] = new boolean[]{false};

        try {
            apkName[0] = appItem.getString("apk");
            dataName[0] = appItem.getString("data");
            perm[0] = appItem.getBoolean("permissions");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (apkName[0].equals("NULL")) {
            app.setVisibility(View.INVISIBLE);
        }
        if (dataName[0].equals("NULL")) {
            data.setVisibility(View.INVISIBLE);
        }
        if (!perm[0]) {
            permissions.setVisibility(View.INVISIBLE);
        }

        if (data.isChecked() && !apkName[0].equals("NULL")) {

            app.setChecked(true);
            app.setEnabled(false);
            try {
                appItem.put(APP_CHECK, true);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else if (!apkName[0].equals("NULL")) {
            app.setEnabled(true);
        }

        if (perm[0] && (apkName[0].equals("NULL") || app.isChecked() || data.isChecked())) {
            permissions.setEnabled(true);
            try {
                appItem.put(IS_PERMISSIBLE, true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            permissions.setEnabled(false);
            try {
                appItem.put(IS_PERMISSIBLE, false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        permissions.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                try {
                    appItem.put(PERM_CHECK, isChecked);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                onCheck.onCheck(appList);

            }
        });

        app.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                try {
                    appItem.put(APP_CHECK, b);

                    if (perm[0]) {
                        if (b || data.isChecked()) {
                            permissions.setEnabled(true);
                            appItem.put(IS_PERMISSIBLE, true);
                        } else {
                            permissions.setChecked(false);
                            permissions.setEnabled(false);
                            appItem.put(IS_PERMISSIBLE, false);
                        }
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
                    appItem.put(DATA_CHECK, b);

                    if (perm[0]) {

                        if (b || apkName[0].equals("NULL") || app.isChecked()) {
                            permissions.setEnabled(true);
                            appItem.put(IS_PERMISSIBLE, true);
                        } else {
                            permissions.setChecked(false);
                            permissions.setEnabled(false);
                            appItem.put(IS_PERMISSIBLE, false);
                        }

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
                    if (appItem.getBoolean(APP_CHECK) && appItem.getBoolean(DATA_CHECK) && appItem.getBoolean(PERM_CHECK)) {
                        data.setChecked(false);
                        app.setChecked(false);
                        permissions.setChecked(false);
                        permissions.setEnabled(false);
                        appItem.put(IS_PERMISSIBLE, false);
                    } else {

                        if (!apkName[0].equals("NULL")) {
                            app.setChecked(true);
                        }
                        if (!dataName[0].equals("NULL")) {
                            data.setChecked(true);
                        }
                        if (perm[0] && (apkName[0].equals("NULL") || app.isChecked() || data.isChecked())) {
                            permissions.setEnabled(true);
                            permissions.setChecked(true);
                            appItem.put(IS_PERMISSIBLE, true);
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

    void checkAllApp(boolean check) {
        for (int i = 0; i < appList.size(); i++) {
            try {
                if (!appList.get(i).getString("apk").equals("NULL")) {
                    appList.get(i).put(APP_CHECK, check);

                    boolean r = appList.get(i).getBoolean("permissions") && (check || appList.get(i).getBoolean(DATA_CHECK));

                    appList.get(i).put(IS_PERMISSIBLE, r);
                    if (!r) appList.get(i).put(PERM_CHECK, false);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        onCheck.onCheck(appList);
    }

    void checkAllData(boolean check) {
        for (int i = 0; i < appList.size(); i++) {
            try {
                if (!appList.get(i).getString("data").equals("NULL")) {
                    appList.get(i).put(DATA_CHECK, check);

                    boolean r = appList.get(i).getBoolean("permissions") && (check
                            || appList.get(i).getBoolean(APP_CHECK)
                            || appList.get(i).getString("apk").equals("NULL"));

                    appList.get(i).put(IS_PERMISSIBLE, r);
                    if (!r) appList.get(i).put(PERM_CHECK, false);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        onCheck.onCheck(appList);
    }

    void checkAllPermissions(boolean check) {
        for (int i = 0; i < appList.size(); i++) {
            try {
                if (appList.get(i).getBoolean("permissions")) {

                    boolean r1 = appList.get(i).getString("apk").equals("NULL")
                            || appList.get(i).getBoolean(DATA_CHECK)
                            || appList.get(i).getBoolean(APP_CHECK);

                    boolean r2 = check && r1;

                    appList.get(i).put(IS_PERMISSIBLE, r1);
                    appList.get(i).put(PERM_CHECK, r2);

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        onCheck.onCheck(appList);
    }

    Vector<JSONObject> sortByAppName(Vector<JSONObject> appList) {
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
