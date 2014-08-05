<?xml version="1.0" ?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!--
      XSLT stylesheet to add an x-site backups element and a backup element to a cache in a cache container.

      Upon execution, this:
      <distributed-cache>
        ...
      </distributed-cache>

      becomes:

      <distributed-cache>
        ...
        <backups>
          <backup site-name=".." failure-policy=".." strategy=".." replication-timeout=".." enabled=".."/>
        </backups>
      </distributed-cache>
    -->

    <xsl:param name="container" select="'web'"/>
    <xsl:param name="cache-type" select="'replicated-cache'"/>
    <xsl:param name="cache" select="'repl'"/>
    <xsl:param name="backup.site" select="'siteB'"/>
    <xsl:param name="backup.failure-policy" select="'WARN'"/>
    <xsl:param name="backup.strategy" select="'SYNC'"/>
    <xsl:param name="backup.replication-timeout" select="'10000'"/>
    <xsl:param name="backup.enabled" select="'true'"/>

    <xsl:variable name="infinispanns" select="'urn:jboss:domain:infinispan:'"/>

    <xsl:output method="xml" indent="yes"/>

    <!-- populate the <backup/> element by input parameters -->
    <xsl:template name="add-backup-element">
        <xsl:element name="backup" namespace="{namespace-uri()}">
            <xsl:attribute name="site">
                <xsl:value-of select="$backup.site"/>
            </xsl:attribute>
            <xsl:attribute name="failure-policy">
                <xsl:value-of select="$backup.failure-policy"/>
            </xsl:attribute>
            <xsl:attribute name="strategy">
                <xsl:value-of select="$backup.strategy"/>
            </xsl:attribute>
            <xsl:attribute name="timeout">
                <xsl:value-of select="$backup.replication-timeout"/>
            </xsl:attribute>
            <xsl:attribute name="enabled">
                <xsl:value-of select="$backup.enabled"/>
            </xsl:attribute>
        </xsl:element>
    </xsl:template>

    <xsl:template name="copy-attributes">
        <xsl:for-each select="@*">
            <xsl:copy/>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="add-backups-element">
        <xsl:if test="count(*[local-name() = 'backups']) = 0">
            <!-- create a new <backups/> element and add the new backup -->
            <xsl:element name="backups" namespace="{namespace-uri()}">
                <xsl:call-template name="add-backup-element"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <!-- copy the cache over and add the backup element -->
    <xsl:template match="//*[local-name() = 'subsystem' and starts-with(namespace-uri(), $infinispanns)]
                          /*[local-name() = 'cache-container' and @name=$container]
                          /*[local-name()=$cache-type and @name=$cache]">
        <xsl:copy>
            <xsl:call-template name="copy-attributes"/>
            <xsl:copy-of select="child::*"/>
            <xsl:call-template name="add-backups-element"/>
        </xsl:copy>
    </xsl:template>

    <!-- copy everything else -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
