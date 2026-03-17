Mixed Domain Test Suite
========================

This suite tests a domain containing old versions of WildFly .

Building
-------------------

Create a folder *old-releases/* somewhere on your disk

> mkdir old-releases

Download the zip files of the old releases you want to test into this folder

Now to run the tests:

```
mvn clean install -DskipTests && \
cd testsuite/mixed-domain && \
mvn clean install \
-Djboss.test.mixed.domain.dir=/path/to/dir/with/old-releases/ \
-Djava8.home=/usr/lib/jvm/java-1.8.0 \
-Djava17.home=/usr/lib/jvm/java-17
```

*Note*: The tests will only run if there is an available JDK version valid for each legacy EAP versions under test. Check the valid ranges for each EAP version in the [Version.AsVersion](src/test/java/org/jboss/as/test/integration/domain/mixed/Version.java) enums (maxVM/minVM) and define the javaX.home properties to establish the versions that cover all of EAP enums.

mvn clean install -DskipTests && \
cd testsuite/mixed-domain && \
mvn clean install \
-Djboss.test.mixed.domain.dir=/home/yborgess/dev/servers/mixed-domain \
-Djava8.home=/home/yborgess/.sdkman/candidates/java/8.0.462-tem \
-Djava17.home=/home/yborgess/.sdkman/candidates/java/17.0.12-tem
