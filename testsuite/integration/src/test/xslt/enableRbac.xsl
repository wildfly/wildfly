<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:do="urn:jboss:domain:3.0"
                xmlns="urn:jboss:domain:3.0"
        >
    <xsl:output indent="yes"/>

    <!-- User params. -->
    <xsl:param name="rbac" select="'rbac'"/>
    <xsl:param name="realm" select="'ManagementRealm'"/>
    <xsl:param name="mgmt-user-props" select="'mgmt-users.properties'"/>
    <xsl:param name="mgmt-groups-props" select="'mgmt-groups.properties'"/>
    <xsl:param name="rbac-user-props" select="'rbac-users.properties'"/>
    <xsl:param name="rbac-groups-props" select="'rbac-groups.properties'"/>

    <!-- Change the standalone access-control provider to rbac -->
    <xsl:template match="/do:server/do:management/do:access-control" priority="100">
        <xsl:copy>
            <xsl:attribute name="provider">
                <xsl:value-of select="$rbac"/>
            </xsl:attribute>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Change the domain access-control provider to rbac -->
    <xsl:template match="/do:domain/do:management/do:access-control" priority="100">
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
