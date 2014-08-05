<?xml version="1.0" ?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!--
      XSLT stylesheet to add an x-site backup-for element to a cache in a cache container.
    -->

    <xsl:param name="container" select="'web'"/>
    <xsl:param name="cache-type" select="'replicated-cache'"/>
    <xsl:param name="cache" select="'repl'"/>
    <xsl:param name="remote-cache" select="users"/>
    <xsl:param name="remote-site" select="'LON'"/>

    <xsl:output method="xml" indent="yes"/>

    <xsl:variable name="infinispanns" select="'urn:jboss:domain:infinispan:'"/>

    <xsl:template name="copy-attributes">
        <xsl:for-each select="@*">
            <xsl:copy/>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="add-backup-for-element">
        <xsl:element name="backup-for" namespace="{namespace-uri()}">
            <xsl:attribute name="remote-site">
                <xsl:value-of select="$remote-site"/>
            </xsl:attribute>
            <xsl:attribute name="remote-cache">
                <xsl:value-of select="$remote-cache"/>
            </xsl:attribute>
        </xsl:element>
    </xsl:template>

    <!-- copy the cache over and add the backup element -->
    <xsl:template match="//*[local-name() = 'subsystem' and starts-with(namespace-uri(), $infinispanns)]
                          /*[local-name() = 'cache-container' and @name=$container]
                          /*[local-name()=$cache-type and @name=$cache]">
        <xsl:copy>
            <xsl:call-template name="copy-attributes"/>
            <xsl:copy-of select="child::*"/>
            <xsl:call-template name="add-backup-for-element"/>
        </xsl:copy>
    </xsl:template>

    <!-- copy everything else -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
