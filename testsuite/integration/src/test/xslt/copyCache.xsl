<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <!--
      XSLT stylesheet to add make a copy of an existing cache such that:
      - the children of the cache are copied over unchanged.
      - new caches are added at the end

      Upon execution, this:
      <cache-container name="A">
        <local-cache name="B" ..."/>
        <local-cache name="C" ..."/>
        <replicated-cache name="E" ..."/>
      </cache-container>

      becomes (when copying B to D):

      <cache-container name="A">
        <local-cache name="B" ..."/>
        <local-cache name="C" ..."/>
        <replicated-cache name="E" ..."/>
        <local-cache name="D" ..."/>
      </cache-container>
    -->

    <xsl:param name="container.name" select="'web'"/>
    <xsl:param name="cache.base" select="'repl'"/>
    <xsl:param name="cache.name" select="'new-cache'"/>
    <xsl:param name="cache-type" select="'replicated-cache'"/>

    <xsl:variable name="infinispanns" select="'urn:jboss:domain:infinispan:'"/>

    <xsl:output method="xml" indent="yes"/>

    <xsl:template name="copy-attributes">
        <xsl:for-each select="@*">
            <xsl:copy/>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="copy-cache-attributes-and-override">
        <!-- copy all attributes of the specified cache here -->
        <xsl:copy-of select="*[local-name()=$cache-type and starts-with(namespace-uri(), $infinispanns) and @name=$cache.base]/@*"/>
        <!-- override the ones we need to override -->
        <xsl:attribute name="name">
            <xsl:value-of select="$cache.name"/>
        </xsl:attribute>
    </xsl:template>

    <!-- copy the container -->
    <xsl:template match="//*[local-name() = 'subsystem' and starts-with(namespace-uri(), $infinispanns)]
                          /*[local-name() = 'cache-container' and @name=$container.name]">
        <xsl:copy>
            <xsl:call-template name="copy-attributes"/>
            <xsl:apply-templates select="@*|node()"/>
            <!-- create copy of specified cache element -->
            <xsl:element name="{$cache-type}" namespace="{namespace-uri()}">
                <xsl:call-template name="copy-cache-attributes-and-override"/>
                <xsl:copy-of select="*[local-name()=$cache-type and starts-with(namespace-uri(), $infinispanns) and @name=$cache.base]/child::*"/>
            </xsl:element>
        </xsl:copy>
    </xsl:template>

    <!-- copy everything else -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
