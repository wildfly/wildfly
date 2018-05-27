#############################################################################
#                                                                          ##
#    AppClient Startup Script                                              ##
#                                                                          ##
#############################################################################

$scripts = (Get-ChildItem $MyInvocation.MyCommand.Path).Directory.FullName;
. $scripts'\common.ps1'

$JAVA_OPTS = Get-Java-Opts
Process-Java-Opts-Parameters -Params $JAVA_OPTS
$SERVER_OPTS = Process-Script-Parameters -Params $ARGS

# Read an optional running configuration file
$APPCLIENT_CONF_FILE = $scripts + '.\appclient.conf.ps1'
$APPCLIENT_CONF_FILE = Get-Env RUN_CONF $APPCLIENT_CONF_FILE
. $APPCLIENT_CONF_FILE

if ($global:SECMGR -eq 'true') {
    $MODULE_OPTS +="-secmgr";
}

$dir = Get-Env JBOSS_BASE_DIR $JBOSS_HOME\appclient
Set-Global-Variables -baseDir $dir

$PROG_ARGS = Jbossws-Get-Java-Arguments -scriptName "appclient.ps1"  -entryModule "org.jboss.as.appclient"  -javaOpts $JAVA_OPTS -serverOpts $SERVER_OPTS -logFileProperties "$JBOSS_HOME\appclient\configuration\logging.properties" -logFile "$JBOSS_HOME\appclient\log\appclient.log"

Start-WildFly-Process -programArguments $PROG_ARGS

Env-Clean-Up
