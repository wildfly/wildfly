<?xml version="1.0" encoding="UTF-8"?>
<!--
  JBoss, Home of Professional Open Source.
  Copyright 2012, Red Hat, Inc., and individual contributors
  as indicated by the @author tags. See the copyright.txt file in the
  distribution for a full listing of individual contributors.

  This is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as
  published by the Free Software Foundation; either version 2.1 of
  the License, or (at your option) any later version.

  This software is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this software; if not, write to the Free
  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  02110-1301 USA, or see the FSF site: http://www.fsf.org.

  Author: David Bosschaert
 -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:variable name="configadmin" select="'urn:jboss:domain:configadmin:'"/>

    <xsl:output indent="yes"/>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $configadmin)]" >
        <xsl:copy>
            <xsl:apply-templates select="@* | *"/>
            <xsl:element name="configuration" namespace="{namespace-uri()}">
                <xsl:attribute name="pid">a.test.pid</xsl:attribute>
                <xsl:element name="property" namespace="{namespace-uri()}">
                    <xsl:attribute name="name">testkey</xsl:attribute>
                    <xsl:attribute name="value">test value</xsl:attribute>
                </xsl:element>
                <xsl:element name="property" namespace="{namespace-uri()}">
                    <xsl:attribute name="name">test.key.2</xsl:attribute>
                    <xsl:attribute name="value">nothing</xsl:attribute>
                </xsl:element>
            </xsl:element>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
