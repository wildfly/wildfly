<?xml version="1.0" encoding="UTF-8"?>
<!-- See http://www.w3.org/TR/xslt -->

<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ds="urn:jboss:domain:datasources:2.0">
    <xsl:output method="xml" indent="yes"/>

    <xsl:param name="ds.jdbc.url" select="'wilma'"/>
    <xsl:param name="ds.jdbc.pass" select="'test'"/> 
    <xsl:param name="ds.jdbc.user" select="'test'"/>
    <xsl:param name="ds.jndi.name" select="'java:jboss/datasources/testDS'"/>

    <xsl:param name="ds.jdbc.driver.jar" select="'fred'"/>
    <xsl:param name="ds.jdbc.driver"/>
    <xsl:param name="ds.jdbc.driver.module"/>
    <xsl:param name="ds.jdbc.isModule"/>

    <xsl:variable name="driver">
        <xsl:choose>
            <xsl:when test="$ds.jdbc.isModule = 'true'">
                <xsl:value-of select="translate($ds.jdbc.driver.module,'/','.')"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$ds.jdbc.driver.jar"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <xsl:variable name="newDatasourceDefinition">
            <ds:datasource jndi-name="{$ds.jndi.name}" pool-name="ExamplePool" enabled="true" jta="true"
                       use-java-context="true">
                <ds:connection-url><xsl:value-of select="$ds.jdbc.url"/></ds:connection-url>
                <ds:driver><xsl:value-of select="$driver"/></ds:driver>
                <ds:security>
                    <ds:user-name><xsl:value-of select="$ds.jdbc.user"/></ds:user-name>
                    <ds:password><xsl:value-of select="$ds.jdbc.pass"/></ds:password>
                </ds:security>
            </ds:datasource>
    </xsl:variable>

    <!-- Replace the old datasource with the new. -->
    <xsl:template match="//ds:subsystem/ds:datasources/ds:datasource">
        <!-- http://docs.jboss.org/ironjacamar/userguide/1.0/en-US/html/deployment.html#deployingds_descriptor -->
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
        <xsl:copy-of select="$newDatasourceDefinition"/>
    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>

