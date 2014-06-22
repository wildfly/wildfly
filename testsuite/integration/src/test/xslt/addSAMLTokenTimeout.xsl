<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!--
      An XSLT style sheet which will add saml element with tomeout set to long period to enable debugging.
    -->
    
    <xsl:variable name="jboss" select="'urn:jboss:domain:'"/>
    <xsl:variable name="pl-fed" select="'urn:jboss:domain:picketlink-federation:'"/>

    <!-- parameter of this script with default values -->
    <xsl:param name="samlTokenTimeout" select="3600000"/>
    <xsl:param name="samlClockSkew"     select="1000"/>

    <!-- Change all PicketLink Federations defined in standalone-*.xml -->
    <xsl:template
        match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $pl-fed)]
                /*[local-name()='federation']">
                  
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:element name="saml">
                <xsl:attribute name="token-timeout"><xsl:value-of
                    select="$samlTokenTimeout" /></xsl:attribute>
                <xsl:attribute name="clock-skew"><xsl:value-of
                    select="$samlClockSkew" /></xsl:attribute>
            </xsl:element>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
