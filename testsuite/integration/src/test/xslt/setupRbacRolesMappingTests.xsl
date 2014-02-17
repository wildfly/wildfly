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

    <xsl:template match="/do:server/do:management/do:access-control/do:role-mapping" priority="100">
        <xsl:copy>
            <role name="Monitor">
                <include>
                    <group name="Monitor"/>
                </include>
                <exclude>
                    <user name="UserMappedToGroupOperatorAndMonitorAndExcludedFromGroupMonitor"/>
                    <user name="UserMappedToGroupMaintainerAndMonitorAndExcludedFromGroupMonitor"/>
                    <user name="UserMappedToGroupDeployerAndMonitorAndExcludedFromGroupMonitor"/>
                    <user name="UserMappedToGroupAdministratorAndMonitorAndExcludedFromGroupMonitor"/>
                    <user name="UserMappedToGroupAuditorAndMonitorAndExcludedFromGroupMonitor"/>
                    <user name="UserMappedToGroupSuperUserAndMonitorAndExcludedFromGroupMonitor"/>
                    <group name="ExcludingGroup"/>
                </exclude>
            </role>
            <role name="Operator">
                <include>
                    <group name="Operator"/>
                </include>
            </role>
            <role name="Maintainer">
                <include>
                    <group name="Maintainer"/>
                </include>
            </role>
            <role name="Deployer">
                <include>
                    <group name="Deployer"/>
                </include>
            </role>
            <role name="Administrator">
                <include>
                    <group name="Administrator"/>
                </include>
            </role>
            <role name="Auditor">
                <include>
                    <group name="Auditor"/>
                </include>
            </role>
            <role name="SuperUser">
                <include>
                    <user name="$local"/>
                    <group name="SuperUser"/>
                </include>
            </role>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/do:domain/do:management/do:access-control/do:role-mapping" priority="100">
        <xsl:copy>
            <role name="Monitor">
                <include>
                    <group name="Monitor"/>
                </include>
                <exclude>
                    <user name="UserMappedToGroupOperatorAndMonitorAndExcludedFromGroupMonitor"/>
                    <user name="UserMappedToGroupMaintainerAndMonitorAndExcludedFromGroupMonitor"/>
                    <user name="UserMappedToGroupDeployerAndMonitorAndExcludedFromGroupMonitor"/>
                    <user name="UserMappedToGroupAdministratorAndMonitorAndExcludedFromGroupMonitor"/>
                    <user name="UserMappedToGroupAuditorAndMonitorAndExcludedFromGroupMonitor"/>
                    <user name="UserMappedToGroupSuperUserAndMonitorAndExcludedFromGroupMonitor"/>
                    <group name="ExcludingGroup"/>
                </exclude>
            </role>
            <role name="Operator">
                <include>
                    <group name="Operator"/>
                </include>
            </role>
            <role name="Maintainer">
                <include>
                    <group name="Maintainer"/>
                </include>
            </role>
            <role name="Deployer">
                <include>
                    <group name="Deployer"/>
                </include>
            </role>
            <role name="Administrator">
                <include>
                    <group name="Administrator"/>
                </include>
            </role>
            <role name="Auditor">
                <include>
                    <group name="Auditor"/>
                </include>
            </role>
            <role name="SuperUser">
                <include>
                    <user name="$local"/>
                    <group name="SuperUser"/>
                </include>
            </role>
        </xsl:copy>
    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
