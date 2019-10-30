<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:param name="thedate">undefined</xsl:param>

<xsl:output method='html' indent='yes' doctype-public='-//W3C//DTD HTML 3.2 FINAL//EN'/>


<xsl:template match="/">

<xsl:variable name="numberOfTests" select="sum(//@tests)"/>
<xsl:variable name="numberOfErrors" select="sum(//@errors)"/>
<xsl:variable name="numberOfFailures" select="sum(//@failures)"/>
<xsl:variable name="numberOfSuccesses" select="$numberOfTests - $numberOfErrors - $numberOfFailures"/>

<html>
<head>

<META NAME="ROBOTS" CONTENT="ALL"/>
<meta name="rating" content="Safe For Kids"/>

<title>JBoss Test Results</title>
</head>
<body bgcolor='white'>

<p/>

<hr/>

The results of the latest <a href='http://jboss.org'>JBoss</a> daily build 
and test results - make sure the JBoss Group do not let anything slip!
<br/>

<i>The tests are run around 2:30am GMT each day on <a href="http://lubega.com">lubega.com</a> - so expect the files to be 
empty/half complete around that time.  Also, the tests are run using 3 JDKs - Sun, IBM and Blackdown.  Only the last
run is shown on this web page - follow the archives link for the earlier runs.</i>

<p/>
<b>Date of last successful run: <xsl:value-of select="$thedate"/> GMT</b>
<ul>
 <li><a href='#tests'>Test Results</a></li>
 <li><a href='#javadocs'>Javadocs of the JBoss modules</a></li>
 <li><a href='#testlogs'>Test Logs</a></li>
 <li><a href='#links'>Useful Links</a></li>
 <li><a href='#source'>How tests were run</a></li>
</ul>

<hr/>

<table width='100%' border='1'>
<tr valign='top'><td>

<a name='tests'/>

<h3>Test Results</h3>


<p/>

What were the results?
<ul>
  <li><a href='TEST-all-test-results.log'>summary of test results</a> - that is, whats in the email</li>
  <li><a href='TEST-all-test-results.xml'>all test results combined into one file</a></li>
  <li><a href='testarchive/?M=D'>archive</a> - the last months worth of results</li>
</ul>


</td><td>

<table border='1'>

 <tr bgcolor='lightblue'>
  <th>Test Name</th>
  <th>Tests (<xsl:value-of select='$numberOfTests'/>)</th>
  <th>Successes (<xsl:value-of select='$numberOfSuccesses'/>)</th>
  <th>Failures (<xsl:value-of select='$numberOfFailures'/>)</th>
  <th>Errors (<xsl:value-of select='$numberOfErrors'/>)</th>
  <th>-</th>
 </tr>

 <xsl:for-each select="//testsuite">
  <xsl:sort select="@name"/>
  <tr>
   <td>
       <a><xsl:attribute name='href'>detailed-results.html#<xsl:value-of select='@name'/></xsl:attribute>
         <xsl:value-of select='@name'/>
       </a>
       (<a><xsl:attribute name='href'>TEST-<xsl:value-of select='@name'/>.xml</xsl:attribute>
        xml 
       </a>)
   </td>
   <td bgcolor='yellow'>
    <xsl:if test='@failures!=0 or @errors!=0'><xsl:attribute name='bgcolor'>red</xsl:attribute>
    </xsl:if>
       <a><xsl:attribute name='href'>detailed-results.html#<xsl:value-of select='@name'/></xsl:attribute>
         <xsl:value-of select='@tests'/>
       </a>
   </td>
   <td bgcolor='gold'> 
       <a><xsl:attribute name='href'>detailed-results.html#<xsl:value-of select='@name'/></xsl:attribute>
         <xsl:value-of select='@tests - @errors - @failures'/>
       </a>
   </td>
   <td>
    <xsl:if test='@failures=0'><xsl:attribute name='bgcolor'>lightgreen</xsl:attribute></xsl:if>
    <xsl:if test='@failures!=0'><xsl:attribute name='bgcolor'>red</xsl:attribute></xsl:if>
       <a><xsl:attribute name='href'>detailed-results.html#<xsl:value-of select='@name'/></xsl:attribute>
         <xsl:value-of select='@failures'/>
       </a>
   </td>
   <td>
    <xsl:if test='@errors=0'><xsl:attribute name='bgcolor'>lightgreen</xsl:attribute></xsl:if>
    <xsl:if test='@errors!=0'><xsl:attribute name='bgcolor'>red</xsl:attribute></xsl:if>
       <a><xsl:attribute name='href'>detailed-results.html#<xsl:value-of select='@name'/></xsl:attribute>
         <xsl:value-of select='@errors'/>
       </a>
   </td>
   <td>
    <xsl:if test='@failures=0 and @errors=0'><xsl:attribute name='bgcolor'>lightgreen</xsl:attribute>passed!
    </xsl:if>
    <xsl:if test='@failures!=0 or @errors!=0'><xsl:attribute name='bgcolor'>red</xsl:attribute>failures!
    </xsl:if>
   </td>
  </tr>
 </xsl:for-each>

