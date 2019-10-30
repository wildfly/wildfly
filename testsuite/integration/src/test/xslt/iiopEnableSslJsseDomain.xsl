<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes"/>

    <xsl:param name="domain-name"/>

    <xsl:variable name="iiopNS" select="'urn:jboss:domain:iiop-openjdk:'"/>
    <xsl:variable name="iiop" select="'iiop'"/>
    <xsl:variable name="iiop-ssl" select="'iiop-ssl'"/>

    <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $iiopNS)]
                                          /*[local-name()='orb']">
        <xsl:copy>
            <xsl:attribute name="socket-binding"><xsl:value-of select="$iiop"/></xsl:attribute>
            <xsl:attribute name="ssl-socket-binding"><xsl:value-of select="$iiop-ssl"/></xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $iiopNS)]
                                          /*[local-name()='security']">
        <xsl:copy>
            <xsl:attribute name="support-ssl">true</xsl:attribute>
            <xsl:attribute name="server-requires-ssl">false</xsl:attribute>
            <xsl:attribute name="security-domain"><xsl:value-of select="$domain-name"/></xsl:attribute>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
