package balti.migratehelper;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import static balti.migratehelper.CommonTools.ACTION_END_ALL;

public class UninstallActivity extends Activity {

    CheckBox changeDpiCheckbox;
    CheckBox rebootCheckbox;
    LinearLayout uninstallView;
    LinearLayout temporaryDisableView;

    int dpiValue = 0;

    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uninstall_activity);

        editor = getSharedPreferences("main", MODE_PRIVATE).edit();

        changeDpiCheckbox = findViewById(R.id.change_dpi_checkbox);
        rebootCheckbox = findViewById(R.id.reboot_checkbox);
        uninstallView = findViewById(R.id.uninstall_view);
        temporaryDisableView = findViewById(R.id.temporary_disable_view);

        dpiValue = getIntent().getIntExtra("dpiValue", 0);

        changeDpiCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    rebootCheckbox.setChecked(true);
                    rebootCheckbox.setEnabled(false);
                }
                else {
                    rebootCheckbox.setEnabled(true);
                }
            }
        });

        if (dpiValue > 0) changeDpiCheckbox.setChecked(true);
        else changeDpiCheckbox.setVisibility(View.GONE);

        final Intent uninstallIntent = new Intent(this, UninstallService.class);

        uninstallView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uninstallIntent.putExtra("doUninstall", true);
                if (changeDpiCheckbox.isChecked())
                    uninstallIntent.putExtra("dpiValue", dpiValue);
                else uninstallIntent.putExtra("dpiValue", 0);
                uninstallIntent.putExtra("doReboot", rebootCheckbox.isChecked());

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(uninstallIntent);
                }
                else {
                    startService(uninstallIntent);
                }

                sendBroadcast(new Intent(ACTION_END_ALL));
                finish();
            }
        });

        temporaryDisableView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                stopService(new Intent(UninstallActivity.this, StupidStartupService.class));
                editor.putBoolean("temporaryDisable", true);
                editor.commit();

                uninstallIntent.putExtra("doUninstall", false);
                if (changeDpiCheckbox.isChecked())
                    uninstallIntent.putExtra("dpiValue", dpiValue);
                else uninstallIntent.putExtra("dpiValue", 0);
                uninstallIntent.putExtra("doReboot", rebootCheckbox.isChecked());

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(uninstallIntent);
                }
                else {
                    startService(uninstallIntent);
                }

                sendBroadcast(new Intent(ACTION_END_ALL));
                finish();
            }
        });
    }
}
