<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:osgi="urn:jboss:domain:osgi:1.1" version="1.0">
    <xsl:output method="xml" indent="yes"/>

    <xsl:variable name="jaxrConfiguration">
        <configuration pid="org.jboss.as.jaxr.service.JAXRConfiguration">
            <property name="connection-factory" value="java:jboss/jaxr/ConnectionFactory"/>
            <property name="datasource" value="java:jboss/datasources/ExampleDS"/>
            <property name="drop-on-start" value="true"/>
            <property name="create-on-start" value="true"/>
            <property name="drop-on-stop" value="false"/>
        </configuration>
    </xsl:variable>

    <!-- replace the old definition with the new -->
    <xsl:template match="//osgi:subsystem/osgi:configuration[@pid='org.apache.felix.webconsole.internal.servlet.OsgiManager']">
        <xsl:copy-of select="."/>
        <xsl:copy-of select="$jaxrConfiguration"/>
    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>

