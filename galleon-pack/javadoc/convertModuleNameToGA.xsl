<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="text" indent="no"/>

    <xsl:param name="moduleName"></xsl:param>

    <!-- Print the groupId. -->
    <xsl:template match="//module-def[@name]">
        <!-- This needs to go inside since we don't have XSLT 2.0. -->
        <xsl:if test="@name = $moduleName">
            <xsl:apply-templates select="maven-resource"/>
        </xsl:if>
    </xsl:template>

    <xsl:template match="//maven-resource">
        <xsl:value-of select="@group"/>:<xsl:value-of select="@artifact"/>
        <xsl:text>
</xsl:text>
    </xsl:template>


    <!-- Skip everything else. -->
    <xsl:template match="@*|node()">
        <xsl:apply-templates select="@*|node()"/>
    </xsl:template>

</xsl:stylesheet>
