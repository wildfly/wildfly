
###
###  Creates a groups definition of Java packages for Public JBoss AS 7 API aggregated JavaDoc.
###


PROGNAME=`basename $0`
DIRNAME=`dirname $0`

TARGET=$DIRNAME/target
PROJECT_ROOT_DIR="$DIRNAME/../..";

AS_BUILT_DIR=`ls -1 -d $PROJECT_ROOT_DIR/build/target/jboss-as-* | tail -1`  # Latest built AS in target/

if [ ! `which xsltproc` ]; then
  echo "xsltproc not found. This script needs it. Please install it.";
  exit 2;
fi

M2_REPO=~/.m2/repository

mkdir -p $TARGET;
rm $TARGET/listedPackages.tmp.txt
touch $TARGET/listedPackages.tmp.txt

###
###  Print out the groups of packages from artifacts grouped by module; see build.xml for "groups definition".
###

###  Get the list of modules and their artifacts in this format:
###  MODULE:  org.foo.bar
###  org.foo:bar
###  org.foo:baz
###  ...
xsltproc $DIRNAME/printModulesInPlainText.xsl $PROJECT_ROOT_DIR/build/build.xml > $TARGET/modulesList.tmp.txt

MODULES_REPO='modules'                    ## AS 7.1
MODULES_REPO='modules/system/layers/base' ## AS 7.2

echo "<groups>"

  ##  For each module in build.xml...
  while read -r LINE ; do

    #echo $LINE;
    ##  If it's a module name, create the <group>.
    if [[ $LINE == MODULE:* ]] ; then
      MOD_NAME=${LINE#MODULE: }
      MOD_PATH=`echo $MOD_NAME | tr . /`
      PACKAGES="";

      for JAR in `find $AS_BUILT_DIR/$MODULES_REPO/$MOD_PATH/main -name *.jar`; do
        #echo "    JAR: $JAR";
        for PACKAGE in `jar tf $JAR | grep .class | sed 's#/[^/]*\.class##' | sort | uniq`; do
          #PACKAGE=`dirname $PACKAGE | tr / .`
          PACKAGE=`echo $PACKAGE | tr / .`
          ##  Check whether the package is listed in some previous module.
          #if grep --line-regexp "$PACKAGE" $TARGET/listedPackages.tmp.txt  ; then  #> /dev/null
          if grep --line-regexp "$PACKAGE in .*" $TARGET/listedPackages.tmp.txt  -q; then  #> /dev/null
            echo "[WARN]  Package $PACKAGE was already used!" >&2;
            grep --line-regexp "$PACKAGE in .*" $TARGET/listedPackages.tmp.txt  >&2;
            #read -p "Press enter."
            #read -n1 -r -p "Press any key to continue..." key <&0
          else
            PACKAGES="$PACKAGES:$PACKAGE";
          fi
          echo "$PACKAGE in $MOD_NAME" >> $TARGET/listedPackages.tmp.txt
        done;
      done;
      if [ "" == "$PACKAGES" ] ; then continue; fi;
      echo "  <group>"
      echo "    <title>Module $MOD_NAME</title>"
      echo "    <packages>${PACKAGES#:}</packages>";  ## Remove first colon.
      echo "  </group>";
      continue;
    fi;

    ##  Otherwise, it's a groupId:artifactId. Get it, unzip it, get a list of packages.
    #ART_PATH=$LINE
    #	PACKAGES="$PACKAGES:$LINE"
  done < $TARGET/modulesList.tmp.txt

echo "</groups>"

#    <!-- Includes java.lang, java.lang.ref, java.lang.reflect and only java.util (i.e. not java.util.jar) -->
#    <packages>java.lang*:java.util</packages>
#    <!-- Includes javax.accessibility, javax.crypto, ... (among others) -->
#    <packages>javax.*</packages>


