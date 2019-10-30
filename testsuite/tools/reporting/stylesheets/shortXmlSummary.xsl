<?xml version='1.0'?>

<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>

<xsl:param name="thedate"/>
<xsl:param name="java_version"/>
<xsl:param name="java_vendor"/>
<xsl:param name="java_vm_specification_version"/>
<xsl:param name="java_vm_version"/>
<xsl:param name="java_vm_name"/>
<xsl:param name="java_vm_info"/>
<xsl:param name="java_specification_version"/>
<xsl:param name="java_class_version"/>
<xsl:param name="os_name"/>
<xsl:param name="os_arch"/>
<xsl:param name="os_version"/>

<xsl:output method='xml'/> 

<xsl:template match='/'>

<xsl:variable name="numberOfTests" select="sum(//@tests)"/>
<xsl:variable name="numberOfErrors" select="sum(//@errors)"/>
<xsl:variable name="numberOfFailures" select="sum(//@failures)"/>
<xsl:variable name="numberOfSuccesses" select="$numberOfTests - $numberOfErrors - $numberOfFailures"/>

<testsuitesummary>

<testsuite>

  <time-of-test> <xsl:value-of select="$thedate"/> </time-of-test>

  <jdk-vendor> <xsl:value-of select="$java_vendor"/> </jdk-vendor>

  <jdk-version> <xsl:value-of select="$java_version"/> </jdk-version>

  <jvm-vm-spec-version> <xsl:value-of select="$java_vm_specification_version"/> </jvm-vm-spec-version>

  <jvm-name> <xsl:value-of select="$java_vm_name"/> </jvm-name>

  <jvm-version> <xsl:value-of select="$java_vm_version"/> </jvm-version>

  <jvm-info> <xsl:value-of select="$java_vm_info"/> </jvm-info>

  <java-spec-version> <xsl:value-of select="$java_specification_version"/> </java-spec-version>

  <java-class-version> <xsl:value-of select="$java_class_version"/> </java-class-version>

  <os-name> <xsl:value-of select="$os_name"/> </os-name>

  <os-arch> <xsl:value-of select="$os_arch"/> </os-arch>

  <os-version> <xsl:value-of select="$os_version"/> </os-version>

  <tests> <xsl:value-of select="$numberOfTests"/> </tests>

  <successes> <xsl:value-of select="$numberOfSuccesses"/> </successes>

  <errors> <xsl:value-of select="$numberOfErrors"/> </errors>

  <failures> <xsl:value-of select="$numberOfFailures"/> </failures>

</testsuite>

</testsuitesummary>

</xsl:template>

</xsl:stylesheet>
