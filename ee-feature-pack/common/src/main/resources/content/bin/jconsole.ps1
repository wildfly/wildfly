##################################################################
#                                                               ##
#    JConsole script for Windows                                ##
#                                                               ##
##################################################################
$scripts = (Get-ChildItem $MyInvocation.MyCommand.Path).Directory.FullName;
. $scripts'\common.ps1'

if (!$JAVA_HOME){
  Write-Warning JAVA_HOME is not set. Unable to locate the jars needed to run jconsole.
  exit
}

$CLASSPATH= "$JAVA_HOME\lib\jconsole.jar;$JAVA_HOME\lib\tools.jar;$JBOSS_HOME\bin\client\jboss-cli-client.jar"

& $JAVA_HOME\bin\java.exe --add-modules=java.se -version 2> $nul

if ($LASTEXITCODE -eq 0) {
 $PROG_ARGS= @("-J--add-modules=jdk.unsupported", "-J-Djava.class.path=$CLASSPATH")
} else {
 $PROG_ARGS = @("-J-Djava.class.path=$CLASSPATH")
}

if ($ARGS){
 $PROG_ARGS += $ARGS
}

echo $PROG_ARGS

try{
    pushd $JBOSS_HOME
    & $JAVA_HOME\bin\jconsole.exe $PROG_ARGS
}finally{
    popd
    Env-Clean-Up
}