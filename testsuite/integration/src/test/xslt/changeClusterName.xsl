<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ JBoss, Home of Professional Open Source.
  ~ Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
<!-- Author: Radoslav Husar -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!--
        An XSL transformation that will change the cluster name of the default (ee) channel.

        <subsystem xmlns="urn:jboss:domain:jgroups:5.0">
            <channels default="ee">
                <channel name="ee" stack="tcp" cluster="ejb-forwarder">
                ...
    -->

    <!-- Namespaces -->
    <xsl:variable name="jgroupsns" select="'urn:jboss:domain:jgroups:'"/>

    <!-- Parameters -->
    <xsl:param name="cluster" select="'ejb-forwarder'"/>

    <!-- Change cluster attribute of the channel -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $jgroupsns)]
                          /*[local-name()='channels']
                          /*[local-name()='channel']
                          /@cluster">
        <xsl:attribute name="cluster">
            <xsl:value-of select="$cluster"/>
        </xsl:attribute>
    </xsl:template>

    <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
