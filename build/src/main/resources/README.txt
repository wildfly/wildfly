       ______                     ___   _____    _____
      / / __ )____  __________   /   | / ___/   /__  /
 __  / / __  / __ \/ ___/ ___/  / /| | \__ \      / / 
/ /_/ / /_/ / /_/ (__  |__  )  / ___ |___/ /     / /  
\____/_____/\____/____/____/  /_/  |_/____/     /_/   

Welcome to JBoss Application Server 7.1.0
http://www.jboss.org/jbossas/

Go to the above link for documentation, and additional downloads.

Also, once JBoss AS7 is started you can go to http://localhost:8080
for additional information.


Key Features
--------------
* Java EE 6
* Fast Startup
* Small Footprint
* Modular Design
* Unified Configuration and Management
* Distributed Domain Management
* OSGi

Release Notes
-------------
You can obtain the release notes here:
https://community.jboss.org/wiki/AS710FinalReleaseNotes

Getting Started
----------------
JBoss AS 7 requires JDK 1.6 or later.  For information regarding installation
of the JDK, see http://www.oracle.com/technetwork/java/index.html

JBoss AS 7 has two modes of operation: Standalone and Domain.  For more
information regarding these modes, please refer to the documentation 
available on the JBoss.org site:

https://docs.jboss.org/author/display/AS71/Documentation


Starting a Standalone Server
----------------------------
An AS7 standalone server runs a single instance of AS7.

<JBOSS_HOME>/bin/standalone.sh      (Unix / Linux)

<JBOSS_HOME>\bin\standalone.bat     (Windows)


Starting a Managed Domain
--------------------------
An AS7 managed domain allows you to control and configure multiple instances 
of AS7, potentially across several physical (or virtual) machines.  The default 
configuration includes a domain controller and a single server group with three 
servers (two of which start automatically), all running on the localhost.

<JBOSS_HOME>/bin/domain.sh      (Unix / Linux)

<JBOSS_HOME>\bin\domain.bat     (Windows)
 

Accessing the Web Console
--------------------------
Once the server has started you can access the landing page:

http:/localhost:8080/

This page includes links to online documentation, quick start guides, forums 
and the administration console.


Stopping the Server
-------------------
The JBoss AS7 server can be stopped by pressing Ctrl-C on the command line.
If the server is running in a background process, the server can be stopped
using the JBoss CLI:

<JBOSS_HOME>/bin/jboss-cli.sh --connect --command=:shutdown

