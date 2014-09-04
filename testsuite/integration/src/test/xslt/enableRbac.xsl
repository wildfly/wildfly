<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output indent="yes"/>

    <xsl:variable name="jboss" select="'urn:jboss:domain:'"/>

    <!-- User params. -->
    <xsl:param name="rbac" select="'rbac'"/>
    <xsl:param name="realm" select="'ManagementRealm'"/>
    <xsl:param name="mgmt-user-props" select="'mgmt-users.properties'"/>
    <xsl:param name="mgmt-groups-props" select="'mgmt-groups.properties'"/>
    <xsl:param name="rbac-user-props" select="'rbac-users.properties'"/>
    <xsl:param name="rbac-groups-props" select="'rbac-groups.properties'"/>

    <!-- Changes both domain and standalone access-control provider to rbac -->
    <xsl:template match="//*[local-name()='management' and starts-with(namespace-uri(), $jboss)]
                          /*[local-name()='access-control']" priority="100">
        <xsl:copy>
            <xsl:attribute name="provider">
                <xsl:value-of select="$rbac"/>
            </xsl:attribute>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
