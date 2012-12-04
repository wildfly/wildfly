<xsl:stylesheet version="2.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns="urn:jboss:domain:1.5"
		xmlns:d="urn:jboss:domain:1.5"
                xmlns:o='urn:jboss:domain:osgi:1.2'> 

 <!-- Copy everything else. -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
    
    <!-- subsystem xmlns="urn:jboss:domain:osgi:1.2" activation="lazy" -->
    <xsl:template match="//d:profile/o:subsystem[@activation='lazy']">
        <xsl:copy>
            <xsl:attribute name="activation">eager</xsl:attribute>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
