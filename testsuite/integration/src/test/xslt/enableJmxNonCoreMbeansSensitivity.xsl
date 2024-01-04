<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Copyright The WildFly Authors
  ~ SPDX-License-Identifier: Apache-2.0
  -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="urn:jboss:domain:6.0"
        >
    <xsl:output indent="yes"/>

    <xsl:variable name="jmx" select="'urn:jboss:domain:jmx:'"/>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $jmx)]">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <xsl:element name="sensitivity" namespace="{namespace-uri()}">
                <xsl:attribute name="non-core-mbeans">true</xsl:attribute>
            </xsl:element>
        </xsl:copy>
    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
