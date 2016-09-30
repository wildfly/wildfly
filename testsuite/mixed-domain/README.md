Mixed Domain Test Suite
========================

This suite tests a domain containing old versions of WildFly .

Building
-------------------

Create a folder *old-releases/* somewhere on your disk

> mkdir old-releases

Download the zip files of the old releases you want to test into this folder

Now to run the tests:

> mvn clean install -DallTests -pl testsuite/mixed-domain -Djboss.test.mixed.domain.dir=/path/to/dir/with/old-releases/