</table>

Environment Information:<br/>
[java.version: <xsl:value-of select="$java_version"/>]
[java.vendor: <xsl:value-of select="$java_vendor"/>]
[java.vm.version: <xsl:value-of select="$java_vm_version"/>]
[java.vm.name: <xsl:value-of select="$java_vm_name"/>]
[java.vm.info: <xsl:value-of select="$java_vm_info"/>]
[os.name: <xsl:value-of select="$os_name"/>]
[os.arch: <xsl:value-of select="$os_arch"/>]
[os.version: <xsl:value-of select="$os_version"/>]

</td></tr><tr valign='top'><td>

<a name='javadocs'/>

<h3>Javadocs</h3>

The following <b>javadocs</b> are available:
<ul>
  <li><a href='jboss/build/docs/api/'>jboss</a></li>
  <li><a href='jbosssx/build/docs/api/'>jbosssx</a></li>
  <li><a href='jbosscx/build/docs/api/'>jbosscx</a></li>
  <li><a href='jbossmx/build/docs/api/'>jbossmx</a></li>
  <li><a href='jbosspool/build/docs/api/'>jbosspool (aka minerva)</a></li>
  <li><a href='jboss-j2ee/build/docs/api/'>jboss-j2ee</a></li>
  <li><a href='jbossmq/build/docs/api/'>jbossmq</a></li>
  <li><a href='zoap/build/docs/api/'>zoap</a></li>
  <li><a href='zola/ZOL/ZOL-2.0/docs/javadoc/'>zola</a></li>
  <li><a href='jbosstest/build/docs/api/'>jbosstest</a></li>
</ul>

</td><td>


<a name='testlogs'/>

<h3>Test Logs</h3>

<a href='cronjob.log'>Overall log file for tests</a><p/>

The jboss server logs:
<ul>
 <li><a href='jboss/dist/log/server.log'>server.log</a></li>
 <li><a href='jboss/dist/log/trace.log'>trace.log</a></li>
</ul>
<p/>

The test run log is <a href='jbosstest/src/build/cronjob_test.log'>here</a>

</td></tr><tr valign='top'><td>


<a name='links'/>

<h3>Useful Links</h3>

<ul>
 <li><a href='http://www.jboss.org'>JBoss</a></li>
 <li><a href='http://java.sun.com'>Sun and Java</a></li>
 <li><a href='http://java.sun.com/j2ee'>Sun and J2EE</a></li>
 <li><a href='http://www.theserverside.com'>News and Tips on Java on the Server</a></li>
</ul>

</td><td>


<a name='source'/>

<h3>Source to the scripts that run the tests</h3>

How are these tests performed - using these scripts
<ul>
  <li><a href='cronjob.sh'>cronjob.sh</a>
  <ul>
    <li><a href='cronjob_clean.sh'>cronjob_clean.sh</a></li>
    <li><a href='cronjob_build.sh'>cronjob_build.sh</a></li>
    <li><a href='cronjob_build.sh'>cronjob_javadocs.sh</a></li>
    <li><a href='cronjob_test.sh'>cronjob_test.sh</a></li>
    <li><a href='cronjob_setup.sh'>cronjob_setup.sh</a></li>
    <li><a href='cronjob_mail.sh'>cronjob_mail.sh</a></li>
  </ul> 
  </li>
</ul>

</td></tr></table>

<hr/>

<font color='navy'>
<i><a href='mailto:chris@kimptoc.net'>chris@kimptoc.net</a></i>
</font>


</body>
</html>

</xsl:template>
</xsl:stylesheet>

