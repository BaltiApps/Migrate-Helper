package balti.migratehelper;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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

    private String TEMP_DIR_NAME = "/data/balti.migrate";

    UIDClass(Context context) {
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
                String target = "/data/data/" + info.packageName;
                String comm = "chown " + info.uid + ":" + info.uid + " -Rf " + target + "\nrestorecon -RF " + target + "\n";
                if (isUserApplication(info)) {
                    String src = info.sourceDir;
                    src = src.substring(0, src.lastIndexOf('/'));
                    String srcName = src.substring(src.lastIndexOf('/') + 1);
                    target = "/data/app/" + srcName;
                    comm = comm + "chown " + info.uid + ":" + info.uid + " -Rf " + target + "\nrestorecon -RF " + target + "\n";
                }
                else {
                    String src = info.sourceDir;
                    String srcName = src.substring(0, src.lastIndexOf('/'));
                    target = srcName;
                    comm = comm + "chown " +  info.uid + ":" + info.uid + " -Rf " + srcName + "\nrestorecon -RF " + srcName + "\n";
                }
                comm = comm + "echo PERM: " + target + "\n";
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
        File permissionListFile = new File(context.getExternalCacheDir(), "permissionList");
        permissionListPath = permissionListFile.getAbsolutePath();
        String line;
        try {

            File pListMoveScript = new File(context.getFilesDir(), "pListMove.sh");
            BufferedWriter bw = new BufferedWriter(new FileWriter(pListMoveScript));
            bw.write("#!sbin/sh" + "\n\n" +
                    "mv -f " + TEMP_DIR_NAME + "/permissionList " + permissionListPath + "\n" +
                    "rm " + pListMoveScript.getAbsolutePath() + "\n"

            );
            bw.close();

            Process copy = Runtime.getRuntime().exec("su -c sh " + pListMoveScript.getAbsolutePath());
            copy.waitFor();
            Log.d("MigrateHelper", copy.exitValue() + "");
            if (copy.exitValue() == 0) {
                Vector<String> packages = new Vector<>(1);
                BufferedReader reader = new BufferedReader(new FileReader(permissionListFile));
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
