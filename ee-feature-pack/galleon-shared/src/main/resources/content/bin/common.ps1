if($PSVersionTable.PSVersion.Major -lt 2) {
    Write-Warning "This script requires PowerShell 2.0 or better; you have version $($Host.Version)."
    return
}

$SCRIPTS_HOME = (Get-ChildItem $MyInvocation.MyCommand.Path).Directory.FullName
$RESOLVED_JBOSS_HOME = (Get-ChildItem $MyInvocation.MyCommand.Path).Directory.Parent.FullName


# A collection of functions that are used by the other scripts

Function Set-Env {
  $key = $args[0]
  $value = $args[1]
  Set-Content -Path env:$key -Value $value
}

Function Get-Env {
  $key = $args[0]
  if( Test-Path env:$key ) {
    return (Get-ChildItem env:$key).Value
  }
  return $args[1]
}
Function Get-Env-Boolean{
  $key = $args[0]
  if( Test-Path env:$key ) {
    return (Get-ChildItem env:$key).Value -eq 'true'
  }
  return $args[1]
}

$COMMOM_CONF_FILE = $SCRIPTS_HOME + '\common.conf.ps1'
$COMMOM_CONF_FILE = Get-Env COMMON_CONF $COMMOM_CONF_FILE
if ([System.IO.File]::Exists($COMMOM_CONF_FILE)) {
    . $COMMOM_CONF_FILE
} else {
    if (Test-Path env:COMMON_CONF) {
        Write-Output "Config file not found $env:COMMON_CONF"
    }
}

$global:SECMGR = Get-Env-Boolean SECMGR $false
$global:DEBUG_MODE=Get-Env DEBUG $false
$global:DEBUG_PORT=Get-Env DEBUG_PORT 8787
$global:RUN_IN_BACKGROUND=$false
$GC_LOG=Get-Env GC_LOG
#module opts that are passed to jboss modules
$global:MODULE_OPTS = @()

Function Get-String {
  $value = ''
  foreach($k in $args) {
    $value += $k
  }
  return $value
}

Function String-To-Array($value) {
  $res = @()
  if (!$value){
  	return $res
  }
  $tmpArr = $value.split()

  foreach ($str in $tmpArr) {
    if ($str) {
	  $res += $str
	}
  }
  return $res
}

Function Display-Environment {
Param(
   [string[]]$javaOpts
) #end param

if (-Not $javaOpts){
	$javaOpts = Get-Java-Opts
}

# Display our environment
Write-Host "================================================================================="
Write-Host ""
Write-Host "  JBoss Bootstrap Environment"
Write-Host ""
Write-Host "  JBOSS_HOME: $JBOSS_HOME"
Write-Host ""
Write-Host "  JAVA: $JAVA"
Write-Host ""
Write-Host "  MODULE_OPTS: $MODULE_OPTS"
Write-Host ""
Write-Host "  JAVA_OPTS: $javaOpts"
Write-Host ""
Write-Host "================================================================================="
Write-Host ""

}

#todo: bit funky at the moment, should probably be done via global variable
Function Get-Java-Opts {
	if($PRESERVE_JAVA_OPTS -ne 'true') { # if not perserve, then check for enviroment variable and use that
		if( (Test-Path env:JAVA_OPTS)) {
			$ops = Get-Env JAVA_OPTS
			# This is Powershell, so split the incoming string on a space into array
			return String-To-Array -value $ops
			Write-Host "JAVA_OPTS already set in environment; overriding default settings with values: $JAVA_OPTS"
		}
	}
	return $JAVA_OPTS
}

Function SetPackageAvailable($packageName) {
    # java -version actually writes what we all read in our terminals to stderr, not stdout!
    # So we redirect it to stdout with 2>&1 before piping to Out-String and Select-String
    $PACKAGE_AVAILABLE = !(& $JAVA "--add-opens=$packageName=ALL-UNNAMED" -version 2>&1 | Out-String -Stream | Select-String 'WARNING' -SimpleMatch -Quiet)
    return $PACKAGE_AVAILABLE
}

