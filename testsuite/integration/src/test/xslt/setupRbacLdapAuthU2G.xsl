<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ JBoss, Home of Professional Open Source.
  ~ Copyright 2013, Red Hat, Inc., and individual contributors
  ~ as indicated by the @author tags. See the copyright.txt file in the
  ~ distribution for a full listing of individual contributors.
  ~
  ~ This is free software; you can redistribute it and/or modify it
  ~ under the terms of the GNU Lesser General Public License as
  ~ published by the Free Software Foundation; either version 2.1 of
  ~ the License, or (at your option) any later version.
  ~
  ~ This software is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  ~ Lesser General Public License for more details.
  ~
  ~ You should have received a copy of the GNU Lesser General Public
  ~ License along with this software; if not, write to the Free
  ~ Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  ~ 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:do="urn:jboss:domain:3.0"
                xmlns="urn:jboss:domain:3.0"
        >
    <xsl:output indent="yes"/>

    <xsl:variable name="datasources" select="'urn:jboss:domain:datasources:'"/>

    <xsl:template match="/do:server/do:management">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <outbound-connections>
                <ldap name="ldap" url="ldap://localhost:10389"/>
            </outbound-connections>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/do:server/do:management/do:security-realms/do:security-realm[@name='ManagementRealm']">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <authentication>
                <local default-user="UserMappedToGroupSuperUser"/> <!-- local user must authorize against LDAP -->
                <ldap connection="ldap" base-dn="ou=Users,dc=wildfly,dc=org" user-dn="dn">
                    <username-filter attribute="uid"/>
                </ldap>
            </authentication>
            <authorization map-groups-to-roles="false">
                <ldap connection="ldap">
                    <username-to-dn force="false"> <!-- needed for local user -->
                        <username-filter base-dn="ou=Users,dc=wildfly,dc=org" user-dn-attribute="dn" attribute="uid" />
                    </username-to-dn>
                    <group-search group-name="SIMPLE" group-dn-attribute="dn" group-name-attribute="cn">
                        <principal-to-group group-attribute="seeAlso"/> <!-- seeAlso just exists in the default schema -->
                    </group-search>
                </ldap>
            </authorization>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/do:domain/do:management">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <outbound-connections>
                <ldap name="ldap" url="ldap://localhost:10389"/>
            </outbound-connections>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/do:domain/do:management/do:security-realms/do:security-realm[@name='ManagementRealm']">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <authentication>
                <local default-user="UserMappedToGroupSuperUser"/> <!-- local user must authorize against LDAP -->
                <ldap connection="ldap" base-dn="ou=Users,dc=wildfly,dc=org" user-dn="dn">
                    <username-filter attribute="uid"/>
                </ldap>
            </authentication>
            <authorization map-groups-to-roles="false">
                <ldap connection="ldap">
                    <username-to-dn force="false"> <!-- needed for local user -->
                        <username-filter base-dn="ou=Users,dc=wildfly,dc=org" user-dn-attribute="dn" attribute="uid" />
                    </username-to-dn>
                    <group-search group-name="SIMPLE" group-dn-attribute="dn" group-name-attribute="cn">
                        <principal-to-group group-attribute="seeAlso"/> <!-- seeAlso just exists in the default schema -->
                    </group-search>
                </ldap>
            </authorization>
        </xsl:copy>
    </xsl:template>


    <!-- Copy everything else. -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
