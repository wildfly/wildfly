#!/bin/bash
MAVEN_VERSION=$1
MAVEN_MAJOR_VERSION="$(echo $MAVEN_VERSION | cut -d '.' -f 1,2)"
MAVEN_LIST_URL='http://www.apache.org/dist/maven/maven-3/'
LWD="$(pwd)"

trap on_exit EXIT
on_exit(){
    cd "$LWD"
}

get_latest_maven_version(){
    # List available maven minor versions for download
    major_version="$1"

    # filter out the link elements on page and grab the highest number
    latest_version="$(curl -s $MAVEN_LIST_URL | grep $major_version |\
                           sed "s_</a>.*__g" | rev |\
                           cut -d '>' -f 1 | rev |\
                           sort -n | tail -n 1 |\
                           sed 's_/__g')"
    if [ $(echo $latest_version | grep -c $major_version) -ne 1 ]; then
        >&2 echo "ERROR: No download candidate for major version $major_version from $MAVEN_LIST_URL"
    elif [ "$MAVEN_VERSION" != "$latest_version" ]; then
        >&2 echo "INFO: $MAVEN_VERSION is not the latest available minor version."
    fi
    echo "$latest_version"
}

download_maven_version(){
    # Download a specific version of maven (won't redownload if sha1 matches)
    maven_version="$1"
    maven_file_str="apache-maven-$maven_version-bin.zip"

    maven_curlpath="$MAVEN_LIST_URL""$maven_version/binaries/$maven_file_str"
    sha1_curlpath="$maven_curlpath".sha1
    maven_sha1="$(curl -s $sha1_curlpath)"
    retries=0
    max_retries=3
    if [ ! -f "$maven_file_str" ]; then
        touch "$maven_file_str"
    fi
    until [ "$maven_sha1" == "$(sha1sum $maven_file_str | cut -d ' ' -f 1)" ]; do
        if [ $retries -ge $(expr $max_retries + 1) ]; then
            >&2 echo "ERROR: Unable to download $maven_curlpath after $max_retries tries."
            exit 1
        fi
        retries=$(expr $retries + 1)
        curl -sO "$maven_curlpath"
    done
    echo "$maven_file_str"
}

install_maven_version(){
    # Install maven locally
    maven_file_str="$1"
    unzipped_file_str="$(echo $maven_file_str | sed 's/-bin.zip//')"

    if [ -d maven ]; then
        rm -rf maven
    fi
    if [ -f "$maven_file_str" ]; then
        unzip -qq "$maven_file_str"
        mv "$unzipped_file_str" maven
        rm -f mvn
        ln -s maven/bin/mvn mvn
    fi
}

check_maven_version(){
    # Check what version (if any) of maven is available
    major_version="$1"
    
    PATH=$PATH:.
    mvn_exists=0
    which mvn 2>&1 > /dev/null
    if [ $? -eq 0 ]; then
        mvn_exists=1
    fi
    if [ -f mvn ]; then
        mvn_exists=1
    fi
    if [ $mvn_exists -eq 1 ]; then
        version_str="$(mvn -version | grep 'Apache Maven' | cut -d ' ' -f 3)"
        version_bool="$(echo $version_str | grep -c $major_version)"
        if [ $version_bool -gt 0 ]; then
            >&2 echo "INFO: maven version $version_str available."
            return 0
        elif [ $version_bool -eq 0 ]; then
            >&2 echo "INFO: maven version $version_str available, but not in major version $major_version."
            return 1
        fi
    fi
    return 1
}

# Do the things
cd "$(dirname $0)"
check_maven_version "$MAVEN_MAJOR_VERSION"
if [ $? -ne 0 ]; then
    maven_desired_version="$(get_latest_maven_version $MAVEN_MAJOR_VERSION)"
    if [ -z "$maven_desired_version" ]; then
        exit 1
    fi
    >&2 echo "INFO: Attempting to download maven $maven_desired_version"
    maven_desired_version_filepath="$(download_maven_version $maven_desired_version)"
    install_maven_version "$maven_desired_version_filepath"
    check_maven_version "$MAVEN_MAJOR_VERSION"
    if [ $? -ne 0 ]; then
        >&2 echo "ERROR: Unable to install maven version $MAVEN_MAJOR_VERSION"'.*'
        exit 1
    fi
else exit 0
fi
exit 0