Function SetEnhancedSecurityManager {
    $ENHANCED_SM = $false
    & $JAVA "-Djava.security.manager=allow" -version >$null 2>&1
    if ($LastExitCode -eq 0){
        $ENHANCED_SM = $true
    }
    return $ENHANCED_SM
}

Function Get-Security-Manager-Default {
Param(
   [bool]$enhancedSM

) #end param
    if($PRESERVE_JAVA_OPTS -eq 'true') {
        return $null
    }
    $SECURITY_MANAGER_CONFIG_OPTION = @()
    if ($enhancedSM) {
        # Needed to be able to install Security Manager dynamically since JDK18
        $SECURITY_MANAGER_CONFIG_OPTION += "-Djava.security.manager=allow"
    }
    return $SECURITY_MANAGER_CONFIG_OPTION
}

Function SetModularJDK {
    $MODULAR_JDK = $false
    & $JAVA --add-modules java.se -version >$null 2>&1
    if ($LastExitCode -eq 0){
        $MODULAR_JDK = $true
    }
    return $MODULAR_JDK
}

Function Get-Default-Modular-Jvm-Options {
Param(
   [string[]]$opts,
   [bool]$modularJDK

) #end param
    if($PRESERVE_JAVA_OPTS -eq 'true') {
        return $null
    }
    $DEFAULT_MODULAR_JVM_OPTIONS = @()
    if ($modularJDK) {
        if ($opts -ne $null) {
              for($i=0; $i -lt $opts.Count; $i++) {
                  $arg = $opts[$i]
                  if ($arg -contains "--add-modules") {
                      return $DEFAULT_MODULAR_JVM_OPTIONS
                  }
              }
        }
        # Set default modular jdk options
        # Needed by the iiop-openjdk subsystem
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-exports=java.desktop/sun.awt=ALL-UNNAMED"
        # Needed to instantiate the default InitialContextFactory implementation used by the
        # Elytron subsystem dir-context and core management ldap-connection resources
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED"
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-exports=java.naming/com.sun.jndi.url.ldap=ALL-UNNAMED"
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-exports=java.naming/com.sun.jndi.url.ldaps=ALL-UNNAMED"
        # Needed by Netty
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-exports=jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED"
        # Needed by WildFly Elytron Extension
        $packageName = "java.base/com.sun.net.ssl.internal.ssl"
        $PACKAGE_AVAILABLE = setPackageAvailable($packageName)
        if($PACKAGE_AVAILABLE) {
            $DEFAULT_MODULAR_JVM_OPTIONS += "--add-opens=$packageName=ALL-UNNAMED"
        }
        # Needed if Hibernate applications use Javassist
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-opens=java.base/java.lang=ALL-UNNAMED"
        # Needed by the MicroProfile REST Client subsystem
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED"
        # Needed for marshalling of proxies
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED"
        # Needed by JBoss Marshalling
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-opens=java.base/java.io=ALL-UNNAMED"
        # Needed by WildFly Http Client
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-opens=java.base/java.net=ALL-UNNAMED"
        # Needed by WildFly Security Manager
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-opens=java.base/java.security=ALL-UNNAMED"
        # Needed for marshalling of collections
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-opens=java.base/java.util=ALL-UNNAMED"
        # Needed for marshalling of concurrent collections
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED"
        # EE integration with sar mbeans requires deep reflection in javax.management
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-opens=java.management/javax.management=ALL-UNNAMED"
        # InitialContext proxy generation requires deep reflection in javax.naming
        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-opens=java.naming/javax.naming=ALL-UNNAMED"
    }
    return $DEFAULT_MODULAR_JVM_OPTIONS
}

