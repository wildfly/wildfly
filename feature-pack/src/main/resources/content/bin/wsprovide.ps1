##################################################################
#                                                               ##
#    wsprovide tool script for Windows                          ##
#                                                               ##
##################################################################
$scripts = (Get-ChildItem $MyInvocation.MyCommand.Path).Directory.FullName;
. $scripts'\common.ps1'

$JAVA_OPTS = Get-Java-Opts
Process-Java-Opts-Parameters -Params $JAVA_OPTS
$SERVER_OPTS = Process-Script-Parameters -Params $ARGS

if ($global:SECMGR -eq 'true') {
    $global:MODULE_OPTS = '-secmgr'
}

# Sample JPDA settings for remote socket debugging
#$JAVA_OPTS+="-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=y"

$PROG_ARGS = Jbossws-Get-Java-Arguments -scriptName "wsprovide.ps1"  -entryModule "org.jboss.ws.tools.wsprovide"  -javaOpts $JAVA_OPTS -logFileProperties $null -serverOpts $SERVER_OPTS

Start-WildFly-Process -programArguments $PROG_ARGS

Env-Clean-Up