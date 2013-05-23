<?xml version="1.0" ?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ispn="urn:jboss:domain:infinispan:2.0">

    <!--
      XSLT stylesheet to add an x-site backup element to a cache in a cache container.
      The <backups/> element is created if it does not exist.

      Upon execution, this:
      <backups>
        <backup site-name=".." failure-policy=".." strategy=".." replication-timeout=".." enabled=".."/>
      </backups>

      becomes:

      <backups>
        <backup site-name=".." failure-policy=".." strategy=".." replication-timeout=".." enabled=".."/>
        <backup site-name=".." failure-policy=".." strategy=".." replication-timeout=".." enabled=".."/>
      </backups>
    -->

    <xsl:param name="container" select="'web'"/>
    <xsl:param name="cache-type" select="'replicated-cache'"/>
    <xsl:param name="cache" select="'repl'"/>
    <xsl:param name="backup.site" select="'siteB'"/>
    <xsl:param name="backup.failure-policy" select="'WARN'"/>
    <xsl:param name="backup.strategy" select="'SYNC'"/>
    <xsl:param name="backup.replication-timeout" select="'10000'"/>
    <xsl:param name="backup.enabled" select="'true'"/>

    <xsl:output method="xml" indent="yes"/>

    <xsl:variable name="ispnns">urn:jboss:domain:infinispan:2.0</xsl:variable>

    <!-- populate the <backup/> element by input parameters -->
    <xsl:variable name="new-backup-element">
        <backup site="{$backup.site}" failure-policy="{$backup.failure-policy}" strategy="{$backup.strategy}"
                timeout="{$backup.replication-timeout}" enabled="{$backup.enabled}"/>
    </xsl:variable>

    <xsl:template name="copy-attributes">
        <xsl:for-each select="@*">
            <xsl:copy/>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="add-backup-element">
        <xsl:choose>
            <xsl:when test="count(backups)">
                <!-- copy the existing backups over -->
                <xsl:copy-of select="backups/backup"/>
                <xsl:copy-of select="$new-backup-element"/>
            </xsl:when>
            <xsl:otherwise>
                <!-- create a new <backups/> element and add the new backup -->
                <backups>
                    <xsl:copy-of select="$new-backup-element"/>
                </backups>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- copy the cache over and add the backup element -->
    <xsl:template match="ispn:cache-container[@name=$container]/*[local-name()=$cache-type and @name=$cache]">
        <xsl:copy>
            <xsl:call-template name="copy-attributes"/>
            <xsl:copy-of select="child::*"/>
            <xsl:call-template name="add-backup-element"/>
        </xsl:copy>
    </xsl:template>

    <!-- copy everything else -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
