<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Copyright The WildFly Authors
  ~ SPDX-License-Identifier: Apache-2.0
  -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!--
      Changes the default interface for the 'jgroups-mping' and 'jgroups-udp' clustering multicast socket bindings.

      Example:

      <socket-binding name="jgroups-mping" multicast-address="${jboss.default.multicast.address:230.0.0.4}"
                      multicast-port="45700" interface="multicast"/>

    -->


    <!-- Namespaces -->
    <xsl:variable name="domainns" select="'urn:jboss:domain:'"/>
    <xsl:variable name="jgroupsns" select="'urn:jboss:domain:jgroups:'"/>

    <!-- Parameters -->
    <xsl:param name="interface" select="'multicast'"/>

    <!-- Change 'jgroups-mping' binding interface. -->
    <xsl:template match="//*[local-name()='socket-binding-group' and @name='standard-sockets' and starts-with(namespace-uri(), $domainns)]
                          /*[local-name()='socket-binding' and @name='jgroups-mping']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <xsl:attribute name="interface">
                <xsl:value-of select="$interface"/>
            </xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <!-- Change 'jgroups-udp' binding interface. -->
    <xsl:template match="//*[local-name()='socket-binding-group' and @name='standard-sockets' and starts-with(namespace-uri(), $domainns)]
                          /*[local-name()='socket-binding' and @name='jgroups-udp']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <xsl:attribute name="interface">
                <xsl:value-of select="$interface"/>
            </xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
