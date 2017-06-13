<?xml version="1.0"?>
<!--
  ~ JBoss, Home of Professional Open Source.
  ~ Copyright 2017, Red Hat, Inc., and individual contributors
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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:variable name="var1">val1</xsl:variable>
  <xsl:param name="param1" />

  <!-- Assert that a built-in function can be loaded -->
  <xsl:template match="//txt">
    <xsl:element name="txt">
      <xsl:value-of select="normalize-space(.)" />
    </xsl:element>
  </xsl:template>

  <!-- Assert that a JRE Java method can be loaded -->
  <xsl:template match="//min" xmlns:Math="http://xml.apache.org/xalan/java/java.lang.Math" >
    <xsl:element name="min">
      <xsl:value-of select="Math:min(0, 1)"/>
    </xsl:element>
  </xsl:template>

  <!-- Assert that a Java method from a lib in the same module can be loaded -->
  <xsl:template match="//EmbeddedJarMethods" xmlns:EmbeddedJarMethods="http://xml.apache.org/xalan/java/org.jboss.as.test.integration.deployment.classloading.jaxp.EmbeddedJarMethods" >
    <xsl:element name="EmbeddedJarMethods">
      <xsl:value-of select="EmbeddedJarMethods:getString()"/>
    </xsl:element>
  </xsl:template>

  <!-- Assert that a Java method from a dependency module can be loaded -->
  <xsl:template match="//CustomModuleMethods" xmlns:CustomModuleMethods="http://xml.apache.org/xalan/java/org.jboss.as.test.integration.deployment.classloading.jaxp.CustomModuleMethods" >
    <xsl:element name="CustomModuleMethods">
      <xsl:value-of select="CustomModuleMethods:getString()"/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="*">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
