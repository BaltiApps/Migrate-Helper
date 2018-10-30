#!sbin/sh

# parameters: TEMP_DIR_NAME app_name.apk

apkPath=$1/$2

chmod 777 ${apkPath}
pm install -r -d ${apkPath}
rm -rf ${apkPath}
