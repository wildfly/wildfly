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

    <xsl:template match="//j:subsystem">
        <xsl:copy>
            <j:orb>
                <initializers transactions="on" security="on"/>
            </j:orb>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//t:subsystem">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <t:jts/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
