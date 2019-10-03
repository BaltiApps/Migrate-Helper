#!sbin/sh

# parameters:

TEMP_DIR_NAME=$1
tar_gz_file=$2
package_name=$3

dataDir=$(dumpsys package ${package_name} | grep dataDir | head -n 1 | cut -d '=' -f2)
app_uid=$(dumpsys package ${package_name} | grep userId= | head -n 1 | cut -d '=' -f2 | cut -d ' ' -f1)

# sometimes app_uid has two lines. example: Google maps (com.google.android.apps.maps)
# so "head -n 1" takes only the first line.
# in android lollipop unnecessary extras along with app_uid. So take only first item.

if [[ -z "$app_uid" && -n "$(pm list packages ${package_name})" ]]; then
    echo "Failed to find package $package_name. Cannot restore data."
else

    rm -rf ${dataDir} 2>/dev/null

    # adding legacy support in v2.0

    if [[ -e /data/data/${tar_gz_file} ]]; then

        cd /data/data/
        busybox_file=${TEMP_DIR_NAME}/busybox

        if [[ -e ${busybox_file} ]]; then
            ${busybox_file} tar -xzpf ${tar_gz_file}
        else
            echo "${tar_gz_file} not found under /data/data/"
        fi

        rm ${tar_gz_file}

        if [[ -e ${dataDir} ]]; then
            chmod 755 ${dataDir}
            chown ${app_uid}:${app_uid} -Rf ${dataDir}
            restorecon -RF ${dataDir} 2>/dev/null
        else
            echo "Data dir $dataDir for package $package_name not found!"
            echo "Data file was: $tar_gz_file"
        fi

    else
        echo "Data file $tar_gz_file was not found!"
    fi

fi


