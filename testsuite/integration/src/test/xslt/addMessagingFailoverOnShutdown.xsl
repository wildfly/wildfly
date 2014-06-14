<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                
    <xsl:variable name="messaging" select="'urn:jboss:domain:messaging:'"/>

	<xsl:output method="xml" indent="yes"/>
    
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $messaging)]
   						  /*[local-name()='hornetq-server']">
        <xsl:copy>
            <xsl:element name="failover-on-shutdown" namespace="{namespace-uri()}">true</xsl:element>
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
