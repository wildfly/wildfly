##################################################################
#                                                               ##
#    wsprovide tool script for Windows                          ##
#                                                               ##
##################################################################
$scripts = (Get-ChildItem $MyInvocation.MyCommand.Path).Directory.FullName;
. "$scripts\common.ps1"
$SERVER_OPTS = Process-Script-Parameters -Params $ARGS

# initialize JAVA_OPTS from the environment
# @(...) keeps the result an array; PowerShell unrolls the value returned by String-To-Array,
# so an empty or single valued JAVA_OPTS would otherwise be assigned as $null or a String
$JAVA_OPTS = @(String-To-Array -value $env:JAVA_OPTS)
$JAVA_OPTS = @(Process-Java-Opts-Parameters -Params $JAVA_OPTS)

if (Get-Env-Boolean SECMGR $false) {
    Write-Warning "ERROR: The SECMGR option has been removed. The Security Manager is no longer supported."
    exit 1
}

# Sample JPDA settings for remote socket debugging
#$JAVA_OPTS+="-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=y"

$JAVA_OPTS+="-Dprogram.name=wsprovide.ps1"

$PROG_ARGS = Get-Java-Arguments -entryModule "org.jboss.ws.tools.wsprovide" -logFileProperties $null -serverOpts $SERVER_OPTS
& $JAVA $PROG_ARGS

Env-Clean-Up