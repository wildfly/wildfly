<?xml version="1.0" ?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ispn="urn:jboss:domain:infinispan:2.0">

    <!--
      XSLT stylesheet to add an x-site backup-for element to a cache in a cache container.
    -->

    <xsl:param name="container" select="'web'"/>
    <xsl:param name="cache-type" select="'replicated-cache'"/>
    <xsl:param name="cache" select="'repl'"/>
    <xsl:param name="remote-cache" select="users"/>
    <xsl:param name="remote-site" select="'LON'"/>

    <xsl:output method="xml" indent="yes"/>

    <xsl:variable name="ispnns">urn:jboss:domain:infinispan:2.0</xsl:variable>

    <!-- populate the <backup/> element by input parameters -->
    <xsl:variable name="new-backup-element">
        <backup-for remote-site="{$remote-site}" remote-cache="{$remote-cache}"/>
    </xsl:variable>

    <xsl:template name="copy-attributes">
        <xsl:for-each select="@*">
            <xsl:copy/>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="add-backup-for-element">
        <xsl:copy-of select="$new-backup-element"/>
    </xsl:template>

    <!-- copy the cache over and add the backup element -->
    <xsl:template match="ispn:cache-container[@name=$container]/*[local-name()=$cache-type and @name=$cache]">
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
