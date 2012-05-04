
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

mkdir $TARGET;


###
###  Also print out the groups of packages from artifacts grouped by module; see build.xml for "groups definition".
###

###  Get the groups of artifacts in format:
###  MODULE:  org.foo.bar
###      ARTIFACT:  org.foo:bar 
xsltproc $DIRNAME/printModulesInPlainText.xsl $PROJECT_ROOT_DIR/build/build.xml > packagesGroups.tmp.txt

echo "<groups>"

  while read -r LINE ; do

echo $LINE;
    ##  If it's a module name, create the <group>.
    if [[ $LINE == MODULE:* ]] ; then
      if [ "" != "$MOD_NAME" ] ; then 
        #echo "    <packages>$PACKAGES</packages>"
        echo "  </group>";
      fi;
      MOD_NAME=${LINE#MODULE: }
      MOD_PATH=`echo $MOD_NAME | tr . /`
      echo "  <group>"
      echo "    <title>Module $MOD_NAME</title>"
      PACKAGES="";
      for JAR in `find $PROJECT_ROOT_DIR/build/target/jboss-as-7.1.2.Final-SNAPSHOT/modules/$MOD_PATH/main -name *.jar`; do
        #echo "    JAR: $JAR";
        for PACKAGE in `jar tf $JAR | grep .class | sort | uniq`; do
          PACKAGE=`dirname $PACKAGE | tr / .`
          PACKAGES="$PACKAGES:$PACKAGE";
        done;
      done;
      echo "    <packages>$PACKAGES</packages>";
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


