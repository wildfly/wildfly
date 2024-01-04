<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Copyright The WildFly Authors
  ~ SPDX-License-Identifier: Apache-2.0
  -->
<!-- Author: Radoslav Husar, Version: July 2015 -->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!--
        An XSLT that will change the default stack used by messaging. E.g.:

        <broadcast-group name="bg-group1" jgroups-stack="tcp" connectors="http-connector" jgroups-channel="activemq-cluster"/>
        <discovery-group name="dg-group1" jgroups-stack="tcp" jgroups-channel="activemq-cluster"/>
    -->

    <!-- Namespaces -->
    <xsl:variable name="messagingns" select="'urn:jboss:domain:messaging-activemq:'"/>

    <!-- Parameters -->
    <xsl:param name="stack" select="'tcp'"/>

    <!-- Change broadcast-group -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $messagingns)]
                          /*[local-name()='server']
                          /*[local-name()='broadcast-group']
                          /@jgroups-stack">
        <xsl:attribute name="jgroups-stack">
            <xsl:value-of select="$stack"/>
        </xsl:attribute>
    </xsl:template>

    <!-- Change discovery-group -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $messagingns)]
                          /*[local-name()='server']
                          /*[local-name()='discovery-group']
                          /@jgroups-stack">
        <xsl:attribute name="jgroups-stack">
            <xsl:value-of select="$stack"/>
        </xsl:attribute>
    </xsl:template>

    <!-- Traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
