<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ml="urn:jboss:module:1.5">
    <xsl:output method="xml" indent="yes"/>

    <xsl:param name="ds.jdbc.driver.resource" select="'fred'"/>
    <xsl:param name="ds.jdbc.driver.module.path" select="'wilma'"/>

    <xsl:variable name="module" select="translate($ds.jdbc.driver.module.path,'/','.')"/>

    <xsl:template match="@name[.='module-name']">
        <xsl:attribute name="name"><xsl:value-of select="$module" /></xsl:attribute>
    </xsl:template>

    <xsl:template match="@path[.='resource-path']">
        <xsl:attribute name="path"><xsl:value-of select="$ds.jdbc.driver.resource" /></xsl:attribute>
    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>

