


If somebody needs to create test reports as we all know them from Hudson or EAP testsuite, here is the example how.
For example: running jbossws-native testsuite.

  cd <your build directory>
  svn export https://svn.devel.redhat.com/repos/jboss-qa/reporttool
  ant -Dtest.report.xml="<test xml files spec>" -f reporttool/testreports.xml
 
<test xml files spec> :  Use ant file spec. E.g.,

   stack-native/modules/testsuite/**/target/surefire-reports/TEST-*.xml

Reports are located in ./reports directory.

  Enjoy!



(See https://docspace.corp.redhat.com/docs/DOC-25940 for latest info.)