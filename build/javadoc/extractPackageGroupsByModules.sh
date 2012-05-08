
###  
###  Creates a groups definition of Java packages for Public JBoss AS 7 API aggregated JavaDoc.
###  


PROGNAME=`basename $0`
DIRNAME=`dirname $0`

TARGET=$DIRNAME/target
PROJECT_ROOT_DIR="$DIRNAME/../..";

if [ ! `which xsltproc` ]; then 
  echo "xsltproc not found. This script needs it. Please install it.";
  exit 2;
fi

M2_REPO=~/.m2/repository
#for i in `find $PROJECT_ROOT_DIR/build/src/main/resources/modules/ -name module.xml` ;  do

mkdir -p $TARGET;
rm $TARGET/listedPackages.tmp.txt

###
###  Also print out the groups of packages from artifacts grouped by module; see build.xml for "groups definition".
###

###  Get the groups of artifacts in format:
###  MODULE:  org.foo.bar
###      ARTIFACT:  org.foo:bar 
xsltproc $DIRNAME/printModulesInPlainText.xsl $PROJECT_ROOT_DIR/build/build.xml > packagesGroups.tmp.txt

echo "<groups>"

  while read -r LINE ; do

    #echo $LINE;
    ##  If it's a module name, create the <group>.
    if [[ $LINE == MODULE:* ]] ; then
      if [ "" != "$MOD_NAME" ] ; then     ##  This is here for the case we wanted to grab packages from the list of artifacts from packagesGroups.tmp.txt.
        #echo "    <packages>$PACKAGES</packages>"
        echo "  </group>";
        MOD_NAME="";
      fi;
      MOD_NAME=${LINE#MODULE: }
      MOD_PATH=`echo $MOD_NAME | tr . /`
      PACKAGES="";
      for JAR in `find $PROJECT_ROOT_DIR/build/target/jboss-as-7.1.2.Final-SNAPSHOT/modules/$MOD_PATH/main -name *.jar`; do
        #echo "    JAR: $JAR";
        for PACKAGE in `jar tf $JAR | grep .class | sed 's#/[^/]*\.class##' | sort | uniq`; do
          #PACKAGE=`dirname $PACKAGE | tr / .`
          PACKAGE=`echo $PACKAGE | tr / .`
          ##  Check whether the package is listed in some previous module.
          #if grep --line-regexp "$PACKAGE" $TARGET/listedPackages.tmp.txt  ; then  #> /dev/null
          if grep --line-regexp "$PACKAGE in $MOD_NAME" $TARGET/listedPackages.tmp.txt  ; then  #> /dev/null
            echo "[WARN]  Package $PACKAGE was already used!" >&2;
            #read -p "Press enter."
            #read -n1 -r -p "Press any key to continue..." key <&0
          fi
          echo "$PACKAGE in $MOD_NAME" >> $TARGET/listedPackages.tmp.txt
          PACKAGES="$PACKAGES:$PACKAGE";
        done;
      done;
      if [ "" == "$PACKAGES" ] ; then continue; fi;
      echo "  <group>"
      echo "    <title>Module $MOD_NAME</title>"
      echo "    <packages>${PACKAGES#:}</packages>";  ## Remove first colon.
      continue;
    fi;

    ##  Otherwise, it's an groupId:artifactId. Get it, unzip it, get a list of packages.
    #ART_PATH=$LINE
    #	PACKAGES="$PACKAGES:$LINE"
  done < packagesGroups.tmp.txt

echo "</groups>"

#    <!-- To includes java.lang, java.lang.ref, java.lang.reflect and only java.util (i.e. not java.util.jar) -->
#    <packages>java.lang*:java.util</packages>
#     <!-- To include javax.accessibility, javax.crypto, ... (among others) -->
#    <packages>javax.*</packages>


