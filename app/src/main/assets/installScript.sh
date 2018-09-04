#!sbin/sh

# parameters: TEMP_DIR_NAME app_name.apk

cd $1
pm install -r $2
rm -rf $2

