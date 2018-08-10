<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes"/>

    <xsl:param name="domain-name"/>
    <xsl:param name="keystore"/>

    <xsl:variable name="securityNS" select="'urn:jboss:domain:security:'"/>
    <xsl:variable name="iiopNS" select="'urn:jboss:domain:iiop-openjdk:'"/>
    <xsl:variable name="iiop" select="'iiop'"/>
    <xsl:variable name="iiop-ssl" select="'iiop-ssl'"/>

    <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>


    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $securityNS)]
                          /*[local-name()='security-domains' and starts-with(namespace-uri(), $securityNS)]">
        <xsl:choose>
            <xsl:when test="not(//*[local-name()='subsystem' and starts-with(namespace-uri(), $securityNS)]
                                 /*[local-name()='security-domains' and starts-with(namespace-uri(), $securityNS)]
                                 /*[local-name()='security-domain' and @name=$domain-name and starts-with(namespace-uri(), $securityNS)]
                               )">
                <xsl:copy>
                    <xsl:apply-templates select="node()|@*"/>
                    <xsl:element name="security-domain" namespace="{namespace-uri()}">
                        <xsl:attribute name="name"><xsl:value-of select="$domain-name"/></xsl:attribute>
                        <xsl:attribute name="cache-type">default</xsl:attribute>
                        <xsl:element name="jsse" namespace="{namespace-uri()}">
                            <xsl:attribute name="keystore-password">password</xsl:attribute>
                            <xsl:attribute name="keystore-url"><xsl:value-of select="$keystore"/></xsl:attribute>
                            <xsl:attribute name="truststore-password">password</xsl:attribute>
                            <xsl:attribute name="truststore-url"><xsl:value-of select="$keystore"/></xsl:attribute>
                        </xsl:element>
                    </xsl:element>
                </xsl:copy>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy>
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:copy>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
