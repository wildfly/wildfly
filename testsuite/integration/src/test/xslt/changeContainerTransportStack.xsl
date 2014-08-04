<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:param name="container" select="'web'"/>
    <xsl:param name="stack" select="'udp'"/>

    <xsl:variable name="infinispanns" select="'urn:jboss:domain:infinispan:'"/>

    <xsl:output method="xml" indent="yes"/>

    <xsl:template name="copy-attributes">
        <xsl:for-each select="@*">
            <xsl:copy/>
        </xsl:for-each>
    </xsl:template>

    <!-- update the stack -->
    <xsl:template match="//*[local-name() = 'subsystem' and starts-with(namespace-uri(), $infinispanns)]
                          /*[local-name() = 'cache-container' and @name=$container]
                          /*[local-name() = 'transport']">
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
