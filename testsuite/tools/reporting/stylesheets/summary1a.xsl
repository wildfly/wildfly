<?xml version='1.0'?>

<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>

<xsl:param name="thedate"/>
<xsl:param name="java_version"/>
<xsl:param name="java_vendor"/>
<xsl:param name="java_vm_version"/>
<xsl:param name="java_vm_name"/>
<xsl:param name="java_vm_info"/>
<xsl:param name="os_name"/>
<xsl:param name="os_arch"/>
<xsl:param name="os_version"/>
<xsl:param name="builduid"/>
<xsl:param name="results_web"/>


<xsl:output method='text'/> 

<xsl:template match='/'>
<xsl:variable name="numberOfTests" select="sum(//@tests)"/>
<xsl:variable name="numberOfErrors" select="sum(//@errors)"/>
<xsl:variable name="numberOfFailures" select="sum(//@failures)"/>
<xsl:variable name="numberOfSuccesses" select="$numberOfTests - $numberOfErrors - $numberOfFailures"/>
Number of tests run:   <xsl:value-of select="$numberOfTests"/>

--------------------------------------------

Successful tests:      <xsl:value-of select="$numberOfSuccesses"/>
Errors:                <xsl:value-of select="$numberOfErrors"/>
Failures:              <xsl:value-of select="$numberOfFailures"/>

--------------------------------------------

[time of test: <xsl:value-of select="$thedate"/> GMT]
[java.version: <xsl:value-of select="$java_version"/>]
[java.vendor: <xsl:value-of select="$java_vendor"/>]
[java.vm.version: <xsl:value-of select="$java_vm_version"/>]
[java.vm.name: <xsl:value-of select="$java_vm_name"/>]
[java.vm.info: <xsl:value-of select="$java_vm_info"/>]
[os.name: <xsl:value-of select="$os_name"/>]
[os.arch: <xsl:value-of select="$os_arch"/>]
[os.version: <xsl:value-of select="$os_version"/>]

Useful resources:

- <xsl:value-of select="$results_web"/>/<xsl:value-of select="$builduid"/> for the junit report of this test.
- <xsl:value-of select="$results_web"/>/<xsl:value-of select="$builduid"/>/logs/ for the logs for this test.

NOTE: If there are any errors shown above - this mail is only highlighting 
them - it is NOT indicating that they are being looked at by anyone.
Remember - if a test becomes broken after your changes - fix it or fix the test!

--------------------------------------------

<xsl:if test='$numberOfFailures!=0 or $numberOfErrors!=0'>

Oh dear - still got some errors!

</xsl:if>

<xsl:if test='$numberOfFailures=0 and $numberOfErrors=0'>

HURRAY - everything worked!

</xsl:if>

Thanks for all your effort - we really do love you!

</xsl:template>

</xsl:stylesheet>
