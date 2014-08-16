<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="text" indent="no"/>

    <!-- Print the groupId. -->
    <xsl:template match="//module-def[@name and not(@slot) ]">
            <xsl:apply-templates select="maven-resource"/>
            <xsl:text>MODULE: </xsl:text><xsl:value-of select="@name"/>
            <xsl:text>
</xsl:text>
    </xsl:template>

    <xsl:template match="//maven-resource">
        <!--<xsl:text>    ARTIFACT: </xsl:text>-->
        <xsl:value-of select="@group"/>:<xsl:value-of select="@artifact"/>
        <xsl:text>
</xsl:text>
    </xsl:template>


    <!-- Skip everything else. -->
    <xsl:template match="@*|node()">
        <xsl:apply-templates select="@*|node()"/>
    </xsl:template>

</xsl:stylesheet>
