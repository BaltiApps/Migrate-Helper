package balti.migratehelper;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;
import java.util.Vector;

/**
 * Created by sayantan on 23/10/17.
 */

public class UIDClass {
    private Context context;
    Vector<String> core = null;
    String permissionListPath = "";

    public UIDClass(Context context) {
        this.context = context;
        core = new Vector<>(1);
    }

    void generateAllUids(){
        generateUids(null);
    }

    private void generateUids(Vector<String> specific){
        core.removeAllElements();
        List<PackageInfo> appList = context.getPackageManager().getInstalledPackages(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
        for (int i = 0; i < appList.size(); i++){
            ApplicationInfo info = appList.get(i).applicationInfo;
            if (isPresent(specific, info.packageName)) {
                String comm = "chown " + info.uid + ":" + info.uid + " -R /data/data/" + info.packageName + "\nrestorecon -RF /data/data/" + info.packageName + "\n";
                if (isUserApplication(info)) {
                    String src = info.sourceDir;
                    src = src.substring(0, src.lastIndexOf('/'));
                    String srcName = src.substring(src.lastIndexOf('/') + 1);
                    comm = comm + "chown " + info.uid + ":" + info.uid + " -R /data/app/" + srcName + "\nrestorecon -RF /data/app/" + srcName + "\n";
                }
                else {
                    String src = info.sourceDir;
                    String srcName = src.substring(0, src.lastIndexOf('/'));
                    comm = comm + "chown " +  info.uid + ":" + info.uid + " -R " + srcName + "\nrestorecon -RF " + srcName + "\n";
                }
                core.addElement(comm);
            }
        }
    }

    private boolean isUserApplication(ApplicationInfo info){
        boolean isUser = true;
        if (info.sourceDir.startsWith("/system")) isUser = false;
        return isUser;
    }

    private boolean isPresent(Vector<String> specific, String pkg)
    {
        if (specific == null) return true;
        for (String s : specific)
            if (s.equals(pkg)) return true;
        return false;
    }

    void generateSpecificUids(){
        permissionListPath = Environment.getExternalStorageDirectory() + "/Migrate/";
        String line;
        (new File(permissionListPath)).mkdirs();
        permissionListPath = permissionListPath + "permissionList";
        try {
            (new File(permissionListPath)).createNewFile();
            Process copy = Runtime.getRuntime().exec("su -c mv -f /cache/permissionList " + permissionListPath);
            copy.waitFor();
            Log.d("MigrateHelper", copy.exitValue() + "");
            if (copy.exitValue() == 0) {
                Vector<String> packages = new Vector<>(1);
                BufferedReader reader = new BufferedReader(new FileReader(new File(permissionListPath)));
                while ((line = reader.readLine()) != null){
                    packages.addElement(line);
                }
                generateUids(packages);
            }
            else core = new Vector<>(0);
        } catch (Exception e) {
            e.printStackTrace();
            core = new Vector<>(0);
        }
    }
}
