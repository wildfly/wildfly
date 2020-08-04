###
###  Creates a list of Maven artifacts to be included in Public JBoss AS 7 API aggregated JavaDoc.
###
PROGNAME=`basename $0`
DIRNAME=`dirname $0`
TARGET=$DIRNAME/target
PROJECT_ROOT_DIR="$DIRNAME/../..";
function error()  { echo    "$@" 1>&2; }
function errore() { echo -e "$@" 1>&2; }

if [ ! `which xsltproc` ]; then
  error "xsltproc not found. This script needs it. Please install it.";
  exit 2;
fi

mkdir -p $TARGET;


#####  With exported dependencies, converted from module names to groupIDs:

errore "\n\n===  Printing modules which don't have value=\"private\" and are not aliases.\n"

echo '' > $TARGET/modules.include.tmp.txt
echo '' > $TARGET/modules.exclude.tmp.txt
for i in `find $PROJECT_ROOT_DIR/build/src/main/resources/modules/ -name module.xml` ;  do

  DEST='include';

  FILE=`grep 'value="private"' --files-without-match  $i`;
  if [ "$FILE" == "" ] ; then DEST='exclude'; fi;
  FILE=`grep 'value="unsupported"' --files-without-match  $i`;
  if [ "$FILE" == "" ] ; then DEST='exclude'; fi
  error "  Module $i : $DEST";
  
  FILE=$i
  ##  Extract module name.
  if grep --quiet '<module-alias ' $FILE; then  error "     (Module alias.)"; continue; fi;
  MOD=`grep '<module .* name="' "$FILE" | head -1 | sed 's#<module .* name="\([^"]*\).*"#\1#' | sed 's#/\?>##'`;
  error "    Module name: $MOD"
  
  echo $MOD >> $TARGET/modules.$DEST.tmp.txt

  ##  Exported dependencies.
  #grep '<module name="' $FILE | grep 'export="true"' | sed 's#<module name="\([^"]*\).*"#\1#' | sed 's#/\?>##' | sed 's#\s*\(.*\)\s*#\1#' | tee --append $TARGET/packages.include.tmp.txt | sed 's#.*#        Exported dep: \0#' 1>&2
done
sort $TARGET/modules.include.tmp.txt | uniq > $TARGET/modules.include.txt
sort $TARGET/modules.exclude.tmp.txt | uniq > $TARGET/modules.exclude.txt



###  Now we have a list of public API modules, e.g. javax.management.j2ee.api
###  Let's convert it into a list of G:A's.
errore "\n\n===  Converting list of public API modules into list of groupID:artifactID.\n"

function convertModuleNameToGA() {
  inFile=$1;
  outFile=$2;
  
  echo > $outFile-unsorted;
  while read -r MODULE ; do
    error "Artifacts for module '$MODULE':"
    GROUP_IDS=`xsltproc --stringparam moduleName "$MODULE"  $DIRNAME/convertModuleNameToGA.xsl $PROJECT_ROOT_DIR/build/build.xml`
    error "$GROUP_IDS" | sed 's#.*#        \0#'
    echo "$GROUP_IDS" >> $outFile-unsorted;
  done < $inFile;
  cat $outFile-unsorted | sort | uniq > $outFile;
}

convertModuleNameToGA $TARGET/modules.include.txt $TARGET/artifacts.in.tmp.txt;
convertModuleNameToGA $TARGET/modules.exclude.txt $TARGET/artifacts.ex.tmp.txt;

###  Blacklisted ->  prepend '#'.
  echo > $TARGET/artifacts.in.tmp-blist.txt;
  while read -r ARTIFACT ; do
    if grep --quiet "$ARTIFACT\$" $DIRNAME/artifactsBlacklist.txt ; then echo -n "# " >> $TARGET/artifacts.in.tmp-blist.txt; fi
    echo "$ARTIFACT" >> $TARGET/artifacts.in.tmp-blist.txt;
  done < $TARGET/artifacts.in.tmp.txt;


###  Wrap it as includes for pom.xml, removing empty lines first.
errore "\n\n===  Wrapping list of groupID:artifactID into <include> / <exclude> tags.\n"
echo '<?xml version="1.0" ?>'
echo '<root>'
echo '<dependencySourceIncludes>'
cat $TARGET/artifacts.in.tmp-blist.txt | sed '/^$/d' | sed 's#.*#    <include>\0</include>#' | sed 's@    <.*#.*@    <!-- \0 -->@'
echo '</dependencySourceIncludes>'
echo '<dependencySourceExcludes>'
cat $TARGET/artifacts.ex.tmp.txt       | sed '/^$/d' | sed 's#.*#    <exclude>\0</exclude>#'
echo '    <!-- Blacklisted artifacts - see build/javadoc/artifactsBlacklist.txt. -->'
echo '    <!-- [ERROR] java.lang.ClassCastException: com.sun.tools.javadoc.ClassDocImpl cannot be cast to com.sun.javadoc.AnnotationTypeDoc -->'
cat $DIRNAME/artifactsBlacklist.txt   | sed '/^$/d' | sed 's#.*#    <exclude>\0</exclude>#'
echo '</dependencySourceExcludes>'
echo '</root>'






###  Not used;  this uses Xalan instead of xsltproc which might not be available.
function doXSLT(){
  AS_DIR=`ls -1d build/target/jboss-as-*`
  CP=$AS_DIR/modules/org/apache/xalan/main/xalan-2.7.1.jbossorg-2.jar
  echo java -cp $CP org.apache.xalan.xslt.Process -IN $1 -XSL $2 -OUT $3
  java -cp $CP org.apache.xalan.xslt.Process -IN $1 -XSL $2 -OUT $3
}

