#!sbin/sh

# parameters: TEMP_DIR_NAME tar.gz_file dir_name app_uid
cp $1/$2 /data/data/
cd /data/data/
rm -rf $3
$1/busybox tar -xzpf $2
rm $2
chmod 755 $3
chown $4:$4 -Rf $3
restorecon -RF $3
rm -rf $1/$2
