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

This script aids in releasing the BOMs 

OPTIONS:
   -u      Updates version numbers in all POMs, used with -o and -n      
   -o      Old version number to update from
   -n      New version number to update to
   -m      Generate html versions of markdown readmes
   -h      Shows this message
EOF
}

update()
{
cd $DIR
echo "Updating versions from $OLDVERSION TO $NEWVERSION for all Java and XML files under $PWD"
perl -pi -e "s/${OLDVERSION}/${NEWVERSION}/g" `find . -name \*.xml -or -name \*.java`
}

markdown_to_html()
{
   cd $DIR
   readmes=`find . -iname readme.md -or -iname contributing.md`
   echo $readmes
   for readme in $readmes
   do
      output_filename=${readme//.md/.html}
      output_filename=${output_filename//.MD/.html}
      markdown $readme -f $output_filename  
   done
}

OLDVERSION="1.0.0-SNAPSHOT"
NEWVERSION="1.0.0-SNAPSHOT"
VERSION="1.0.0-SNAPSHOT"
CMD="usage"

while getopts “muo:n:r:” OPTION

do
     case $OPTION in
         u)
             CMD="update"
             ;;
         h)
             usage
             exit
             ;;
         o)
             OLDVERSION=$OPTARG
             ;;
         n)
             NEWVERSION=$OPTARG
             ;;
         m)
             CMD="markdown_to_html"
             ;;
         [?])
             usage
             exit
             ;;
     esac
done

$CMD

