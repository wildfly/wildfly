#!/bin/sh

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

# DEFINE


# SCRIPT

usage()
{
cat << EOF
usage: $0 options

This script performs a release of the BOMs 

OPTIONS:
   -s      Snapshot version number to update from
   -n      New snapshot version number to update to, if undefined, defaults to the version number updated from
   -r      Release version number
EOF
}

release()
{
   echo "Releasing JBoss BOMs version $RELEASEVERSION"
   $DIR/release-utils.sh -u -o $SNAPSHOTVERSION -n $RELEASEVERSION
   git commit -a -m "Prepare for $RELEASEVERSION release"
   git tag -a $RELEASEVERSION -m "Tag $RELEASEVERSION"
   $DIR/release-utils.sh -r
   $DIR/release-utils.sh -u -o $RELEASEVERSION -n $NEWSNAPSHOTVERSION
   git commit -a -m "Prepare for development of $NEWSNAPSHOTVERSION"
   echo "***** Now close and release the staged repository at http://repository.jboss.org/nexus"
}

SNAPSHOTVERSION="1.0.0-SNAPSHOT"
RELEASEVERSION="1.0.0.M1"
NEWSNAPSHOTVERSION="UNDEFINED"

while getopts “n:r:s:” OPTION

do
     case $OPTION in
         h)
             usage
             exit
             ;;
         n)
             SNAPSHOTVERSION=$OPTARG
             ;;
         r)
             RELEASEVERSION=$OPTARG
             ;;
         s)
             NEWSNAPSHOTVERSION=$OPTARG
             ;;
         [?])
             usage
             exit
             ;;
     esac
done

if [ $NEWSNAPSHOTVERSION -eq "UNDEFINED" ]
then
   NEWSNAPSHOTVERSION=$SNAPSHOTVERSION
fi

release
