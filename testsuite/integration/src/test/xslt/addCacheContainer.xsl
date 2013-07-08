<?xml version="1.0" encoding="UTF-8"?>
<!-- See http://www.w3.org/TR/xslt -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:param name="cacheType" select="replicated" />
    <xsl:param name="cacheLoader" select="string-keyed" />
    <xsl:param name="mode" select="SYNC" />

    <xsl:variable name="ispn" select="'urn:jboss:domain:infinispan:'"/>
    
    <xsl:attribute-set name="cacheLoaderAttributes">
        <xsl:attribute name="datasource">java:jboss/datasources/ExampleDS</xsl:attribute>
        <xsl:attribute name="preload">true</xsl:attribute>
        <xsl:attribute name="passivation">false</xsl:attribute>
        <xsl:attribute name="purge">false</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="cacheAttributes">
        <xsl:attribute name="name">jdbc-cache</xsl:attribute>
        <xsl:attribute name="mode"><xsl:value-of select="$mode"/></xsl:attribute>
        <xsl:attribute name="batching">true</xsl:attribute>
    </xsl:attribute-set>

    <xsl:variable name="newCacheDefinition">
        <cache-container name="testDbPersistence" default-cache="jdbc-cache" module="org.wildfly.clustering.web.infinispan">
            <transport lock-timeout="60000" />
            <xsl:element name="{$cacheType}-cache" use-attribute-sets="cacheAttributes">
                <xsl:element name="{$cacheLoader}-jdbc-store" use-attribute-sets="cacheLoaderAttributes">
                    <xsl:choose>
                        
                        <xsl:when test="$cacheLoader = 'string-keyed'">
                            <string-keyed-table prefix="stringbased">
                                <id-column name="id" type="VARCHAR(255)" />
                                <data-column name="datum" type="VARBINARY(10000)" />
                                <timestamp-column name="version" type="BIGINT" />
                            </string-keyed-table>
                        </xsl:when>
                        
                        <xsl:when test="$cacheLoader = 'binary-keyed'">
                            <binary-keyed-table prefix="binarybased">
                                <id-column name="id" type="VARCHAR(255)" />
                                <data-column name="datum" type="VARBINARY(10000)" />
                                <timestamp-column name="version" type="BIGINT" />
                            </binary-keyed-table>
                        </xsl:when>

                        <xsl:when test="$cacheLoader = 'mixed-keyed'">
                            <string-keyed-table prefix="stringbased">
                                <id-column name="id" type="VARCHAR(255)" />
                                <data-column name="datum" type="VARBINARY(10000)" />
                                <timestamp-column name="version" type="BIGINT" />
                            </string-keyed-table>
                            <binary-keyed-table prefix="binarybased">
                                <id-column name="id" type="VARCHAR(255)" />
                                <data-column name="datum" type="VARBINARY(10000)" />
                                <timestamp-column name="version" type="BIGINT" />
                            </binary-keyed-table>
                        </xsl:when>

                    </xsl:choose>
                </xsl:element>
            </xsl:element>
        </cache-container>
    </xsl:variable>
    <!-- replace the old definition with the new -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $ispn)]
    /*[local-name()='cache-container' and starts-with(namespace-uri(), $ispn) and @name='web']">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
        <xsl:copy-of select="$newCacheDefinition" />
    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>

