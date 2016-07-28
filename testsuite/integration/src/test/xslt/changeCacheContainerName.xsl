<?xml version="1.0" encoding="UTF-8"?>
<!-- Author: Radoslav Husar rhusar@redhat.com, Version: Dec 2011 -->

<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        version="1.0">

    <xsl:output method="xml" indent="yes"/>

    <xsl:variable name="nsInf" select="'urn:jboss:domain:infinispan:'"/>

    <!-- User params. -->
    <xsl:param name="name" select="'ejb'"/>
    <xsl:param name="newName" select="'ejb'"/>

    <xsl:template match="node()|@*" name="identity">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <!-- change the name attribute on the container, if it matches the name parameter value $name  -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsInf)]
   /*[local-name()='cache-container' and starts-with(namespace-uri(), $nsInf)]/@name[.=$name]">
        <xsl:attribute name="name">
            <xsl:value-of select="$newName"/>
        </xsl:attribute>
    </xsl:template>

    <!-- Copy everything else untouched. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
