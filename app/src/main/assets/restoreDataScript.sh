#!sbin/sh

# parameters: TEMP_DIR_NAME tar.gz_file package_name

#   Starting from v1.2 tar.gz files are unpacked under /data/data

#mv $1/$2 /data/data/
# Changed from v1.2

dataDir=$(dumpsys package $3 | grep dataDir | head -n 1 | cut -d '=' -f2)
app_uid=$(dumpsys package $3 | grep userId= | head -n 1 | cut -d '=' -f2 | cut -d ' ' -f1)

# sometimes app_uid has two lines. example: Google maps (com.google.android.apps.maps)
# so "head -n 1" takes only the first line.
# in android lollipop unnecessary extras along with app_uid. So take only first item.

if [ ! -n "$app_uid" ]; then
    echo "Failed to find package $3. Cannot restore data."
else
    rm -rf ${dataDir} 2>/dev/null

    # adding legacy support in v1.3

    if [ ! -e /data/data/$2 ] && [ -e $1/$2 ]; then
        cd $1
        mv $2 /data/data/$2
    fi

    cd /data/data/

    $1/busybox tar -xzpf $2
    rm $2

    chmod 755 ${dataDir}
    chown ${app_uid}:${app_uid} -Rf ${dataDir}

    restorecon -RF ${dataDir} 2>/dev/null
fi


