
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

mkdir $TARGET;


#####  With exported dependencies, converted from module names to groupIDs:

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
  #grep '<module name="' $FILE | grep 'export="true"' | sed 's#<module name="\([^"]*\).*"#\1#' | sed 's#/\?>##' | sed 's#\s*\(.*\)\s*#\1#' | sed 's#.*#    Exported dep: \0#'
  #grep '<module name="' $FILE | grep 'export="true"' | sed 's#<module name="\([^"]*\).*"#\1#' | sed 's#/\?>##' | sed 's#\s*\(.*\)\s*#\1#' >> $TARGET/packages.tmp.txt
  grep '<module name="' $FILE | grep 'export="true"' | sed 's#<module name="\([^"]*\).*"#\1#' | sed 's#/\?>##' | sed 's#\s*\(.*\)\s*#\1#' | tee --append $TARGET/packages.tmp.txt | sed 's#.*#        Exported dep: \0#'
done
sort $TARGET/packages.tmp.txt | uniq > $TARGET/modules.tmp2.txt
#cat $TARGET/packages.tmp2.txt | sed 's#.*#<include>\0:*</include>#'



###  Now we have a list of public API modules, e.g. javax.management.j2ee.api
###  Let's convert it into a list of groupIDs.
echo > $TARGET/groupIDs.tmp.txt
while read -r MODULE ; do
  echo "Artefacts for module: $MODULE"
  GROUP_ID=`xsltproc --stringparam moduleName "$MODULE"  $DIRNAME/convertModuleNameToGroupID.xsl $PROJECT_ROOT_DIR/build/build.xml`
  echo "  GroupID: $GROUP_ID"
  echo $GROUP_ID >> $TARGET/groupIDs.tmp.txt
done < $TARGET/modules.tmp2.txt
cat $TARGET/groupIDs.tmp.txt | sort | uniq > $TARGET/groupIDs.tmp-sorted.txt

###  Wrap it as includes for pom.xml.
cat $TARGET/groupIDs.tmp-sorted.txt | sed 's#.*#<include>\0</include>#'



###
###  Also print out the groups of packages from artifacts grouped by module; see build.xml for "groups definition".
###

###  Get the groups of artifacts in format:
###  MODULE:  org.foo.bar
###      ARTIFACT:  org.foo:bar
xsltproc $DIRNAME/printModulesInPlainText.xsl $PROJECT_ROOT_DIR/build/build.xml




###  Not used;  this uses Xalan instead of xsltproc which might not be available.
function doXSLT(){
  AS_DIR=`ls -1d build/target/jboss-as-*`
  CP=$AS_DIR/modules/org/apache/xalan/main/xalan-2.7.1.jbossorg-1.jar
  echo java -cp $CP org.apache.xalan.xslt.Process -IN $1 -XSL $2 -OUT $3
  java -cp $CP org.apache.xalan.xslt.Process -IN $1 -XSL $2 -OUT $3
}

