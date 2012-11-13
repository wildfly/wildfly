pushd `dirname $0`
CURDIR=`pwd`
popd

export TCKCHECKOUT=$CURDIR/r4v42tck
export JBOSS_AS_BUILD_NAME=`ls $CURDIR/../../build/target | grep jboss-as`
export JBOSS_AS_LOCATION=$CURDIR/../../build/target/$JBOSS_AS_BUILD_NAME

export JBOSS_AS_EMBEDDED=`find $JBOSS_AS_LOCATION | grep "\/jboss-as-embedded.*[.]jar$"`
export JBOSS_MODULES=`find $JBOSS_AS_LOCATION | grep "\/jboss-modules.*[.]jar$"`
export JBOSS_LOGGING=`find $JBOSS_AS_LOCATION | grep "\/jboss-logging.*[.]jar$"`
export JBOSS_AS_CONTROLLER_CLIENT=`find $JBOSS_AS_LOCATION | grep "\/jboss-as-controller-client.*[.]jar$"`
export JBOSS_LOGMANAGER=`find $JBOSS_AS_LOCATION | grep "\/jboss-logmanager.*[.]jar$"`
export JBOSS_DMR=`find $JBOSS_AS_LOCATION | grep "\/jboss-dmr.*[.]jar$"`
export JBOSS_MSC=`find $JBOSS_AS_LOCATION | grep "\/jboss-msc.*[.]jar$"`
export JBOSS_OSGI_CORE=`find $JBOSS_AS_LOCATION | grep "\/org[.]osgi[.]core.*[.]jar$"`
export JBOSS_OSGI_LAUNCHER=`find $CURDIR/../launcher/target | grep "\/jboss-as-osgi-launcher.*[.]jar$" | grep -v sources`

echo TCK Checkout directory: $TCKCHECKOUT
echo JBOSS_AS_LOCATION: $JBOSS_AS_LOCATION


# Clone the TCK repo 
if [ ! -d $TCKCHECKOUT ]; then
  svn co https://svn.devel.redhat.com/repos/jboss-tck/osgitck/r4v42 $TCKCHECKOUT
  #git clone /home/hudson/static_build_env/osgi-tck-4.2/r4v42 $TCKCHECKOUT
fi

# Switch to known tck-checkout tag 
# cd $TCKCHECKOUT; git checkout r4v42-core-cmpn-final

# Setup the TCK
ant setup.vi

# Run the core tests
ant run-core-tests
      
# Run the package admin tests
#ant run-packageadmin-tests

# Run the HTTP Service tests
#ant run-httpservice-tests

