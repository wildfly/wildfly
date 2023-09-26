<!--
  ~ Copyright The WildFly Authors
  ~ SPDX-License-Identifier: Apache-2.0
  -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xslt">
    <xsl:output method="xml" indent="yes" xalan:indent-amount="2" />
    <xsl:param name="fileList"/>
    <xsl:param name="fileSeparator"/>

    <xsl:template match="/">
        <xsl:text>&#10;</xsl:text>
        <xsl:element name="licenseSummary">
            <xsl:element name="dependencies" >
                <xsl:call-template name="tokenizeString">
                    <xsl:with-param name="list" select="$fileList"/>
                    <xsl:with-param name="delimiter" select="','"/>
                </xsl:call-template>
            </xsl:element>
        </xsl:element>
    </xsl:template>

    <xsl:template name="tokenizeString">
        <!--passed template parameter -->
        <xsl:param name="list"/>
        <xsl:param name="delimiter"/>
        <xsl:choose>
            <xsl:when test="contains($list, $delimiter)">
                <xsl:call-template name="processFile">
                    <!-- get everything in front of the first delimiter -->
                    <xsl:with-param name="fileName" select="substring-before($list,$delimiter)"/>
                </xsl:call-template>
                <xsl:call-template name="tokenizeString">
                    <!-- store anything left in another variable -->
                    <xsl:with-param name="list" select="substring-after($list,$delimiter)"/>
                    <xsl:with-param name="delimiter" select="$delimiter"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when test="$list = ''">
                        <xsl:text/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:call-template name="processFile">
                            <!-- get everything in front of the first delimiter -->
                            <xsl:with-param name="fileName" select="$list"/>
                        </xsl:call-template>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="substring-after-last">
        <xsl:param name="string" />
        <xsl:param name="delimiter" />
        <xsl:choose>
            <xsl:when test="contains($string, $delimiter)">
                <xsl:call-template name="substring-after-last">
                    <xsl:with-param name="string" select="substring-after($string, $delimiter)" />
                    <xsl:with-param name="delimiter" select="$delimiter" />
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise><xsl:value-of select="$string" /></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- FILE PROCESSING -->

    <xsl:template name="processFile">
        <xsl:param name="fileName"/>
        <xsl:for-each select="document($fileName)/licenseSummary/dependencies/*">
            <xsl:copy>
                <xsl:apply-templates select="@* | node()"/>
                <xsl:element name="source">
                    <xsl:call-template name="substring-after-last">
                        <xsl:with-param name="string" select="$fileName"/>
                        <xsl:with-param name="delimiter" select="$fileSeparator"/>
                    </xsl:call-template>
                </xsl:element>
            </xsl:copy>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="text()">
        <xsl:value-of select="normalize-space(.)"/>
    </xsl:template>

</xsl:stylesheet>
