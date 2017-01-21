<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes"/>

    <xsl:param name="domainName"/>
    <xsl:param name="keystore"/>
 
    <xsl:variable name="securityNS" select="'urn:jboss:domain:security:'"/>
    <xsl:variable name="jacorbNS" select="'urn:jboss:domain:iiop-openjdk:'"/>
    <xsl:variable name="iiop-ssl" select="'iiop-ssl'"/>

    <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $jacorbNS)]
                                          /*[local-name()='orb']">
        <xsl:copy>
            <xsl:attribute name="socket-binding"><xsl:value-of select="@socket-binding"/></xsl:attribute>
            <xsl:attribute name="ssl-socket-binding"><xsl:value-of select="$iiop-ssl"/></xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <!-- Enable SSL support in iiop configuration -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $jacorbNS)]
                                          /*[local-name()='security']">
        <xsl:copy>
            <xsl:attribute name="support-ssl">true</xsl:attribute>
            <xsl:attribute name="security-domain"><xsl:value-of select="$domainName"/></xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <!-- Create a security domain with keystore used by Jacorb -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $securityNS)]
                          /*[local-name()='security-domains' and starts-with(namespace-uri(), $securityNS)]">
        <xsl:choose>
            <xsl:when test="not(//*[local-name()='subsystem' and starts-with(namespace-uri(), $securityNS)]
                                 /*[local-name()='security-domains' and starts-with(namespace-uri(), $securityNS)]
                                 /*[local-name()='security-domain' and @name=$domainName and starts-with(namespace-uri(), $securityNS)]
                               )">
                <xsl:copy>
                    <xsl:apply-templates select="node()|@*"/>
                    <xsl:element name="security-domain" namespace="{namespace-uri()}">
                        <xsl:attribute name="name"><xsl:value-of select="$domainName"/></xsl:attribute>
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
