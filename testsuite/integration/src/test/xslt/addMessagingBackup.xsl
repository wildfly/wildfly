<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:messaging="urn:jboss:domain:messaging:1.3">

<xsl:output method="xml" indent="yes"/>
    <xsl:template match="//messaging:subsystem/messaging:hornetq-server">
        <xsl:copy>
            <xsl:element name="backup">true</xsl:element>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
