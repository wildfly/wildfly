<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:t="urn:jboss:domain:transactions:1.1"
                xmlns:j="urn:jboss:domain:jacorb:1.1">

    <!--
        An XSLT style sheet which will enable JTS,
        by adding the JTS attribute to the transactions subsystem,
        and turning on transaction propagation in the JacORB subsystem.
    -->
    <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//j:subsystem/j:orb">
                <xsl:copy>
                        <initializers transactions="on" security="on"/>
                </xsl:copy>
    </xsl:template>

    <xsl:template match="//t:subsystem">
        <xsl:choose>
            <xsl:when test="not(//t:subsystem/t:jts)">
                <xsl:copy>
                    <xsl:apply-templates select="node()|@*"/>
                    <t:jts/>
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
