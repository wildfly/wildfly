<?xml version="1.0" encoding="UTF-8"?>
<!-- Author: Radoslav Husar rhusar@redhat.com, Version: Dec 2011 -->

<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        version="1.0">

    <xsl:output method="xml" indent="yes"/>

    <xsl:variable name="nsEjb3" select="'urn:jboss:domain:ejb3:'"/>

    <!-- User params. -->
    <xsl:param name="cluster" select="'ejb'"/>

    <xsl:template match="node()|@*" name="identity">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <!-- add or modify cache-container=* attribute on passivation store named $cache. -->

    <!-- attribute exists - change its value in the passivation-store element -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsEjb3)]
   /*[local-name()='remote' and starts-with(namespace-uri(), $nsEjb3)]/@cluster">
        <xsl:attribute name="cluster">
            <xsl:value-of select="$cluster"/>
        </xsl:attribute>
    </xsl:template>

    <!-- attribute does not exist - add it to the passivation-store element -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsEjb3)]
   /*[local-name()='remote' and starts-with(namespace-uri(), $nsEjb3)]">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name(.)}">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:for-each>
            <xsl:attribute name="cluster">
                <xsl:value-of select="$cluster"/>
            </xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <!-- Copy everything else untouched. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
