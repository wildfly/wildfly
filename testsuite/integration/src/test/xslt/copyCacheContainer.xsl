<?xml version="1.0" ?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!--
      XSLT stylesheet to add make a copy of an existing cache container such that:
      - the children of the cache container are copied over unchanged.
      - new cache-containers are added at the end

      Upon execution, this:
      <cache-container name="A">
        <local-cache name=".." ..."/>
      </cache-container>
      <cache-container name="B">
        <local-cache name=".." ..."/>
        <replicated-cache name=".." ..."/>
      </cache-container>

      becomes (when copying A to D):

      <cache-container name="A">
        <local-cache name=".." ..."/>
      </cache-container>
      <cache-container name="B">
        <local-cache name=".." ..."/>
        <replicated-cache name=".." ..."/>
      </cache-container>
      <cache-container name="D">
        <local-cache name=".." ..."/>
      </cache-container>

    -->

    <xsl:param name="container.name" select="'new-container'"/>
    <xsl:param name="container.base" select="'web'"/>
    <xsl:param name="container.default-cache" select="'default-cache'"/>

    <xsl:variable name="infinispanns" select="'urn:jboss:domain:infinispan:'"/>

    <xsl:output method="xml" indent="yes"/>

    <xsl:template name="copy-attributes">
        <xsl:for-each select="@*">
            <xsl:copy/>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="copy-container-attributes-and-override">
        <!-- copy all attributes of cache-container here -->
        <xsl:copy-of select="*[local-name() = 'cache-container' and starts-with(namespace-uri(), $infinispanns) and @name=$container.base]/@*"/>
        <!-- override the ones we need to override -->
        <xsl:attribute name="name">
            <xsl:value-of select="$container.name"/>
        </xsl:attribute>
        <xsl:attribute name="default-cache">
            <xsl:value-of select="$container.default-cache"/>
        </xsl:attribute>
    </xsl:template>

    <!-- copy the subsystem -->
    <xsl:template match="//*[local-name() = 'subsystem' and starts-with(namespace-uri(), $infinispanns)]">
        <xsl:copy>
            <xsl:call-template name="copy-attributes"/>
            <xsl:apply-templates select="@*|node()"/>
            <!-- create copy of specified stack element -->
            <xsl:element name="cache-container" namespace="{namespace-uri()}">
                <xsl:call-template name="copy-container-attributes-and-override"/>
                <xsl:copy-of select="*[local-name() = 'cache-container' and starts-with(namespace-uri(), $infinispanns) and @name=$container.base]/child::*"/>
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
