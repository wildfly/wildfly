<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Copyright The WildFly Authors
  ~ SPDX-License-Identifier: Apache-2.0
  -->

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!--
      XSLT stylesheet that makes multicast in stacks less chatty by setting or rather reducing TTL of all packets
      to given value by the 'mcast.ttl' variable.

      It sets TTL for all of:
      * transports of type UDP
      * protocols of type MPING

      The resulting configuration will look simiar to:

        <subsystem xmlns="urn:jboss:domain:jgroups:3.0" default-stack="tcp">
            <stack name="udp">
                <transport type="UDP" socket-binding="jgroups-udp">
                    <property name="ip_ttl">0</property>
                </transport>
        ...
    -->

    <xsl:variable name="jgroupsns" select="'urn:jboss:domain:jgroups:'"/>

    <xsl:param name="mcast.ttl" select="'0'"/>

    <xsl:output method="xml" indent="yes"/>

    <!-- Configure for all transport=UDP -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $jgroupsns)]
                          /*[local-name()='stacks']
                          /*[local-name()='stack']
                          /*[local-name()='transport' and @type='UDP']">
        <xsl:copy>
            <xsl:call-template name="copy-attributes"/>
            <xsl:call-template name="add-ttl-property"/>
        </xsl:copy>
    </xsl:template>

    <!-- Configure for all protocol=MPING -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $jgroupsns)]
                          /*[local-name()='stacks']
                          /*[local-name()='stack']
                          /*[local-name()='protocol' and @type='MPING']">
        <xsl:copy>
            <xsl:call-template name="copy-attributes"/>
            <xsl:call-template name="add-ttl-property"/>
        </xsl:copy>
    </xsl:template>

    <!-- Add property ip_ttl -->
    <xsl:template name="add-ttl-property">
        <xsl:element name="property" namespace="{namespace-uri()}">
            <xsl:attribute name="name">
                <xsl:value-of select="'ip_ttl'"/>
            </xsl:attribute>
            <xsl:value-of select="$mcast.ttl"/>
        </xsl:element>
    </xsl:template>

    <xsl:template name="copy-attributes">
        <xsl:for-each select="@*">
            <xsl:copy/>
        </xsl:for-each>
    </xsl:template>

    <!-- Copy everything else -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
