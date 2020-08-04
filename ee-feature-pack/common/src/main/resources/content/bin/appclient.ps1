#############################################################################
#                                                                          ##
#    AppClient Startup Script                                              ##
#                                                                          ##
#############################################################################

$scripts = (Get-ChildItem $MyInvocation.MyCommand.Path).Directory.FullName;
. $scripts'\common.ps1'
$SERVER_OPTS = Process-Script-Parameters -Params $ARGS

# Read an optional running configuration file
$APPCLIENT_CONF_FILE = $scripts + '.\appclient.conf.ps1'
$APPCLIENT_CONF_FILE = Get-Env RUN_CONF $APPCLIENT_CONF_FILE
. $APPCLIENT_CONF_FILE

if ($global:SECMGR) {
    $MODULE_OPTS +="-secmgr";
}

$dir = Get-Env JBOSS_BASE_DIR $JBOSS_HOME\appclient
Set-Global-Variables -baseDir $dir

$PROG_ARGS = Get-Java-Arguments -entryModule "org.jboss.as.appclient" -serverOpts $SERVER_OPTS -logFileProperties "$JBOSS_HOME\appclient\configuration\logging.properties" -logFile "$JBOSS_HOME\appclient\log\appclient.log"

Start-WildFly-Process -programArguments $PROG_ARGS