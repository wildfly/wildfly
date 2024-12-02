Testing subsystems
==================
First build the project using `mvn install`


Modify standalone.xml
---------------------

In `$WFLY/standalone/configuration/standalone.xml` file, do the following changes.
* Add module to the `<extensions>` section.
  ```
  <extensions>
        ...
        <extension module="org.wildfly.extension.microprofile.lra-coordinator"/>
        <extension module="org.wildfly.extension.microprofile.lra-participant"/>
    </extensions>
  ```
* And then we have to add our subsystem to the `<profile>` section:
  ```
  <profile>
  ...
      <subsystem xmlns="urn:wildfly:microprofile-lra-coordinator:1.0" host="default-host" server="default-server"/>
      <subsystem xmlns="urn:wildfly:microprofile-lra-participant:1.0" url="http://localhost:8080/lra-coordinator"/>
  </profile>
  ```

Now start the WildFly server:

    $WFLY/bin/standalone.sh