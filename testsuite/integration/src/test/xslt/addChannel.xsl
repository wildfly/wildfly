<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes"/>

    <xsl:param name="channel"/>

    <xsl:variable name="jgroupsns" select="'urn:jboss:domain:jgroups:'"/>

    <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <!-- Prevent duplicates -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $jgroupsns)]
   						  /*[local-name()='channels']
   						  /*[local-name()='channel' and @name=$channel]"/>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $jgroupsns)]
   						  /*[local-name()='channels']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>            
            <xsl:element name="channel" namespace="{namespace-uri()}">
                <xsl:attribute name="name"><xsl:value-of select="$channel"/></xsl:attribute>
            </xsl:element>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
