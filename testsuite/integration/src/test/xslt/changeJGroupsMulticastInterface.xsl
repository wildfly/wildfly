<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ JBoss, Home of Professional Open Source.
  ~ Copyright 2014, Red Hat Middleware LLC, and individual contributors
  ~ as indicated by the @author tags. See the copyright.txt file in the
  ~ distribution for a full listing of individual contributors.
  ~
  ~ This is free software; you can redistribute it and/or modify it
  ~ under the terms of the GNU Lesser General Public License as
  ~ published by the Free Software Foundation; either version 2.1 of
  ~ the License, or (at your option) any later version.
  ~
  ~ This software is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  ~ Lesser General Public License for more details.
  ~
  ~ You should have received a copy of the GNU Lesser General Public
  ~ License along with this software; if not, write to the Free
  ~ Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  ~ 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
