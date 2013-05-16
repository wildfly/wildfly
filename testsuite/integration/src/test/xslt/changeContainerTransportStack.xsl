<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ispn="urn:jboss:domain:infinispan:2.0">

    <xsl:param name="container" select="'web'"/>
    <xsl:param name="stack" select="'udp'"/>

    <xsl:output method="xml" indent="yes"/>

    <xsl:template name="copy-attributes">
        <xsl:for-each select="@*">
            <xsl:copy/>
        </xsl:for-each>
    </xsl:template>

    <!-- update the stack -->
    <xsl:template match="//ispn:subsystem/ispn:cache-container[@name=$container]/ispn:transport">
        <xsl:copy>
            <xsl:call-template name="copy-attributes"/>
            <xsl:attribute name="stack">
                <xsl:value-of select="$stack"/>
            </xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <!-- copy everything else -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
