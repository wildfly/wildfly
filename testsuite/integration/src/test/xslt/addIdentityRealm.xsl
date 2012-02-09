<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:d="urn:jboss:domain:1.1">
    <xsl:output method="xml" indent="yes"/>

    <xsl:param name="realmName"/>
    <xsl:param name="secret"/>

    <xsl:variable name="newIdentityRealm">
        <d:security-realm>
            <xsl:attribute name="name"><xsl:value-of select="$realmName"/></xsl:attribute>
            <d:server-identities>
                <d:secret>
                    <xsl:attribute name="value"><xsl:value-of select="$secret"/></xsl:attribute>
                </d:secret>
            </d:server-identities>
        </d:security-realm>
    </xsl:variable>

    <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
     
    <!-- Prevent duplicates -->
    <xsl:template match="//d:management/d:security-realms/d:security-realm[@name=$realmName]"/>

    <xsl:template match="//d:management/d:security-realms">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <xsl:copy-of select="$newIdentityRealm"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
