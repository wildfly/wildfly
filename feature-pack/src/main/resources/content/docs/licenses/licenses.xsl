<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="html" encoding="utf-8" standalone="no" media-type="text/html" />
    <xsl:param name="version"/>
    <xsl:variable name="lowercase" select="'abcdefghijklmnopqrstuvwxyz '" />
    <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ!'" />
    
    <xsl:template match="/">
        <html>
            <head>
                <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
                <link rel="stylesheet" type="text/css" href="licenses.css"/>
            </head>
            <body>
                <h2>WildFly <xsl:value-of select="substring-before($version, '-')"/> - Feature Pack</h2>
                <p>The following material has been provided for informational purposes only, and should not be relied upon or construed as a legal opinion or legal advice.</p>
                <!-- Read matching templates -->
                <table>
                    <tr>
                        <th>Package Group</th>
                        <th>Package Artifact</th>
                        <th>Package Version</th>
                        <th>Remote Licenses</th>
                        <th>Local Licenses</th>
                    </tr>
                    <xsl:for-each select="licenseSummary/dependencies/dependency">
                        <xsl:sort select="concat(groupId, '.', artifactId)"/>
                        <tr>
                            <td><xsl:value-of select="groupId"/></td>
                            <td><xsl:value-of select="artifactId"/></td>
                            <td><xsl:value-of select="version"/></td>
                            <td>
                                <xsl:for-each select="licenses/license">
                                    <xsl:choose>
                                        <xsl:when test="name = 'Public Domain'">
                                            <xsl:value-of select="name"/><br/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <a href="{./url}"><xsl:value-of select="name"/></a><br/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:for-each>
                            </td>
                            <td>
                                <xsl:for-each select="licenses/license">
                                    <xsl:variable name="filename">
                                        <xsl:call-template name="remap-local-filename">
                                            <xsl:with-param name="name" select="name" />
                                        </xsl:call-template>
                                    </xsl:variable>
                                    <a href="{$filename}"><xsl:value-of select="$filename"/></a><br/>
                                </xsl:for-each>
                            </td>
                        </tr>
                    </xsl:for-each>
                </table>
            </body>
        </html>
    </xsl:template>

    <xsl:template name="remap-local-filename">
        <xsl:param name="name"/>
        <xsl:choose>
            <xsl:when test="$name = 'BSD 3-clause &quot;New&quot; or &quot;Revised&quot; License'">
                <xsl:text>bsd 3-clause new or revised license.html</xsl:text>
            </xsl:when>
            <xsl:when test="$name = 'BSD 3-Clause No Nuclear License'">
                <xsl:text>bsd 3-clause no nuclear license.html</xsl:text>
            </xsl:when>
            <xsl:when test="$name = 'Creative Commons Attribution 2.5'">
                <xsl:text>creative commons attribution 2.5.html</xsl:text>
            </xsl:when>
            <xsl:when test="$name = 'GNU Lesser General Public License v2.1 or later'">
                <xsl:text>gnu lesser general public license v2.1 or later.html</xsl:text>
            </xsl:when>
            <xsl:when test="$name = 'Mozilla Public License 2.0'">
                <xsl:text>mozilla public license 2.0.html</xsl:text>
            </xsl:when>
            <xsl:when test="$name = 'Plexus Classworlds License'">
                <xsl:text>plexus classworlds license.html</xsl:text>
            </xsl:when>
            <xsl:when test="$name = 'The JSoup MIT License'">
                <xsl:text>the jsoup mit license.html</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat(translate($name, $uppercase, $lowercase), '.txt')"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
