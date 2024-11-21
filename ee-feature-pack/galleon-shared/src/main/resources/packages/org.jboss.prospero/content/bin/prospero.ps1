#############################################################################

#                                                                          ##

#    Startup Script for starting the server installation manager           ##

#                                                                          ##

#############################################################################

# based on https://github.com/wildfly/wildfly-core/blob/main/core-feature-pack/common/src/main/resources/content/bin/standalone.ps1

if($PSVersionTable.PSVersion.Major -lt 2) {

    Write-Warning "This script requires PowerShell 2.0 or better; you have version $($Host.Version)."

    return

}

$SCRIPTS_HOME = (Get-ChildItem $MyInvocation.MyCommand.Path).Directory.FullName

$RESOLVED_PROSPERO_HOME = (Get-ChildItem $MyInvocation.MyCommand.Path).Directory.Parent.FullName

# Setup PROSPERO_HOME

if((Test-Path env:PROSPERO_HOME) -and (Test-Path (Get-Item env:PROSPERO_HOME))) {# checks if env variable PROSPER_HOME is set and is valid folder

    $SANITIZED_PROSPERO_HOME = (Get-Item env:PROSPERO_HOME).FullName

    if($SANITIZED_PROSPERO_HOME -ne $RESOLVED_PROSPERO_HOME) {

        echo "WARNING PROSPERO_HOME may be pointing to a different installation - unpredictable results may occur."

        echo ""

    }

    $PROSPERO_HOME=$SANITIZED_PROSPERO_HOME

} else {

    # get the full path (without any relative bits)

    $PROSPERO_HOME=$RESOLVED_PROSPERO_HOME

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

Function Get-Env {

    $key = $args[0]

    if( Test-Path env:$key ) {

        return (Get-ChildItem env:$key).Value

    }

    return $args[1]

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

Function SetModularJDK {

    $MODULAR_JDK = $false

    & $JAVA --add-modules java.se -version >$null 2>&1

    if ($LastExitCode -eq 0){

        $MODULAR_JDK = $true

    }

    return $MODULAR_JDK

}

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

        # Needed if Hibernate applications use Javassist

        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-opens=java.base/java.lang=ALL-UNNAMED"

        # Needed by the MicroProfile REST Client subsystem

        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED"

        # Needed for marshalling of proxies

        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED"

        # Needed by JBoss Marshalling

        $DEFAULT_MODULAR_JVM_OPTIONS += "--add-opens=java.base/java.io=ALL-UNNAMED"

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



Function Get-Java-Arguments {

    Param(

        [Parameter(Mandatory=$true)]

        [string]$entryModule,

        [string]$logFileProperties = "$PROSPERO_HOME\bin\${prospero.dist.name}-logging.properties",

        [string[]]$serverOpts





    ) #end param

    $MODULAR_JDK = SetModularJDK

    $JAVA_OPTS = Get-Java-Opts #takes care of looking at defind settings and/or using env:JAVA_OPTS

    $DEFAULT_MODULAR_JVM_OPTS = Get-Default-Modular-Jvm-Options -opts $JAVA_OPTS -modularJDK $MODULAR_JDK

    $PROG_ARGS = @()

    if ($JAVA_OPTS -ne $null){

        $PROG_ARGS += $JAVA_OPTS

    }

    if ($DEFAULT_MODULAR_JVM_OPTS -ne $null){

        $PROG_ARGS += $DEFAULT_MODULAR_JVM_OPTS

    }

    if ( $JAVA_OPTS -inotmatch "logging.configuration")
    {
        if ($logFileProperties)
        {

            $PROG_ARGS += "-Dlogging.configuration=file:$logFileProperties"

        }
    }

    # Set default log location
    if ( $JAVA_OPTS -inotmatch "org.wildfly.prospero.log.file"){

        $PROG_ARGS += "-Dorg.wildfly.prospero.log.file=${PROSPERO_HOME}\@prospero.log.location.win@"

    }

    $PROG_ARGS += "-jar"

    $PROG_ARGS += "$TMP_JBOSS_MODULES"

    $PROG_ARGS += "-mp"

    $PROG_ARGS += "$PROSPERO_HOME\modules"

    $PROG_ARGS += $entryModule

    if ($ARGS -ne $null){

        $PROG_ARGS += $ARGS

    }

    return $PROG_ARGS

}

function Get-RandomFilename {
    [IO.Path]::GetFileNameWithoutExtension([System.IO.Path]::GetRandomFileName() )
}

$JAVA_OPTS = Get-Java-Opts

# Use a copy of jboss-modules to avoid locking issues when jboss-modules is updated
$TMP_JBOSS_MODULES = "$PROSPERO_HOME\jboss-modules-$(Get-RandomFilename).jar"
while(Test-Path "$TMP_JBOSS_MODULES"){
    Write-Host "regenerating $TMP_JBOSS_MODULES"
    $TMP_JBOSS_MODULES = "jboss-modules-$(Get-RandomFilename).jar"
}
Copy-Item "$PROSPERO_HOME\jboss-modules.jar" $TMP_JBOSS_MODULES

# Sample JPDA settings for remote socket debugging
# $JAVA_OPTS+= "-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n"
try
{
    $PROG_ARGS = Get-Java-Arguments -entryModule "org.jboss.prospero" -serverOpts $SERVER_OPTS

    & $JAVA $PROG_ARGS
} finally {
    Remove-Item $TMP_JBOSS_MODULES
}

