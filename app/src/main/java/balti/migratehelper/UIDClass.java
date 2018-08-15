package balti.migratehelper;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.List;

/**
 * Created by sayantan on 23/10/17.
 */

public class UIDClass {
    private Context context;

    UIDClass(Context context) {
        this.context = context;
    }

    int getUid(String pkg)
    {
        int uid = -1;
        List<PackageInfo> appList = context.getPackageManager().getInstalledPackages(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
        for (int i = 0; i < appList.size(); i++) {
            PackageInfo info = appList.get(i);
            if (pkg.equals(info.packageName))
            {
                uid = info.applicationInfo.uid;
                break;
            }
        }
        return uid;
    }
}
