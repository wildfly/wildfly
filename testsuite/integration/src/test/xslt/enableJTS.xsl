<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!--
        An XSLT style sheet which will enable JTS,
        by adding the JTS attribute to the transactions subsystem,
        and turning on transaction propagation in the IIOP subsystem.
    -->
    
    <xsl:variable name="transactions" select="'urn:jboss:domain:transactions:'"/>
    <xsl:variable name="iiop-openjdk" select="'urn:jboss:domain:iiop-openjdk:'"/>
    
    <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $iiop-openjdk)]
    					  /*[local-name()='orb']">
        <xsl:copy>
            <xsl:attribute name="socket-binding"><xsl:value-of select="@socket-binding"/></xsl:attribute>
            <!--<xsl:attribute name="ssl-socket-binding"><xsl:value-of select="@ssl-socket-binding"/></xsl:attribute>-->
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $iiop-openjdk)]
                                          /*[local-name()='initializers']">
        <xsl:copy>
            <xsl:attribute name="transactions">full</xsl:attribute>
            <xsl:attribute name="security">client</xsl:attribute>
        </xsl:copy>
    </xsl:template>



    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $transactions)]">
        <xsl:choose>
            <xsl:when test="not(//*[local-name()='subsystem' and starts-with(namespace-uri(), $transactions)]
            				     /*[local-name()='jts'])">
                <xsl:copy>
                    <xsl:apply-templates select="node()|@*"/>
                    <xsl:element name="jts" namespace="{namespace-uri()}"/>
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
