#!sbin/sh

# parameters: TEMP_DIR_NAME apk_dir_name app_name.apk package_name


if [ -e $1/$2 ]; then

    #split apk support from v2.0
    full_apk_dir=$1/$2

    chmod 777 "$full_apk_dir"
    cd ${full_apk_dir}

    pm install -r -d -t ${3}

    for filename in ${full_apk_dir}/split_*.apk; do
        pm install -p $4 "$filename" 2>/dev/null && echo "Split: ${filename}"
    done

    rm -rf "$full_apk_dir"

elif [ -e $1/$3 ]; then

    # legacy support
    cd ${1}
    pm install -r -d -t ${3}
    rm -r ${3}

else
    echo "Apk ${3} not found!"
fi