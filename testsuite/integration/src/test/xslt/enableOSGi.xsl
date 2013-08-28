<xsl:stylesheet version="2.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                
    <xsl:variable name="jboss" select="'urn:jboss:domain:'"/>
    <xsl:variable name="osgi" select="'urn:jboss:domain:osgi:'"/> 
    
    <!-- Copy everything else. -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
    
    <!-- subsystem xmlns="urn:jboss:domain:osgi:1.2" activation="lazy" -->
    <xsl:template match="//*[local-name()='profile' and starts-with(namespace-uri(), $jboss)]
    					  /*[local-name()='subsystem' and starts-with(namespace-uri(), $osgi) and @activation='lazy']">
        <xsl:copy>
            <xsl:attribute name="activation">eager</xsl:attribute>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
