<?xml version="1.0" encoding="UTF-8"?>
<!-- Edited by XMLSpy® -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:do="urn:jboss:domain:1.1"
                xmlns:ds="urn:jboss:domain:datasources:1.0"
                xmlns="urn:jboss:domain:1.1"
        >
    <xsl:output indent="yes"/>

    <xsl:param name="keystore"/>
    <xsl:param name="encFileDir"/>
    <xsl:template match="/do:server/do:extensions" priority="100">
        <xsl:copy>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
        <xsl:element name="vault" >
            <xsl:element name="vault-option">
                <xsl:attribute name="name">KEYSTORE_URL</xsl:attribute>
                <xsl:attribute name="value"><xsl:value-of select="$keystore"/></xsl:attribute>
            </xsl:element>
            <xsl:element name="vault-option">
                <xsl:attribute name="name">KEYSTORE_PASSWORD</xsl:attribute>
                <xsl:attribute name="value">MASK-5WNXs8oEbrs</xsl:attribute>
            </xsl:element>
            <xsl:element name="vault-option">
                <xsl:attribute name="name">KEYSTORE_ALIAS</xsl:attribute>
                <xsl:attribute name="value">vault</xsl:attribute>
            </xsl:element>
            <xsl:element name="vault-option">
                <xsl:attribute name="name">SALT</xsl:attribute>
                <xsl:attribute name="value">12345678</xsl:attribute>
            </xsl:element>
            <xsl:element name="vault-option">
                <xsl:attribute name="name">ITERATION_COUNT</xsl:attribute>
                <xsl:attribute name="value">50</xsl:attribute>
            </xsl:element>
            <xsl:element name="vault-option">
                <xsl:attribute name="name">ENC_FILE_DIR</xsl:attribute>
                <xsl:attribute name="value"><xsl:value-of select="$encFileDir"/></xsl:attribute>
            </xsl:element>
        </xsl:element>
    </xsl:template>

    <xsl:template match="ds:subsystem/ds:datasources" priority="100" xmlns="urn:jboss:domain:datasources:1.0">
        <xsl:copy>
            <xsl:element name="datasource" >
                <xsl:attribute name="jndi-name">java:jboss/datasources/MaskedDS</xsl:attribute>
                <xsl:attribute name="use-java-context">true</xsl:attribute>
                <xsl:attribute name="enabled">true</xsl:attribute>
                <xsl:attribute name="pool-name">MaskedDS</xsl:attribute>
                <xsl:element name="connection-url">jdbc:h2:mem:masked;DB_CLOSE_DELAY=-1</xsl:element>
                <xsl:element name="driver">h2</xsl:element>
                <xsl:element name="security">
                    <xsl:element name="user-name">sa</xsl:element>
                    <xsl:element name="password">${VAULT::ds_ExampleDS::password::MWNjZWNkZjgtMWI2OC00MTMwLTlmNGItYWI0OTFiY2U4ZThiTElORV9CUkVBS3ZhdWx0}</xsl:element>
                </xsl:element>
            </xsl:element>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()" priority="1">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
