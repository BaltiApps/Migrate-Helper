#!sbin/sh

# parameters:

busybox_file=$1
tar_gz_file=$2
package_name=$3
notification_fix=$4

dataDir=$(dumpsys package ${package_name} | grep dataDir | head -n 1 | cut -d '=' -f2)
app_uid=$(dumpsys package ${package_name} | grep userId= | head -n 1 | cut -d '=' -f2 | cut -d ' ' -f1)

# sometimes app_uid has two lines. example: Google maps (com.google.android.apps.maps)
# so "head -n 1" takes only the first line.
# in android lollipop unnecessary extras along with app_uid. So take only first item.

if [[ -z "$app_uid" && -n "$(pm list packages ${package_name})" ]]; then
    echo "Failed to find package $package_name. Cannot restore data."
else

    rm -rf ${dataDir} 2>/dev/null

    if [[ -e /data/data/${tar_gz_file} ]]; then

        cd /data/data/

        if [[ -e ${busybox_file} ]]; then
            tarCmd="${busybox_file} tar"
        elif [[ -n "$(command -v tar)" ]]; then
            tarCmd="tar"
        else
            tarCmd=""
        fi

        if [[ -n ${tarCmd} ]]; then
            ${tarCmd} -xzpf ${tar_gz_file} && rm ${tar_gz_file}
        else
            echo "busybox not found under ${busybox_file}. Tar not installed."
        fi

        if [[ -e ${dataDir} ]]; then
            chmod 755 ${dataDir}
            chown ${app_uid}:${app_uid} -Rf ${dataDir}
            restorecon -RF ${dataDir} 2>/dev/null

            # notification fix added in v3.0
            if [[ "${notification_fix}" == "true" ]]; then
                echo "Removing gms file under $package_name/shared_prefs"
                cd ${package_name}
                rm -f shared_prefs/com.google.android.gms.appid.xml
            fi

        else
            echo "Data dir $dataDir for package $package_name not found!"
            echo "Data file was: $tar_gz_file"
        fi

    else
        echo "Data file $tar_gz_file was not found!"
    fi

fi


