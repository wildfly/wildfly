JBoss Modular Application Server Build
--------------------------------------

This module contains a number of profiles each dedicated to a custom AS7 build.

Defining the Subsystems
-----------------------

Subsystems are defined by a simple comma seperated list of identifiers
Each identifier can be qualified with a supplement identifier.

	<standalone.subsystems>logging:osgi,osgi:eager,configadmin,deployment-scanner</standalone.subsystems>
	
Subsystem Definition Output
---------------------------

Given the above input the Ant task will generate a subsystem definition file

	standalone-subsystems.xml
	
	<config xmlns="urn:subsystems-config:1.0">
	    <subsystems>
	        <subsystem supplement="osgi">configuration/subsystems/logging.xml</subsystem>
	        <subsystem supplement="minimal">configuration/subsystems/osgi.xml</subsystem>
	        <subsystem>configuration/subsystems/configadmin.xml</subsystem>
	        <subsystem>configuration/subsystems/deployment-scanner.xml</subsystem>
	    </subsystems>
	</config>

which is then passed to the Ant macros that are used for the ordinary AS7 build.

Reducing the set of System Modules
----------------------------------

Given the above subsystem definition we obtain the set of extension modules and scan the transitive set of their dependencies 
from the respective module.xml definitions. This is a recursive process which produces a pattern file that can then be used to copy 
the modules hirarchy.

	standalone-module-dependencies.txt
	
	asm/asm/main/**
	ch/qos/cal10n/main/**
	com/github/relaxng/main/**
	com/google/guava/main/**
	com/sun/codemodel/main/**
	com/sun/istack/main/**
	com/sun/xml/bind/main/**
	com/sun/xml/messaging/saaj/main/**
	com/sun/xml/txw2/main/**
	com/sun/xsom/main/**
	javax/activation/api/main/**
	javax/annotation/api/main/**
	javax/api/main/**
	....
	
For debugging purposes this transitive set is also available in xml

	standalone-module-dependencies.xml
	
	...
    <module name="org.jboss.logging:main">
        <module name="org.jboss.logmanager:main">
            <module name="org.jboss.modules:main"/>
            <module name="org.jboss.as.logging:main" defined="true"/>
        </module>
    </module>
    ...
    
Resulting Server Build
----------------------

The resulting server build only contains the generated configurations and system modules that are needed to run the configured subsystems. 
Adding additional subsystems later will likely not work because of missing module dependencies.  
For the above case this reduces the download size from 128MB to 80MB. 

Domain Support
--------------

Domain support is optional and can be enabled/disabled by setting the 'domain.enabled' propperty accordingly. 
Enabling domain configs will include an additional large set of required module dependencies  
