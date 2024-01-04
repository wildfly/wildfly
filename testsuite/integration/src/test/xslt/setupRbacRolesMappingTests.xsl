<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Copyright The WildFly Authors
  ~ SPDX-License-Identifier: Apache-2.0
  -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output indent="yes"/>

    <xsl:variable name="jboss" select="'urn:jboss:domain:'"/>

    <xsl:template match="//*[local-name()='management' and starts-with(namespace-uri(), $jboss)]
                          /*[local-name()='access-control']
                          /*[local-name()='role-mapping']" priority="100">
        <xsl:copy>
            <xsl:element name="role" namespace="{namespace-uri()}">
                <xsl:attribute name="name">Monitor</xsl:attribute>
                <xsl:element name="include" namespace="{namespace-uri()}">
                    <xsl:element name="group" namespace="{namespace-uri()}">
                        <xsl:attribute name="name">Monitor</xsl:attribute>
                    </xsl:element>
                </xsl:element>
                <xsl:element name="exclude" namespace="{namespace-uri()}">
                    <xsl:element name="user" namespace="{namespace-uri()}">
                        <xsl:attribute name="name">UserMappedToGroupOperatorAndMonitorAndExcludedFromGroupMonitor</xsl:attribute>
                    </xsl:element>
                    <xsl:element name="user" namespace="{namespace-uri()}">
                        <xsl:attribute name="name">UserMappedToGroupMaintainerAndMonitorAndExcludedFromGroupMonitor</xsl:attribute>
                    </xsl:element>
                    <xsl:element name="user" namespace="{namespace-uri()}">
                        <xsl:attribute name="name">UserMappedToGroupDeployerAndMonitorAndExcludedFromGroupMonitor</xsl:attribute>
                    </xsl:element>
                    <xsl:element name="user" namespace="{namespace-uri()}">
                        <xsl:attribute name="name">UserMappedToGroupAdministratorAndMonitorAndExcludedFromGroupMonitor</xsl:attribute>
                    </xsl:element>
                    <xsl:element name="user" namespace="{namespace-uri()}">
                        <xsl:attribute name="name">UserMappedToGroupAuditorAndMonitorAndExcludedFromGroupMonitor</xsl:attribute>
                    </xsl:element>
                    <xsl:element name="user" namespace="{namespace-uri()}">
                        <xsl:attribute name="name">UserMappedToGroupSuperUserAndMonitorAndExcludedFromGroupMonitor</xsl:attribute>
                    </xsl:element>
                    <xsl:element name="group" namespace="{namespace-uri()}">
                        <xsl:attribute name="name">ExcludingGroup</xsl:attribute>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
            <xsl:element name="role" namespace="{namespace-uri()}">
                <xsl:attribute name="name">Operator</xsl:attribute>
                <xsl:element name="include" namespace="{namespace-uri()}">
                    <xsl:element name="group" namespace="{namespace-uri()}">
                        <xsl:attribute name="name">Operator</xsl:attribute>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
            <xsl:element name="role" namespace="{namespace-uri()}">
                <xsl:attribute name="name">Maintainer</xsl:attribute>
                <xsl:element name="include" namespace="{namespace-uri()}">
                    <xsl:element name="group" namespace="{namespace-uri()}">
                        <xsl:attribute name="name">Maintainer</xsl:attribute>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
            <xsl:element name="role" namespace="{namespace-uri()}">
                <xsl:attribute name="name">Deployer</xsl:attribute>
                <xsl:element name="include" namespace="{namespace-uri()}">
                    <xsl:element name="group" namespace="{namespace-uri()}">
                        <xsl:attribute name="name">Deployer</xsl:attribute>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
            <xsl:element name="role" namespace="{namespace-uri()}">
                <xsl:attribute name="name">Administrator</xsl:attribute>
                <xsl:element name="include" namespace="{namespace-uri()}">
                    <xsl:element name="group" namespace="{namespace-uri()}">
                        <xsl:attribute name="name">Administrator</xsl:attribute>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
            <xsl:element name="role" namespace="{namespace-uri()}">
                <xsl:attribute name="name">Auditor</xsl:attribute>
                <xsl:element name="include" namespace="{namespace-uri()}">
                    <xsl:element name="group" namespace="{namespace-uri()}">
                        <xsl:attribute name="name">Auditor</xsl:attribute>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
            <xsl:element name="role" namespace="{namespace-uri()}">
                <xsl:attribute name="name">SuperUser</xsl:attribute>
                <xsl:element name="include" namespace="{namespace-uri()}">
                    <xsl:element name="user" namespace="{namespace-uri()}">
                        <xsl:attribute name="name">$local</xsl:attribute>
                    </xsl:element>
                    <xsl:element name="group" namespace="{namespace-uri()}">
                        <xsl:attribute name="name">SuperUser</xsl:attribute>
                    </xsl:element>
                </xsl:element>
            </xsl:element>

        </xsl:copy>
    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
