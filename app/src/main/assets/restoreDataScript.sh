#!sbin/sh

# parameters: TEMP_DIR_NAME tar.gz_file package_name

mv $1/$2 /data/data/
cd /data/data/

dataDir=$(dumpsys package $3 | grep dataDir | head -n 1 | cut -d '=' -f2)
app_uid=$(dumpsys package $3 | grep userId= | head -n 1 | cut -d '=' -f2 | cut -d ' ' -f1)

# sometimes app_uid has two lines. example: Google maps (com.google.android.apps.maps)
# so "head -n 1" takes only the first line.
# in android lollipop unnecessary extras along with app_uid. So take only first item.

if [ ! -n "$app_uid" ]; then
    echo "Failed to find package $3. Cannot restore data."
else
    rm -rf ${dataDir} 2>/dev/null

    $1/busybox tar -xzpf $2
    rm $2

    chmod 755 ${dataDir}
    chown ${app_uid}:${app_uid} -Rf ${dataDir}

    restorecon -RF ${dataDir}

    rm -rf $1/$2
fi


