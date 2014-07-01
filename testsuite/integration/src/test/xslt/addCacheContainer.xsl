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
    </xsl:attribute-set>

    <!-- replace the old definition with the new -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $ispn)]">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
            <xsl:element name="cache-container" namespace="{namespace-uri()}">
                <xsl:attribute name="name">testDbPersistence</xsl:attribute>
                <xsl:attribute name="default-cache">jdbc-cache</xsl:attribute>
                <xsl:attribute name="module">org.wildfly.clustering.web.infinispan</xsl:attribute>

                <xsl:element name="transport" namespace="{namespace-uri()}">
                    <xsl:attribute name="lock-timeout">6000</xsl:attribute>
                </xsl:element>
                <xsl:element name="{$cacheType}-cache" use-attribute-sets="cacheAttributes" namespace="{namespace-uri()}">
                    <xsl:element name="{$cacheLoader}-jdbc-store" use-attribute-sets="cacheLoaderAttributes" namespace="{namespace-uri()}">
                        <xsl:choose>

                            <xsl:when test="$cacheLoader = 'string-keyed'">
                                <xsl:element name="string-keyed-table" namespace="{namespace-uri()}">
                                    <xsl:attribute name="prefix">stringbased</xsl:attribute>
                                    <xsl:element name="id-column" namespace="{namespace-uri()}">
                                        <xsl:attribute name="name">id</xsl:attribute>
                                        <xsl:attribute name="type">VARCHAR(255)</xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="data-column" namespace="{namespace-uri()}">
                                        <xsl:attribute name="name">datum</xsl:attribute>
                                        <xsl:attribute name="type">VARBINARY(10000)</xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="timestamp-column" namespace="{namespace-uri()}">
                                        <xsl:attribute name="name">version</xsl:attribute>
                                        <xsl:attribute name="type">BIGINT</xsl:attribute>
                                    </xsl:element>
                                </xsl:element>
                            </xsl:when>

                            <xsl:when test="$cacheLoader = 'binary-keyed'">
                                <xsl:element name="binary-keyed-table" namespace="{namespace-uri()}">
                                    <xsl:attribute name="prefix">binarybased</xsl:attribute>
                                    <xsl:element name="id-column" namespace="{namespace-uri()}">
                                        <xsl:attribute name="name">id</xsl:attribute>
                                        <xsl:attribute name="type">VARCHAR(255)</xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="data-column" namespace="{namespace-uri()}">
                                        <xsl:attribute name="name">datum</xsl:attribute>
                                        <xsl:attribute name="type">VARBINARY(10000)</xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="timestamp-column" namespace="{namespace-uri()}">
                                        <xsl:attribute name="name">version</xsl:attribute>
                                        <xsl:attribute name="type">BIGINT</xsl:attribute>
                                    </xsl:element>
                                </xsl:element>                                
                            </xsl:when>

                            <xsl:when test="$cacheLoader = 'mixed-keyed'">
                                <xsl:element name="string-keyed-table" namespace="{namespace-uri()}">
                                    <xsl:attribute name="prefix">stringbased</xsl:attribute>
                                    <xsl:element name="id-column" namespace="{namespace-uri()}">
                                        <xsl:attribute name="name">id</xsl:attribute>
                                        <xsl:attribute name="type">VARCHAR(255)</xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="data-column" namespace="{namespace-uri()}">
                                        <xsl:attribute name="name">datum</xsl:attribute>
                                        <xsl:attribute name="type">VARBINARY(10000)</xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="timestamp-column" namespace="{namespace-uri()}">
                                        <xsl:attribute name="name">version</xsl:attribute>
                                        <xsl:attribute name="type">BIGINT</xsl:attribute>
                                    </xsl:element>
                                </xsl:element>
                                <xsl:element name="binary-keyed-table" namespace="{namespace-uri()}">
                                    <xsl:attribute name="prefix">binarybased</xsl:attribute>
                                    <xsl:element name="id-column" namespace="{namespace-uri()}">
                                        <xsl:attribute name="name">id</xsl:attribute>
                                        <xsl:attribute name="type">VARCHAR(255)</xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="data-column" namespace="{namespace-uri()}">
                                        <xsl:attribute name="name">datum</xsl:attribute>
                                        <xsl:attribute name="type">VARBINARY(10000)</xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="timestamp-column" namespace="{namespace-uri()}">
                                        <xsl:attribute name="name">version</xsl:attribute>
                                        <xsl:attribute name="type">BIGINT</xsl:attribute>
                                    </xsl:element>
                                </xsl:element>
                            </xsl:when>

                        </xsl:choose>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
        </xsl:copy>
    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>

