<?xml version="1.0" encoding="UTF-8"?>
<!-- Author: Radoslav Husar, Version: March 2012 -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:domain="urn:jboss:domain:1.3"
                xmlns:jgroups="urn:jboss:domain:jgroups:1.1"
>

    <!--
        An XSLT that will change the default JGroups transport stack.

        <subsystem xmlns="urn:jboss:domain:jgroups:1.1" default-stack="tcp">
    -->

    <!-- stack parameter -->
    <xsl:param name="defaultStack" select="'tcp'"/>

    <!-- only change default-stack attribute directly on jgroups subsystem -->
    <xsl:template match="//jgroups:subsystem">
        <xsl:copy>
            <xsl:attribute name="default-stack">
                <xsl:value-of select="$defaultStack"/>
            </xsl:attribute>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
