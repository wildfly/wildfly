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
      Adds an interface at the end of the list of interfaces.

      Example:

      <server>
      ...
          <interfaces>
          ...
	          <interface name="multicast">
	              <inet-address value="127.0.0.1"/>
	          </interface>
	      </interfaces>
	  </server>
    -->

    <!-- Namespaces -->
    <xsl:variable name="domainns" select="'urn:jboss:domain:'"/>

    <!-- Parameters -->
    <xsl:param name="interface" select="'multicast'"/>
    <xsl:param name="inet-address" select="'127.0.0.1'"/>

    <!-- Add the interface with configured inetAddress -->
    <xsl:template match="//*[local-name()='interfaces' and starts-with(namespace-uri(), $domainns)]
                          /*[local-name()='interface'][last()]">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>

        <xsl:element name="interface" namespace="{namespace-uri()}">
            <xsl:attribute name="name">
                <xsl:value-of select="$interface"/>
            </xsl:attribute>
            <xsl:element name="inet-address" namespace="{namespace-uri()}">
                <xsl:attribute name="value">
                    <xsl:value-of select="$inet-address"/>
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
