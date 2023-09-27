<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Copyright The WildFly Authors
  ~ SPDX-License-Identifier: Apache-2.0
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
