<?xml version="1.0" encoding="UTF-8"?>
<!-- Author: Radoslav Husar rhusar@redhat.com, Version: Dec 2011 -->

<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        version="1.0">

    <xsl:output method="xml" indent="yes"/>

    <xsl:variable name="nsInf" select="'urn:jboss:domain:infinispan:'"/>

    <!-- User params. -->
    <xsl:param name="name" select="'ejb'"/>
    <xsl:param name="channel" select="'ee'"/>

    <xsl:template match="node()|@*" name="identity">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <!-- add or modify cluster=* attribute on cache container transport. -->

    <!-- attribute exists - change its value in the transport element -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsInf)]
   /*[local-name()='cache-container' and starts-with(namespace-uri(), $nsInf) and @name=$name]
   /*[local-name()='transport' and starts-with(namespace-uri(), $nsInf)]/@channel">
        <xsl:attribute name="channel">
            <xsl:value-of select="$channel"/>
        </xsl:attribute>
    </xsl:template>

    <!-- attribute does not exist - add it to the transport element -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsInf)]
   /*[local-name()='cache-container' and starts-with(namespace-uri(), $nsInf) and @name=$name]
   /*[local-name()='transport' and starts-with(namespace-uri(), $nsInf)]">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name(.)}">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:for-each>
            <xsl:attribute name="channel">
                <xsl:value-of select="$channel"/>
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
