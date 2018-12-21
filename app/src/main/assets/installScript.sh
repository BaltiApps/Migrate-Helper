#!sbin/sh

# parameters:

# TEMP_DIR_NAME_OLD
# TEMP_DIR_NAME_NEW
# apk_dir_name
# app_name.apk
# package_name


if [ -e $2/$3 ]; then

    #split apk support from v2.0
    full_apk_dir=$2/$3

    chmod 777 "$full_apk_dir"
    cd ${full_apk_dir}

    pm install -r -d -t ${4}

    for filename in ${full_apk_dir}/split_*.apk; do
        pm install -p $5 "$filename" 2>/dev/null && echo "Split: ${filename}"
    done

    rm -rf "$full_apk_dir"

elif [ -e $1/$4 ]; then

    # legacy support
    cd ${1}
    pm install -r -d -t ${4}
    rm -r ${4}

else
    echo "Apk ${4} not found!"
fi