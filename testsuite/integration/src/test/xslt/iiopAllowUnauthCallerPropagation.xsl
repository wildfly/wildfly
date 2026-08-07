<!--
  ~ Copyright The WildFly Authors
  ~ SPDX-License-Identifier: Apache-2.0
  -->

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes"/>

    <xsl:variable name="iiopNS" select="'urn:jboss:domain:iiop-openjdk:'"/>

    <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
   </xsl:template>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $iiopNS)]">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="node()"/>
            <xsl:element name="sas-context" namespace="{namespace-uri()}">
                <xsl:attribute name="caller-propagation-require-auth">false</xsl:attribute>
            </xsl:element>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
