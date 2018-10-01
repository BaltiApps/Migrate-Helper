#!sbin/sh

# parameters: TEMP_DIR_NAME tar.gz_file package_name app_uid
cp $1/$2 /data/data/
cd /data/data/
rm -rf $3
$1/busybox tar -xzpf $2
rm $2
chmod 755 $3
app_uid=$(dumpsys package $3 | grep userId= | cut -d '=' -f2)
chown ${app_uid}:${app_uid} -Rf $3
restorecon -RF $3
rm -rf $1/$2
