<?xml version="1.0" ?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

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

    <xsl:variable name="jgroupsns" select="'urn:jboss:domain:jgroups:'"/>

    <xsl:param name="stack" select="'udp'"/>
    <xsl:param name="relay.site" select="'siteA'"/>
    <xsl:param name="remote-site.site" select="'siteB'"/>
    <xsl:param name="remote-site.stack" select="'tcp'"/>
    <xsl:param name="remote-site.cluster" select="'bridge'"/>

    <xsl:output method="xml" indent="yes"/>

    <xsl:template name="copy-attributes">
        <xsl:for-each select="@*">
            <xsl:copy/>
        </xsl:for-each>
    </xsl:template>

    <!-- copy the stack and add a relay protocol -->
    <xsl:template match="//*[local-name() = 'subsystem' and starts-with(namespace-uri(), $jgroupsns)]
                          /*[local-name() = 'stack' and @name=$stack]">
        <xsl:copy>
            <xsl:call-template name="copy-attributes"/>
            <xsl:copy-of select="child::*"/>
            <xsl:element name="relay" namespace="{namespace-uri()}">
                <xsl:attribute name="site">
                    <xsl:value-of select="$relay.site"/>
                </xsl:attribute>
                <xsl:element name="remote-site" namespace="{namespace-uri()}">
                    <xsl:attribute name="name">
                        <xsl:value-of select="$remote-site.site"/>
                    </xsl:attribute>
                    <xsl:attribute name="stack">
                        <xsl:value-of select="$remote-site.stack"/>
                    </xsl:attribute>
                    <xsl:attribute name="cluster">
                        <xsl:value-of select="$remote-site.cluster"/>
                    </xsl:attribute>
                </xsl:element>
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
