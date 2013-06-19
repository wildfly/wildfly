<?xml version="1.0" ?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:jg="urn:jboss:domain:jgroups:2.0">

    <!--
      XSLT stylesheet to add make a copy of an existing JGroups stack such that:
      - the children of the stack are copied over unchanged.
      - new stacks are added at the end

      Upon execution, this:
      <stack name="A">
        <transport type=".." ..."/>
        <protocol type=".." ..."/>
        ...
      </stack>
      <stack name="B">
        <transport type=".." ..."/>
        <protocol type=".." ..."/>
        ...
      </stack>

      becomes (when copying A to D):

      <stack name="A">
        <transport type=".." ..."/>
        <protocol type=".." ..."/>
        ...
      </stack>
      <stack name="B">
        <transport type=".." ..."/>
        <protocol type=".." ..."/>
        ...
      </stack>
      <stack name="D">
        <transport type=".." ..."/>
        <protocol type=".." ..."/>
        ...
      </stack>

    -->

    <xsl:param name="stack.name" select="'new-stack'"/>
    <xsl:param name="stack.base" select="'udp'"/>

    <xsl:output method="xml" indent="yes"/>
    <xsl:variable name="jgns">
        <xsl:value-of select="'urn:jboss:domain:jgroups:2.0'"/>
    </xsl:variable>

    <xsl:template name="copy-attributes">
        <xsl:for-each select="@*">
            <xsl:copy/>
        </xsl:for-each>
    </xsl:template>

    <!-- copy the subsystem -->
    <xsl:template match="jg:subsystem">
        <xsl:copy>
            <xsl:call-template name="copy-attributes"/>
            <xsl:apply-templates select="@*|node()"/>
            <!-- create copy of specified stack element -->
            <xsl:element name="stack" namespace="{$jgns}">
                <xsl:attribute name="name">
                    <xsl:value-of select="$stack.name"/>
                </xsl:attribute>
                <xsl:copy-of select="jg:stack[@name=$stack.base]/child::*"/>
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
