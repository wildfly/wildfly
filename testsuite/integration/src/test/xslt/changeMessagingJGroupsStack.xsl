<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ JBoss, Home of Professional Open Source.
  ~ Copyright 2015, Red Hat, Inc., and individual contributors
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
<!-- Author: Radoslav Husar, Version: July 2015 -->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!--
        An XSLT that will change the default stack used by messaging. E.g.:

        <broadcast-group name="bg-group1" jgroups-stack="tcp" connectors="http-connector" jgroups-channel="activemq-cluster"/>
        <discovery-group name="dg-group1" jgroups-stack="tcp" jgroups-channel="activemq-cluster"/>
    -->

    <!-- Namespaces -->
    <xsl:variable name="messagingns" select="'urn:jboss:domain:messaging-activemq:'"/>

    <!-- Parameters -->
    <xsl:param name="stack" select="'tcp'"/>

    <!-- Change broadcast-group -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $messagingns)]
                          /*[local-name()='server']
                          /*[local-name()='broadcast-group']
                          /@jgroups-stack">
        <xsl:attribute name="jgroups-stack">
            <xsl:value-of select="$stack"/>
        </xsl:attribute>
    </xsl:template>

    <!-- Change discovery-group -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $messagingns)]
                          /*[local-name()='server']
                          /*[local-name()='discovery-group']
                          /@jgroups-stack">
        <xsl:attribute name="jgroups-stack">
            <xsl:value-of select="$stack"/>
        </xsl:attribute>
    </xsl:template>

    <!-- Traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
