#!sbin/sh

# parameters: TEMP_DIR_NAME tar.gz_file package_name

cp $1/$2 /data/data/
cd /data/data/

dataDir=$(dumpsys package $3 | grep dataDir | head -n 1 | cut -d '=' -f2)
rm -rf ${dataDir}

$1/busybox tar -xzpf $2
rm $2

app_uid=$(dumpsys package $3 | grep userId= | cut -d '=' -f2)
chown ${app_uid}:${app_uid} -Rf ${dataDir}

chmod 755 ${dataDir}

restorecon -RF ${dataDir}

rm -rf $1/$2
