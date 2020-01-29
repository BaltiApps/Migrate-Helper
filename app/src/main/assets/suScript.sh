#!sbin/sh

SELF_PACKAGE_NAME="$1"
METADATA_HOLDER="$2/"
CACHE_DIR="$3"
SETTINGS_FILE_NAME="$4"
WIFI_FILE_NAME="$5"

# display PID
echo "--- PID: $$"

echo " "

# getting elevated permissions
pm grant ${SELF_PACKAGE_NAME} android.permission.PACKAGE_USAGE_STATS
#pm grant ${SELF_PACKAGE_NAME} android.permission.WRITE_SECURE_SETTINGS

# make METADATA_HOLDER if not present
mkdir -p ${METADATA_HOLDER}

# removing contents of METADATA_HOLDER
if [[ -n ${METADATA_HOLDER} ]]; then
    rm -rf ${METADATA_HOLDER}/* 2>/dev/null
fi

# copy files
cp -f ${CACHE_DIR}/*.json ${METADATA_HOLDER} 2>/dev/null
cp -f ${CACHE_DIR}/*.icon ${METADATA_HOLDER} 2>/dev/null
cp -f ${CACHE_DIR}/*.vcf ${METADATA_HOLDER} 2>/dev/null
cp -f ${CACHE_DIR}/*.sms.db ${METADATA_HOLDER} 2>/dev/null
cp -f ${CACHE_DIR}/*.calls.db ${METADATA_HOLDER} 2>/dev/null
cp -f ${CACHE_DIR}/*.perm ${METADATA_HOLDER} 2>/dev/null
cp -f ${CACHE_DIR}/${SETTINGS_FILE_NAME} ${METADATA_HOLDER} 2>/dev/null
cp -f ${CACHE_DIR}/${WIFI_FILE_NAME} ${METADATA_HOLDER} 2>/dev/null

echo --- END_OF_COPY ---
