@echo off
call %*
goto :eof

:commonConf
if "x%COMMON_CONF%" == "x" (
   set "COMMON_CONF=%DIRNAME%common.conf.bat"
) else (
   if not exist "%COMMON_CONF%" (
       echo Config file not found "%COMMON_CONF%"
   )
)
if exist "%COMMON_CONF%" (
   call "%COMMON_CONF%" %*
)
goto :eof

:setPackageAvailable
    rem java -version actually writes what we all read in our terminals to stderr, not stdout!
    rem So we redirect it to stdout with 2>&1 before piping to findstr
    "%JAVA%" --add-opens=%~1=ALL-UNNAMED -version 2>&1 | findstr /i /c:"WARNING" >nul 2>&1 && (set PACKAGE_AVAILABLE=false) || (set PACKAGE_AVAILABLE=true)
goto :eof

:setEnhancedSecurityManager
    "%JAVA%" -Djava.security.manager=allow -version >nul 2>&1 && (set ENHANCED_SM=true) || (set ENHANCED_SM=false)
goto :eof

:setSecurityManagerDefault
  call :setEnhancedSecurityManager
  if "!ENHANCED_SM!" == "true" (
    rem Needed to be able to install Security Manager dynamically since JDK18
    set "SECURITY_MANAGER_CONFIG_OPTION=-Djava.security.manager=allow"
  )
goto:eof

:setModularJdk
    "%JAVA%" --add-modules=java.se -version >nul 2>&1 && (set MODULAR_JDK=true) || (set MODULAR_JDK=false)
goto :eof

:setDefaultModularJvmOptions
  call :setModularJdk
  if "!MODULAR_JDK!" == "true" (
    echo "%~1" | findstr /I "\-\-add\-modules" > nul
    if errorlevel == 1 (
      rem Set default modular jdk options
      rem Needed by the iiop-openjdk subsystem
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-exports=java.desktop/sun.awt=ALL-UNNAMED"
      rem Needed to instantiate the default InitialContextFactory implementation used by the
      rem Elytron subsystem dir-context and core management ldap-connection resources
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED"
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-exports=java.naming/com.sun.jndi.url.ldap=ALL-UNNAMED"
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-exports=java.naming/com.sun.jndi.url.ldaps=ALL-UNNAMED"
      rem Needed by Netty
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-exports=jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED"
      rem Needed by WildFly Elytron Extension
      set PACKAGE_NAME="java.base/com.sun.net.ssl.internal.ssl"
      call :setPackageAvailable !PACKAGE_NAME!
      if "!PACKAGE_AVAILABLE!" == "true" (
        set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-opens=!PACKAGE_NAME!=ALL-UNNAMED"
      )
      rem Needed if Hibernate applications use Javassist
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-opens=java.base/java.lang=ALL-UNNAMED"
      rem Needed by the MicroProfile REST Client subsystem
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-opens=java.base/java.lang.invoke=ALL-UNNAMED"
      rem Needed for marshalling of proxies
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-opens=java.base/java.lang.reflect=ALL-UNNAMED"
      rem Needed by JBoss Marshalling
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-opens=java.base/java.io=ALL-UNNAMED"
      rem Needed by WildFly Http Client
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-opens=java.base/java.net=ALL-UNNAMED"
      rem Needed by WildFly Security Manager
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-opens=java.base/java.security=ALL-UNNAMED"
      rem Needed for marshalling of collections
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-opens=java.base/java.util=ALL-UNNAMED"
      rem Needed for marshalling of concurrent collections
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-opens=java.base/java.util.concurrent=ALL-UNNAMED"
      rem EE integration with sar mbeans requires deep reflection in javax.management
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-opens=java.management/javax.management=ALL-UNNAMED"
      rem InitialContext proxy generation requires deep reflection in javax.naming
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-opens=java.naming/javax.naming=ALL-UNNAMED"
    ) else (
      set "DEFAULT_MODULAR_JVM_OPTIONS="
    )
  )
goto:eof
