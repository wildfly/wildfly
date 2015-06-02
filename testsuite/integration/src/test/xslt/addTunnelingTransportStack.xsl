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
      Adds a tunneling jgroup transport stack at the end of jgroups subsystem configuration.

      Example:

      <subsystem xmlns="urn:jboss:domain:jgroups:1.1" default-stack="tcp">
        <stacks>
          ...
          <stack name="tunnel">
              <transport type="TUNNEL" shared="false">
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

    <!-- Namespaces -->
    <xsl:variable name="domainns" select="'urn:jboss:domain:jgroups'"/>

    <!-- Parameters -->
    <xsl:param name="stack" select="'tunnel'"/>
    <xsl:param name="router-inet-address" select="'127.0.0.1'"/>
    <xsl:param name="router-port" select="'12001'"/>

    <!-- Add stack with TUNNEL transport -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $domainns)]
                          /*[local-name()='stacks']
                          /*[local-name()='stack'][last()]">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>

        <xsl:element name="stack" namespace="{namespace-uri()}">
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
                    <xsl:value-of select="4000"/> <!-- 127.0.0.1[12001] -->
                </xsl:element>
            </xsl:element>
            <xsl:element name="protocol" namespace="{namespace-uri()}">
                <xsl:attribute name="type">
                    <xsl:value-of select="'PING'"/>
                </xsl:attribute>
            </xsl:element>
            <xsl:element name="protocol" namespace="{namespace-uri()}">
                <xsl:attribute name="type">
                    <xsl:value-of select="'MERGE2'"/>
                </xsl:attribute>
            </xsl:element>
            <xsl:element name="protocol" namespace="{namespace-uri()}">
                <xsl:attribute name="type">
                    <xsl:value-of select="'FD_SOCK'"/>
                </xsl:attribute>
                <xsl:attribute name="socket-binding">
                    <xsl:value-of select="'jgroups-tcp-fd'"/>
                </xsl:attribute>
            </xsl:element>
            <xsl:element name="protocol" namespace="{namespace-uri()}">
                <xsl:attribute name="type">
                    <xsl:value-of select="'FD'"/>
                </xsl:attribute>
            </xsl:element>
            <xsl:element name="protocol" namespace="{namespace-uri()}">
                <xsl:attribute name="type">
                    <xsl:value-of select="'VERIFY_SUSPECT'"/>
                </xsl:attribute>
            </xsl:element>
            <xsl:element name="protocol" namespace="{namespace-uri()}">
                <xsl:attribute name="type">
                    <xsl:value-of select="'pbcast.NAKACK'"/>
                </xsl:attribute>
            </xsl:element>
            <xsl:element name="protocol" namespace="{namespace-uri()}">
                <xsl:attribute name="type">
                    <xsl:value-of select="'UNICAST2'"/>
                </xsl:attribute>
            </xsl:element>
            <xsl:element name="protocol" namespace="{namespace-uri()}">
                <xsl:attribute name="type">
                    <xsl:value-of select="'pbcast.STABLE'"/>
                </xsl:attribute>
            </xsl:element>
            <xsl:element name="protocol" namespace="{namespace-uri()}">
                <xsl:attribute name="type">
                    <xsl:value-of select="'pbcast.GMS'"/>
                </xsl:attribute>
            </xsl:element>
            <xsl:element name="protocol" namespace="{namespace-uri()}">
                <xsl:attribute name="type">
                    <xsl:value-of select="'UFC'"/>
                </xsl:attribute>
            </xsl:element>
            <xsl:element name="protocol" namespace="{namespace-uri()}">
                <xsl:attribute name="type">
                    <xsl:value-of select="'MFC'"/>
                </xsl:attribute>
            </xsl:element>
            <xsl:element name="protocol" namespace="{namespace-uri()}">
                <xsl:attribute name="type">
                    <xsl:value-of select="'FRAG2'"/>
                </xsl:attribute>
            </xsl:element>
            <xsl:element name="protocol" namespace="{namespace-uri()}">
                <xsl:attribute name="type">
                    <xsl:value-of select="'RSVP'"/>
                </xsl:attribute>
            </xsl:element>
        </xsl:element>
    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
