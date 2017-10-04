<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ JBoss, Home of Professional Open Source.
  ~ Copyright 2015, Red Hat Middleware LLC, and individual contributors
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
      Adds a tunneling jgroup transport stack at the end of jgroups subsystem configuration.

      Example:

      <subsystem xmlns="urn:jboss:domain:jgroups:1.1" default-stack="tcp">
        <stacks>
          ...
          <stack name="tunnel">
              <transport type="TUNNEL" shared="false" socket-binding="jgroups-udp">
                  <property name="gossip_router_hosts">127.0.0.1[12001]</property>
              </transport>
              <protocol type="PING"/>
              <protocol type="MERGE2"/>
              <protocol type="FD_SOCK" socket-binding="jgroups-tcp-fd"/>
              <protocol type="FD"/>
              <protocol type="VERIFY_SUSPECT"/>
              <protocol type="pbcast.NAKACK"/>
              <protocol type="UNICAST2"/>
              <protocol type="pbcast.STABLE"/>
              <protocol type="pbcast.GMS"/>
              <protocol type="UFC"/>
              <protocol type="MFC"/>
              <protocol type="FRAG2"/>
              <protocol type="RSVP"/>
          </stack>
        </stacks>
      </subsystem>
    -->

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

    <!-- Namespaces -->
    <xsl:variable name="jgroupsns" select="'urn:jboss:domain:jgroups'"/>

    <!-- Parameters -->
    <xsl:param name="stack" select="'tunnel'"/>
    <xsl:param name="router-inet-address" select="'127.0.0.1'"/>
    <xsl:param name="router-port" select="'12001'"/>

    <!-- Add stack with TUNNEL transport -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $jgroupsns)]
                          /*[local-name()='stacks']
                          /*[local-name()='stack' and @name='udp']">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>

        <xsl:copy>
            <xsl:attribute name="name">
                <xsl:value-of select="$stack"/>
            </xsl:attribute>
            <xsl:element name="transport" namespace="{namespace-uri()}">
                <xsl:attribute name="type">
                    <xsl:value-of select="'TUNNEL'"/>
                </xsl:attribute>
                <xsl:attribute name="shared">
                    <xsl:value-of select="'false'"/>
                </xsl:attribute>
                <!-- https://issues.jboss.org/browse/WFLY-9378 socket-binding is required -->
                <!-- https://issues.jboss.org/browse/WFLY-9377 but reloading the server currently fails which complicates moving the script to use just CLI -->
                <xsl:attribute name="socket-binding">
                    <xsl:value-of select="'jgroups-udp'"/>
                </xsl:attribute>
                <xsl:element name="property" namespace="{namespace-uri()}">
                    <xsl:attribute name="name">
                        <xsl:value-of select="'gossip_router_hosts'"/>
                    </xsl:attribute>
                    <xsl:value-of select="concat($router-inet-address, '[', $router-port, ']')"/> <!-- 127.0.0.1[12001] -->
                </xsl:element>
                <xsl:element name="property" namespace="{namespace-uri()}">
                    <xsl:attribute name="name">
                        <xsl:value-of select="'reconnect_interval'"/>
                    </xsl:attribute>
                    <xsl:value-of select="4000"/>
                </xsl:element>
            </xsl:element>
            <xsl:for-each select="*[local-name()='protocol']">
                <xsl:copy>
                    <xsl:apply-templates select="@*|node()"/>
                    <xsl:if test="@type='MERGE3'">
                        <xsl:attribute name="type">
                            <xsl:value-of select="'MERGE3'"/>
                        </xsl:attribute>
                    </xsl:if>
                </xsl:copy>
            </xsl:for-each>
        </xsl:copy>

    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
