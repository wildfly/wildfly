<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes"/>

    <xsl:variable name="iiopNS" select="'urn:jboss:domain:iiop-openjdk:'"/>
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
            <xsl:attribute name="ssl-socket-binding"><xsl:value-of select="$iiop-ssl"/></xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $iiopNS)]
                          /*[local-name()='security']
                          /@server-requires-ssl">
        <xsl:attribute name="server-requires-ssl">true</xsl:attribute>
    </xsl:template>



</xsl:stylesheet>
