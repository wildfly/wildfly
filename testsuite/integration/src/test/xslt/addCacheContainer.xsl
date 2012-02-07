<?xml version="1.0" encoding="UTF-8"?>
<!-- See http://www.w3.org/TR/xslt -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ispn="urn:jboss:domain:infinispan:1.1" version="1.0">
    <xsl:output method="xml" indent="yes"/>

    <xsl:param name="filename">none</xsl:param>

    <!-- replace the old definition with the new -->
    <xsl:template match="//ispn:subsystem/ispn:cache-container">
        <!-- http://docs.jboss.org/ironjacamar/userguide/1.0/en-US/html/deployment.html#deployingds_descriptor -->
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
        <xsl:copy-of select="document($filename)" />
    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>

