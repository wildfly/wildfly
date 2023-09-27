<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Copyright The WildFly Authors
  ~ SPDX-License-Identifier: Apache-2.0
  -->
<!-- Author: Radoslav Husar, Version: July 2015 -->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!--
        An XSLT that will change the JGroups transport stack used by the default (ee) channel.

        <subsystem xmlns="urn:jboss:domain:jgroups:4.0">
            <channels default="ee">
                <channel name="ee" stack="tcp">
                ...
    -->

    <!-- Namespaces -->
    <xsl:variable name="jgroupsns" select="'urn:jboss:domain:jgroups:'"/>

    <!-- Parameters -->
    <xsl:param name="stack" select="'tcp'"/>
    <xsl:param name="channel" select="'ee'"/>

    <!-- Change stack attribute of the channel -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $jgroupsns)]
                          /*[local-name()='channels']
                          /*[local-name()='channel' and @name=$channel]
                          /@stack">
        <xsl:attribute name="stack">
            <xsl:value-of select="$stack"/>
        </xsl:attribute>
    </xsl:template>

    <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
