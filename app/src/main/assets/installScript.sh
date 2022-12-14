#!sbin/sh

# parameters:

MIGRATE_CACHE=$1
apk_dir_name=$2
base_apk_name=$3
package_name=$4
installer_name=$5
METADATA_HOLDER=$6
app_name=$7
API=$8

if [[ -e ${MIGRATE_CACHE}/${apk_dir_name} || -e ${MIGRATE_CACHE}/${base_apk_name} ]]; then

    full_apk_dir=""    ## support version 1.2

    if [[ -e ${MIGRATE_CACHE}/${apk_dir_name} ]]; then     ## support version 1.2
        full_apk_dir=${MIGRATE_CACHE}/${apk_dir_name}
    else
        full_apk_dir=${MIGRATE_CACHE}
    fi

    #chmod -R 777 "$MIGRATE_CACHE"
    cd ${full_apk_dir}

    # store package verification state
    #### Done from main script (version 4.0) ####
    #verification_state="$(settings get global package_verifier_enable)"

    # disable package verification if the above field exists or if it not 0 by default
    #### Done from main script (version 4.0) ####
    #if [[ -n ${verification_state} && ${verification_state} != "null" && ${verification_state} != "0" ]]; then
    #    settings put global package_verifier_enable 0
    #fi

    #install main app

    # this block for android 11
    pmc="pm install"
    if [[ $API -ge 29 ]]; then
        pmc="pm install --user 0"
    fi

    echo "Installing APK"
    if [[ -n ${installer_name} && ${installer_name} != "NULL" && -n "$(pm list packages ${installer_name})" ]]; then
        $pmc -r -d -t -i ${installer_name} ${base_apk_name}
    else
        $pmc -r -d -t ${base_apk_name}
    fi

    if [[ -z "$(pm list packages ${package_name})" ]]; then
        echo "ERROR:: App $app_name not installed. Please install manually via Google Play or elsewhere"
    else

        split_count=$(ls -1 | grep "split" | grep ".apk" | wc -l)
        if [[ ${split_count} -gt 0 ]]; then

            ############## Made by Vijay ##############

            # create install session
            echo "Split: Creating Install Session"

            # this block for android 11
            pmcs="pm install-create --user 0"
            if [[ $API -ge 29 ]]; then
                pmcs="pm install-create --user 0"
            fi

            if [[ -n ${installer_name} && ${installer_name} != "NULL" ]]; then
                session=$(${pmcs} -i ${installer_name} -p ${package_name} | cut -d'[' -f2 | cut -d']' -f1)
            else
                session=$(${pmcs} -p ${package_name} | cut -d'[' -f2 | cut -d']' -f1)
            fi

            # add split apks
            echo "Split: Adding Split Apks to Session"
            for filename in split_*.apk; do
                size="$(wc -c < ${filename})"
                pm install-write -S ${size} ${session} ${filename} ${full_apk_dir}/${filename} 2>/dev/null && echo "Split: Added ${filename}"
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
    #### Done from main script (version 4.0) ####
    #if [[ -n ${verification_state} && ${verification_state} != "null" && ${verification_state} != "0" ]]; then
    #    settings put global package_verifier_enable ${verification_state}
    #fi

    if [[ -n "$(pm list packages ${package_name})" ]]; then
        echo "Clearing apks"
        if [[ -e ${MIGRATE_CACHE}/${apk_dir_name} ]]; then     ## support version 1.2
            rm -rf "$full_apk_dir"
        else
            rm -f $base_apk_name
        fi
        echo "ok" > ${METADATA_HOLDER}/${package_name}.app.marker
    fi

else
    echo "ERROR:: Apk(s) for ${package_name} not found!"
fi