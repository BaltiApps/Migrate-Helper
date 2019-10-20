#!sbin/sh

# parameters:

TEMP_DIR_NAME=$1
apk_dir_name=$2
base_apk_name=$3
package_name=$4
installer_name=$5

if [[ -e ${TEMP_DIR_NAME}/${apk_dir_name} ]]; then

    full_apk_dir=${TEMP_DIR_NAME}/${apk_dir_name}

    chmod 777 "$full_apk_dir"
    cd ${full_apk_dir}

    # store package verification state
    verification_state="$(settings get global package_verifier_enable)"

    # disable package verification if the above field exists or if it not 0 by default
    if [[ ${verification_state} != "null" && ${verification_state} !="0" ]]; then
        settings put global package_verifier_enable 0
    fi

    #install main app
    if [[ -n ${installer_name} && ${installer_name} != "NULL" ]]; then
        pm install -r -d -t -i ${installer_name} ${base_apk_name}
    else
        pm install -r -d -t ${base_apk_name}
    fi

    if [[ -z "$(pm list packages ${package_name})" ]]; then
        echo "App not installed. Please install manually via Google Play or elsewhere"
    else

        split_count=$(ls -1 | grep "split" | grep ".apk" | wc -l)
        if [[ ${split_count} -gt 0 ]]; then

            ############## Made by Vijay ##############

            # create install session
            echo "Split: Creating Install Session"
            if [[ -n ${installer_name} && ${installer_name} != "NULL" ]]; then
                session=$(pm install-create -i ${installer_name} -p ${package_name} | cut -d'[' -f2 | cut -d']' -f1)
            else
                session=$(pm install-create -p ${package_name} | cut -d'[' -f2 | cut -d']' -f1)
            fi

            # add split apks
            echo "Split: Adding Split Apks to Session"
            for filename in ${full_apk_dir}/split_*.apk; do
                pm install-write ${session} ${filename} ${full_apk_dir}/${filename} 2>/dev/null && echo "Split: Added ${filename}"
            done

            echo "Split: Installing Split Session (Be patient, it may take longer)"
            pm install-commit ${session}
            echo "Split: Done"

            ############## Made by Vijay ##############

        else
            echo "No split apks. Split count: $split_count"
        fi
    fi

    # restore package verification state
    if [[ ${verification_state} != "null" && ${verification_state} != "0" ]]; then
        settings put global package_verifier_enable ${verification_state}
    fi

    rm -rf "$full_apk_dir"

else
    echo "Apk(s) for ${package_name} not found!"
fi