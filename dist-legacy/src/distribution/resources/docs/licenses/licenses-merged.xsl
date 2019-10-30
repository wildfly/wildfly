<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="/">
        <licenseSummary>
            <dependencies>
                <xsl:for-each select="list/item">
                    <xsl:variable name="name" select="text()"/>
                    <xsl:for-each select="document($name)/licenseSummary/dependencies/*">
                        <xsl:text disable-output-escaping="no" />
                        <xsl:copy>
                            <xsl:apply-templates select="@* | node()"/>
                            <xsl:text disable-output-escaping="no"/>
                            <source>
                                <xsl:value-of select="$name"/>
                            </source>
                            <xsl:text disable-output-escaping="no"/>
                        </xsl:copy>
                    </xsl:for-each>
                </xsl:for-each>
                <!--
                <full-feature-pack-licenses.xml>
                    <xsl:copy>
                        <xsl:apply-templates select="document('full-feature-pack-licenses.xml')/licenseSummary/dependencies/*"/>
                    </xsl:copy>
                </full-feature-pack-licenses.xml>
                <migration-feature-pack-licenses>
                    <xsl:copy>
                        <xsl:apply-templates select="document('migration-feature-pack-licenses.xml')/licenseSummary/dependencies/*"/>
                    </xsl:copy>
                </migration-feature-pack-licenses>
                <servlet-feature-pack-licenses.xml>
                    <xsl:copy>
                        <xsl:apply-templates select="document('servlet-feature-pack-licenses.xml')/licenseSummary/dependencies/*"/>
                    </xsl:copy>
                </servlet-feature-pack-licenses.xml>
                -->
            </dependencies>
        </licenseSummary>
    </xsl:template>
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
