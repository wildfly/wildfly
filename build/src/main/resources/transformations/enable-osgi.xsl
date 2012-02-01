<?xml version="1.0"?>
<xsl:stylesheet version="1.1"
                xmlns:d="urn:jboss:domain:1.1"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="xml" version="1.0" indent="yes" xalan:indent-amount="4"/>

    <!-- copy all other data -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="d:extensions">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
            <xsl:apply-templates select="document('../extensions/osgi.xml')/d:server/d:extensions/d:extension"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="d:profile">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
            <xsl:apply-templates select="document('../extensions/osgi.xml')/d:server/d:profile/node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>