Function Display-Array($array){
	for ($i=0; $i -lt $array.length; $i++) {
		$v =  "$i " + $array[$i]
		Write-Host $v
	}
}


Function Get-Java-Arguments {
Param(
   [Parameter(Mandatory=$true)]
   [string]$entryModule,
   [string]$logFileProperties = "$JBOSS_CONFIG_DIR/logging.properties",
   [string]$logFile = "$JBOSS_LOG_DIR/server.log",
   [string[]]$serverOpts


) #end param
  $MODULAR_JDK = SetModularJDK
  $JAVA_OPTS = Get-Java-Opts #takes care of looking at defind settings and/or using env:JAVA_OPTS
  $DEFAULT_MODULAR_JVM_OPTS = Get-Default-Modular-Jvm-Options -opts $JAVA_OPTS -modularJDK $MODULAR_JDK
  $ENHANCED_SM = SetEnhancedSecurityManager
  $SECURITY_MANAGER_CONFIG_OPT = Get-Security-Manager-Default -enhancedSM $ENHANCED_SM

  $PROG_ARGS = @()
  if ($JAVA_OPTS -ne $null){
  	$PROG_ARGS += $JAVA_OPTS
  }
  if ($DEFAULT_MODULAR_JVM_OPTS -ne $null){
  	$PROG_ARGS += $DEFAULT_MODULAR_JVM_OPTS
  }
  if ($SECURITY_MANAGER_CONFIG_OPT -ne $null){
  	$PROG_ARGS += $SECURITY_MANAGER_CONFIG_OPT
  }
  if ($logFile){
  	$PROG_ARGS += "-Dorg.jboss.boot.log.file=$logFile"
  }
  if ($logFileProperties){
  	$PROG_ARGS += "-Dlogging.configuration=file:$logFileProperties"
  }
  $PROG_ARGS += "-Djboss.home.dir=$JBOSS_HOME"
  $PROG_ARGS += "-Djboss.server.base.dir=$global:JBOSS_BASE_DIR"
  $PROG_ARGS += "-Djboss.server.config.dir=$global:JBOSS_CONFIG_DIR"

  if ($GC_LOG -eq $true){
    $dir = New-Item $JBOSS_LOG_DIR -type directory -ErrorAction SilentlyContinue
    if ($PROG_ARGS -notmatch "-Xlog:?gc"){
        Rotate-GC-Logs

        & $JAVA -Xverbosegclog:"$JBOSS_LOG_DIR\gc.log" java.se -version >$null 2>&1
        if ($LastExitCode -eq 0){
            $PROG_ARGS += "-Xverbosegclog:`\`"$JBOSS_LOG_DIR\gc.log`\`""
        }elseif ($MODULAR_JDK -eq $true)
        {
            $PROG_ARGS += "-Xlog:gc*:file=`\`"$JBOSS_LOG_DIR\gc.log`\`":time,uptimemillis:filecount=5,filesize=3M"
        } else {
            $PROG_ARGS += "-verbose:gc"
            $PROG_ARGS += "-XX:+PrintGCDetails"
            $PROG_ARGS += "-XX:+PrintGCDateStamps"
            $PROG_ARGS += "-XX:+UseGCLogFileRotation"
            $PROG_ARGS += "-XX:NumberOfGCLogFiles=5"
            $PROG_ARGS += "-XX:GCLogFileSize=3M"
            $PROG_ARGS += "-XX:-TraceClassUnloading"
            $PROG_ARGS += "-Xloggc:$JBOSS_LOG_DIR\gc.log"
        }
    }
  }

  $global:FINAL_JAVA_OPTS = $PROG_ARGS

  $PROG_ARGS += "-jar"
  $PROG_ARGS += "$JBOSS_HOME\jboss-modules.jar"
  if ($MODULE_OPTS -ne $null){
  	$PROG_ARGS += $MODULE_OPTS
  }
  $PROG_ARGS += "-mp"
  $PROG_ARGS += "$JBOSS_MODULEPATH"
  $PROG_ARGS += $entryModule
  if ($serverOpts -ne $null){
  	$PROG_ARGS += $serverOpts
  }
  return $PROG_ARGS
}

Function Process-Script-Parameters {
Param(
   [Parameter(Mandatory=$false)]
   [string[]]$Params

) #end param
    $res = @()
	for($i=0; $i -lt $Params.Count; $i++){
		$arg = $Params[$i]
		if ($arg -eq '--debug'){
			$global:DEBUG_MODE=$true
			if ($args[$i+1] -match '\d+'){ #port number can only follow --debug
				$global:DEBUG_PORT = $Params[$i+1]
				$i++
				continue
			}
		}elseif ($arg -contains '-Djava.security.manager'){
			Write-Warning "ERROR: The use of -Djava.security.manager has been removed. Please use the -secmgr command line argument or SECMGR=true environment variable."
			exit
		}elseif ($arg -eq '-secmgr'){
			$global:SECMGR = $true
		}elseif ($arg -eq '--background'){
			$global:RUN_IN_BACKGROUND = $true
		}else{
			$res+=$arg
		}
	}
	return $res
}

Function Process-Java-Opts-Parameters {
Param(
   [Parameter(Mandatory=$false)]
   [string[]]$Params

) #end param
    $res = @()
	for($i=0; $i -lt $Params.Count; $i++){
		$arg = $Params[$i]
		if ($arg -contains '-Djava.security.manager'){
			Write-Warning "ERROR: The use of -Djava.security.manager has been removed. Please use the -secmgr command line argument or SECMGR=true environment variable."
			exit
		}else{
			$res+=$arg
		}
	}
	return $res
}

Function Start-WildFly-Process {
 Param(
   [Parameter(Mandatory=$true)]
   [string[]] $programArguments,
   [boolean] $runInBackground = $false

) #end param

	if(($JBOSS_PIDFILE -ne '') -and (Test-Path $JBOSS_PIDFILE)) {
		$processId = gc $JBOSS_PIDFILE
		if ($processId -ne $null){
			$proc = Get-Process -Id $processId -ErrorAction SilentlyContinue
		}
		if ($proc -ne $null){
			Write-Warning "Looks like a server process is already running. If it isn't then, remove the $JBOSS_PIDFILE and try again"
			return
		}else{
			Remove-Item $JBOSS_PIDFILE
		}
	}

	if($runInBackground) {
		$process = Start-Process -FilePath $JAVA -ArgumentList $programArguments -NoNewWindow -RedirectStandardOutput $global:CONSOLE_LOG -WorkingDirectory $JBOSS_HOME -PassThru
		$processId = $process.Id;
		echo "Started process in background, process id: $processId"
		if ($JBOSS_PIDFILE -ne $null){
			$processId >> $JBOSS_PIDFILE
		}
	} else {
		try{
			pushd $JBOSS_HOME
			& $JAVA $programArguments
			if ($LastExitCode -eq 10){ # :shutdown(restart=true) was called
			    Write-Host "INFO: Restarting..."
				Start-WildFly-Process -programArguments $programArguments
			} elseif ($LastExitCode -eq 20) { # :shutdown(perform-installation=true) was called
                Write-Host "INFO: Executing the installation manager"
                & "$JBOSS_HOME\bin\installation-manager.ps1" -installationHome "$JBOSS_HOME" -instMgrLogProperties "$JBOSS_CONFIG_DIR\logging.properties"
                Write-Host "INFO: Restarting..."
                Start-WildFly-Process -programArguments $programArguments
            }
		}finally{
			popd
		}
	}
	Env-Clean-Up
}

Function Set-Global-Variables {
PARAM(
[Parameter(Mandatory=$true)]
   [string]$baseDir
)

	# determine the default base dir, if not set
	$global:JBOSS_BASE_DIR = $baseDir;

	# determine the default log dir, if not set
	$global:JBOSS_LOG_DIR = Get-Env JBOSS_LOG_DIR $JBOSS_BASE_DIR\log

	# determine the default configuration dir, if not set
	$global:JBOSS_CONFIG_DIR = Get-Env JBOSS_CONFIG_DIR $JBOSS_BASE_DIR\configuration

	$global:CONSOLE_LOG = $JBOSS_LOG_DIR + '\console.log'
}

Function Set-Global-Variables-Standalone {
	$dir = Get-Env JBOSS_BASE_DIR $JBOSS_HOME\standalone
	Set-Global-Variables -baseDir $dir
}

Function Set-Global-Variables-Domain {
	$dir = Get-Env JBOSS_BASE_DIR $JBOSS_HOME\domain
	Set-Global-Variables -baseDir $dir
}

Function Env-Clean-Up {
	[Environment]::SetEnvironmentVariable("JBOSS_HOME", $PRE_JBOSS_HOME, "Process")
}

Function Rotate-GC-Logs {
	mv -ErrorAction SilentlyContinue $JBOSS_LOG_DIR/gc.log $JBOSS_LOG_DIR/backupgc.log
	mv -ErrorAction SilentlyContinue $JBOSS_LOG_DIR/gc.log.0 $JBOSS_LOG_DIR/backupgc.log.0
	mv -ErrorAction SilentlyContinue $JBOSS_LOG_DIR/gc.log.1 $JBOSS_LOG_DIR/backupgc.log.1
	mv -ErrorAction SilentlyContinue $JBOSS_LOG_DIR/gc.log.2 $JBOSS_LOG_DIR/backupgc.log.2
	mv -ErrorAction SilentlyContinue $JBOSS_LOG_DIR/gc.log.3 $JBOSS_LOG_DIR/backupgc.log.3
	mv -ErrorAction SilentlyContinue $JBOSS_LOG_DIR/gc.log.4 $JBOSS_LOG_DIR/backupgc.log.4
	mv -ErrorAction SilentlyContinue $JBOSS_LOG_DIR/gc.log.*.current $JBOSS_LOG_DIR/backupgc.log.current
}

# Setup JBOSS_HOME
if((Test-Path env:JBOSS_HOME) -and (Test-Path (Get-Item env:JBOSS_HOME))) {# checks if env variable jboss is set and is valid folder
  $SANITIZED_JBOSS_HOME = (Get-Item env:JBOSS_HOME).FullName
  if($SANITIZED_JBOSS_HOME -ne $RESOLVED_JBOSS_HOME) {
    echo "WARNING JBOSS_HOME may be pointing to a different installation - unpredictable results may occur."
    echo ""
  }
  $JBOSS_HOME=$SANITIZED_JBOSS_HOME
} else {
    # get the full path (without any relative bits)
    $JBOSS_HOME=$RESOLVED_JBOSS_HOME
}

# Setup the JVM
if (!(Test-Path env:JAVA)) {
  if( Test-Path env:JAVA_HOME) {
	$JAVA_HOME = (Get-ChildItem env:JAVA_HOME).Value
	$JAVA = $JAVA_HOME + "\bin\java.exe"
  } else {
    $JAVA = 'java'
  }
}

# determine the default module path, if not set
$JBOSS_MODULEPATH = Get-Env JBOSS_MODULEPATH $JBOSS_HOME\modules

Set-Global-Variables-Standalone

# Determine the default JBoss PID file
$JBOSS_PIDFILE = Get-Env JBOSS_PIDFILE $SCRIPTS_HOME\process.pid

# Set the PRE_JBOSS_HOME variable as the $env:JBOSS_HOME or $null, used in function: Env-Clean-Up
$PRE_JBOSS_HOME = Get-Env JBOSS_HOME $null

[Environment]::SetEnvironmentVariable("JBOSS_HOME", $JBOSS_HOME, "Process")
