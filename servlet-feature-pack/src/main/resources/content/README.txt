Welcome to WildFly (formerly known as JBoss Application Server)
http://www.wildfly.org/

Go to the above link for documentation, and additional downloads.

Also, once WildFly is started you can go to http://localhost:8080/
for additional information.


Key Features
------------
* Java EE 7 support
* Fast Startup
* Small Footprint
* Modular Design
* Unified Configuration and Management
* Distributed Domain Management

Release Notes
-------------
You can obtain the 10.0.0.Final release notes here:

http://wildfly.org/news/2016/01/29/WildFly10-Released/

Getting Started
---------------
WildFly requires JDK 1.8 or later. For information regarding installation
of the JDK, see http://www.oracle.com/technetwork/java/index.html

WildFly has two modes of operation: Standalone and Domain. For more
information regarding these modes, please refer to the documentation 
available on the JBoss.org site:

https://docs.jboss.org/author/display/WFLY10/Documentation


Starting a Standalone Server
----------------------------
A WildFly standalone server runs a single instance.

<JBOSS_HOME>/bin/standalone.sh      (Unix / Linux)

<JBOSS_HOME>\bin\standalone.bat     (Windows)


Starting a Managed Domain
-------------------------
A WildFly managed domain allows you to control and configure multiple instances,
potentially across several physical (or virtual) machines. The default 
configuration includes a domain controller and a single server group with three 
servers (two of which start automatically), all running on the localhost.

<JBOSS_HOME>/bin/domain.sh      (Unix / Linux)

<JBOSS_HOME>\bin\domain.bat     (Windows)
 

Accessing the Web Console
-------------------------
Once the server has started you can access the landing page:

http://localhost:8080/

This page includes links to online documentation, quick start guides, forums 
and the administration console.


Stopping the Server
-------------------
A WildFly server can be stopped by pressing Ctrl-C on the command line.
If the server is running in a background process, the server can be stopped
using the JBoss CLI:

<JBOSS_HOME>/bin/jboss-cli.sh --connect --command=:shutdown      (Unix / Linux)

<JBOSS_HOME>\bin\jboss-cli.bat --connect --command=:shutdown     (Windows)

