<?xml version="1.0" ?>
<xsl:stylesheet version="1.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:jg="urn:jboss:domain:jgroups:2.0">

    <!--
        XSLT stylesheet to add an x-site relay protocol element to an existing JGroups stack.

        Upon execution, this:
        <stack name="..">
          <transport type=".." .../>
          ..
          <protocol type=".." .../>
        </stack>

        becomes:
        <stack name="..">
          <transport type=".." .../>
          ..
          <protocol type=".." .../>
          <relay site="..">
            <remote-site name=".." stack=".." cluster-name=".."/>
          </relay>
        </stack>

      -->

  <xsl:param name="stack" select="'udp'"/>
  <xsl:param name="relay.site" select="'siteA'"/>
  <xsl:param name="remote-site.site" select="'siteB'"/>
  <xsl:param name="remote-site.stack" select="'tcp'"/>
  <xsl:param name="remote-site.cluster" select="'bridge'"/>

  <xsl:output method="xml" indent="yes"/>

  <!-- relay protocol layer to be added -->
  <xsl:variable name="relay-protocol">
    <jg:relay site="{$relay.site}">
      <jg:remote-site name="{$remote-site.site}" stack="{$remote-site.stack}" cluster="{$remote-site.cluster}"/>
    </jg:relay>
  </xsl:variable>

  <xsl:template name="copy-attributes">
    <xsl:for-each select="@*">
      <xsl:copy/>
    </xsl:for-each>
  </xsl:template>

  <!-- copy the stack and add a relay protocol -->
  <xsl:template match="jg:subsystem/jg:stack[@name=$stack]">
    <xsl:copy>
      <xsl:call-template name="copy-attributes"/>
      <xsl:copy-of select="child::*"/>
      <xsl:copy-of select="$relay-protocol"/>
    </xsl:copy>
  </xsl:template>

  <!-- copy everything else -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
