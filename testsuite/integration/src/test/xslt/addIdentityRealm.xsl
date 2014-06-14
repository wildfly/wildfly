<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes"/>

    <xsl:param name="realmName"/>
    <xsl:param name="secret"/>
    
    <xsl:variable name="jboss" select="'urn:jboss:domain:'"/>

    <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <!-- Prevent duplicates -->
    <xsl:template match="//*[local-name()='management' and starts-with(namespace-uri(), $jboss)]
   						  /*[local-name()='security-realms']
   						  /*[local-name()='security-realm' and @name=$realmName]"/>

    <xsl:template match="//*[local-name()='management' and starts-with(namespace-uri(), $jboss)]
   						  /*[local-name()='security-realms']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>            
            <xsl:element name="security-realm" namespace="{namespace-uri()}">
                <xsl:attribute name="name"><xsl:value-of select="$realmName"/></xsl:attribute>
                <xsl:element name="server-identities"  namespace="{namespace-uri()}">
                    <xsl:element name="secret" namespace="{namespace-uri()}">
                        <xsl:attribute name="value"><xsl:value-of select="$secret"/></xsl:attribute>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
