<!--
  ~ Copyright The WildFly Authors
  ~ SPDX-License-Identifier: Apache-2.0
  -->

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes"/>

    <xsl:variable name="elytronNS" select="'urn:wildfly:elytron:'"/>
    <xsl:variable name="iiopNS" select="'urn:jboss:domain:iiop-openjdk:'"/>

    <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $elytronNS)]">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:attribute name="default-authentication-context">default-context</xsl:attribute>
            <xsl:element name="authentication-client" namespace="{namespace-uri()}">
                <xsl:element name="authentication-configuration" namespace="{namespace-uri()}">
                    <xsl:attribute name="name">iiop-auth-config</xsl:attribute>
                    <xsl:attribute name="authentication-name">user1</xsl:attribute>
                    <xsl:element name="credential-reference" namespace="{namespace-uri()}">
                        <xsl:attribute name="clear-text">password1</xsl:attribute>
                    </xsl:element>
                </xsl:element>
                <xsl:element name="authentication-context" namespace="{namespace-uri()}">
                    <xsl:attribute name="name">default-context</xsl:attribute>
                    <xsl:element name="match-rule" namespace="{namespace-uri()}">
                        <xsl:attribute name="match-protocol">iiop</xsl:attribute>
                        <xsl:attribute name="authentication-configuration">iiop-auth-config</xsl:attribute>
                    </xsl:element>
                    <xsl:element name="match-rule" namespace="{namespace-uri()}">
                        <xsl:attribute name="match-protocol">ssliop</xsl:attribute>
                        <xsl:attribute name="authentication-configuration">iiop-auth-config</xsl:attribute>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
