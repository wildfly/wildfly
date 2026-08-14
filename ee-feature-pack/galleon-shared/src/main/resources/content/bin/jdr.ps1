#############################################################################
#                                                                          ##
#    JBoss Diagnostic Report (JDR) Script for Windows                      ##
#                                                                          ##
#############################################################################
$scripts = (Get-ChildItem $MyInvocation.MyCommand.Path).Directory.FullName;
. "$scripts\common.ps1"

# initialize JAVA_OPTS from the environment
# @(...) keeps the result an array; PowerShell unrolls the value returned by String-To-Array,
# so an empty or single valued JAVA_OPTS would otherwise be assigned as $null or a String
$JAVA_OPTS = @(String-To-Array -value $env:JAVA_OPTS)

$PROG_ARGS = Get-Java-Arguments -entryModule "org.jboss.as.jdr" -serverOpts $ARGS -logFileProperties $null

try{
	pushd $JBOSS_HOME
	& $JAVA $PROG_ARGS
}finally{
	popd
	Env-Clean-Up
}
