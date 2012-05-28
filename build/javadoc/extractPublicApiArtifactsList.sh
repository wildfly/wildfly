
###
###  Creates a list of Maven artifacts to be included in Public JBoss AS 7 API aggregated JavaDoc.
###


PROGNAME=`basename $0`
DIRNAME=`dirname $0`

TARGET=$DIRNAME/target
PROJECT_ROOT_DIR="$DIRNAME/../..";

if [ ! `which xsltproc` ]; then
  echo "xsltproc not found. This script needs it. Please install it.";
  exit 2;
fi

mkdir -p $TARGET;


#####  With exported dependencies, converted from module names to groupIDs:

echo -e "\n\n===  Printing modules which don't have value=\"private\" and are not aliases.\n"

echo '' > $TARGET/packages.tmp.txt
for i in `find $PROJECT_ROOT_DIR/build/src/main/resources/modules/ -name module.xml` ;  do
  FILE=`grep 'value="private"' --files-without-match  $i`;
  if [ "$FILE" == "" ] ; then continue; fi;
  echo "  Public module: $i"

  ##  Extract module name.
  PKG=`grep '<module .* name="' $FILE | head -1 | sed 's#<module .* name="\([^"]*\).*"#\1#' | sed 's#/\?>##'`;
  if grep --quiet '<module-alias ' $FILE; then  echo "     (Module alias.)"; continue; fi;
  echo "    Module name: $PKG"
  echo $PKG >> $TARGET/packages.tmp.txt

  ##  Exported dependencies.
  grep '<module name="' $FILE | grep 'export="true"' | sed 's#<module name="\([^"]*\).*"#\1#' | sed 's#/\?>##' | sed 's#\s*\(.*\)\s*#\1#' | tee --append $TARGET/packages.tmp.txt | sed 's#.*#        Exported dep: \0#'
done
sort $TARGET/packages.tmp.txt | uniq > $TARGET/modules.tmp2.txt



###  Now we have a list of public API modules, e.g. javax.management.j2ee.api
###  Let's convert it into a list of groupIDs.
echo -e "\n\n===  Converting list of public API modules into list of groupID:artifactID.\n"

echo > $TARGET/groupIDs.tmp.txt
while read -r MODULE ; do
  echo "Artefacts for module $MODULE :"
  GROUP_ID=`xsltproc --stringparam moduleName "$MODULE"  $DIRNAME/convertModuleNameToGroupID.xsl $PROJECT_ROOT_DIR/build/build.xml`
  echo "  GroupID:ArtifactID = $GROUP_ID"
  echo $GROUP_ID >> $TARGET/groupIDs.tmp.txt
done < $TARGET/modules.tmp2.txt
cat $TARGET/groupIDs.tmp.txt | sort | uniq > $TARGET/groupIDs.tmp-sorted.txt

###  Wrap it as includes for pom.xml.
echo -e "\n\n===  Wrapping list of groupID:artifactID into <include> tags.\n"
cat $TARGET/groupIDs.tmp-sorted.txt | sed 's#.*#<include>\0</include>#'



###  This was intended to list where certain package comes from, but it would need extracting versions. Disabled now.
FILTER="$1";
M2_REPO="$2";

if [ "DISABLED" == "" -a "$M2_REPO" != "" ] ; then
  echo -e "\n\n===  Listing packages contained in public artifacts.\n"
  if [ "$FILTER" == "" ] ; then FILTER="cat" ;
  else FILTER="grep $FILTER"; fi

  for JAR_PATH in `cat $TARGET/groupIDs.tmp-sorted.txt | tr .: / `; do
    echo "=== Contents of $M2_REPO/$JAR_PATH :" 
    jar -tf $M2_REPO/$JAR_PATH | $FILTER | sed 's#.*#        \0#'
  done
fi
###  Instead, one can do:
# for i in `find ../target/jboss-as-7.2.0.Alpha1-SNAPSHOT/modules/ -name *.jar`; do echo "====== $i"; jar -tf $i | grep $FILTER; done



###  Not used;  this uses Xalan instead of xsltproc which might not be available.
function doXSLT(){
  AS_DIR=`ls -1d build/target/jboss-as-*`
  CP=$AS_DIR/modules/org/apache/xalan/main/xalan-2.7.1.jbossorg-1.jar
  echo java -cp $CP org.apache.xalan.xslt.Process -IN $1 -XSL $2 -OUT $3
  java -cp $CP org.apache.xalan.xslt.Process -IN $1 -XSL $2 -OUT $3
}

