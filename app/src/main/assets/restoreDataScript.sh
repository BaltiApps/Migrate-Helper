#!sbin/sh

# parameters:

# TEMP_DIR_NAME_OLD
# TEMP_DIR_NAME_NEW
# tar.gz_file
# package_name

#   Starting from v1.2 tar.gz files are unpacked under /data/data

#mv $1/$2 /data/data/
# Changed from v1.2

dataDir=$(dumpsys package $4 | grep dataDir | head -n 1 | cut -d '=' -f2)
app_uid=$(dumpsys package $4 | grep userId= | head -n 1 | cut -d '=' -f2 | cut -d ' ' -f1)

# sometimes app_uid has two lines. example: Google maps (com.google.android.apps.maps)
# so "head -n 1" takes only the first line.
# in android lollipop unnecessary extras along with app_uid. So take only first item.

if [ ! -n "$app_uid" ]; then
    echo "Failed to find package $4. Cannot restore data."
else
    rm -rf ${dataDir} 2>/dev/null

    # adding legacy support in v2.0

    if [ ! -e /data/data/$3 ] && [ -e $1/$3 ]; then
        cd $1
        mv $3 /data/data/$3
    fi

    cd /data/data/

    busybox_file=$2/busybox
    if [ ! -e ${busybox_file} ] && [ -e $1/busybox ]; then
        busybox_file=$1/busybox
    fi

    ${busybox_file} tar -xzpf $3
    rm $3

    chmod 755 ${dataDir}
    chown ${app_uid}:${app_uid} -Rf ${dataDir}

    restorecon -RF ${dataDir} 2>/dev/null
fi


