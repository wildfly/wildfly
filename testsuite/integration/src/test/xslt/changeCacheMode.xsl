<?xml version="1.0" encoding="UTF-8"?>
<!-- Author: Radoslav Husar rhusar@redhat.com, Version: Dec 2011 -->

<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        version="1.0">

    <xsl:output method="xml" indent="yes"/>

    <xsl:variable name="nsInf" select="'urn:jboss:domain:infinispan:'"/>

    <!-- User params. -->
    <xsl:param name="name" select="'web'"/>
    <xsl:param name="mode" select="'SYNC'"/>

    <!-- Change cache mode for i:replicated-cache. -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsInf)]
   /*[local-name()='cache-container' and starts-with(namespace-uri(), $nsInf) and @name=$name]
   /*[local-name()='replicated-cache' and starts-with(namespace-uri(), $nsInf)]/@mode">
        <xsl:attribute name="mode">
            <xsl:value-of select="$mode"/>
        </xsl:attribute>
    </xsl:template>

    <!-- Change cache mode for i:distributed-cache. -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsInf)]
   /*[local-name()='cache-container' and starts-with(namespace-uri(), $nsInf) and @name=$name]
   /*[local-name()='distributed-cache' and starts-with(namespace-uri(), $nsInf)]/@mode">
        <xsl:attribute name="mode">
            <xsl:value-of select="$mode"/>
        </xsl:attribute>
    </xsl:template>

    <!-- Copy everything else untouched. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
