<?xml version='1.0'?>
<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>

<xsl:param name="thedate">undefined</xsl:param>

<xsl:output method='html' indent='yes' doctype-public='-//W3C//DTD HTML 3.2 FINAL//EN'/>

<xsl:template match='/'>

    <xsl:variable name="numberOfTests" select="sum(//@tests)"/>
    <xsl:variable name="numberOfErrors" select="sum(//@errors)"/>
    <xsl:variable name="numberOfFailures" select="sum(//@failures)"/>
    <xsl:variable name="numberOfSuccesses" select="$numberOfTests - $numberOfErrors - $numberOfFailures"/>

    <html>
    <head>
        <title>JBossTest - Detailed Results</title>
    </head>

<body>

    <h3>JBossTest daily test results</h3>
    <b>SUMMARY</b>
    <p>Number of tests run:   <xsl:value-of select="$numberOfTests"/></p>

    <hr/>

    <table bgcolor="yellow">
        <tr>
            <td>Successful tests:</td><td bgcolor="cyan"><xsl:value-of select="$numberOfSuccesses"/></td>
        </tr><tr>
            <td>Errors:</td><td bgcolor="cyan"><xsl:value-of select="$numberOfErrors"/></td>
        </tr><tr>
            <td>Failures:</td><td bgcolor="cyan"><xsl:value-of select="$numberOfFailures"/></td>
        </tr>
    </table>

    <hr/>

    <pre>
[time of test: <xsl:value-of select="$thedate"/> GMT]
[java.version: <xsl:value-of select="$java_version"/>]
[java.vendor: <xsl:value-of select="$java_vendor"/>]
[java.vm.version: <xsl:value-of select="$java_vm_version"/>]
[java.vm.name: <xsl:value-of select="$java_vm_name"/>]
[java.vm.info: <xsl:value-of select="$java_vm_info"/>]
[os.name: <xsl:value-of select="$os_name"/>]
[os.arch: <xsl:value-of select="$os_arch"/>]
[os.version: <xsl:value-of select="$os_version"/>]
    </pre>


    <hr/>

    <xsl:if test='$numberOfFailures!=0 or $numberOfErrors!=0'>

        <h4>Testsuite summary</h4>


        <table>
            <tr bgcolor="gray">
                <th>Testsuites</th>
                <th>Tests</th>
                <th>Successes</th>
                <th>Failures</th>
                <th>Errors</th>
            </tr>
            <xsl:for-each select="//testsuite">
            <tr>
                <td align="left"> <a><xsl:attribute name='href'>#<xsl:value-of select='@name'/></xsl:attribute><xsl:value-of select="@name"/></a> </td>
                <td align="center">
                    <xsl:if test='@errors!=0 or @failures!=0'><xsl:attribute name='bgcolor'>red</xsl:attribute></xsl:if>
                    <xsl:value-of select='@tests'/></td>
                <td align="center">
                    <xsl:if test='@errors!=0 or @failures!=0'><xsl:attribute name='bgcolor'>red</xsl:attribute></xsl:if>
                    <xsl:value-of select='@tests - @errors - @failures'/></td>
                <td align="center">
                    <xsl:if test='@failures!=0'><xsl:attribute name='bgcolor'>red</xsl:attribute></xsl:if>
                    <xsl:value-of select='@failures'/></td>
                <td align="center">
                    <xsl:if test='@errors!=0'><xsl:attribute name='bgcolor'>red</xsl:attribute></xsl:if>
                    <xsl:value-of select='@errors'/></td>
            </tr>
            </xsl:for-each>
        </table>

        <hr/>

        <h4>Errors details</h4>

        <xsl:for-each select="//testsuite">
        <p>
            <a><xsl:attribute name='name'><xsl:value-of select='@name'/></xsl:attribute><b><xsl:value-of select='@name'/></b></a>
        <p/>

        <table border="1">
            <tr bgcolor="gray">
                <th>Test</th>
                <th>Time</th>
                <th>Problem Type</th>
                <th>Exception</th>
                <th>Message</th>
                <th>Stack Trace</th>
            </tr>

            <xsl:for-each select="testcase">
            <tr>
                <td><xsl:value-of select="@name"/>
                </td><td><xsl:value-of select="@time"/>
                </td>
                <xsl:for-each select="error | failure">
                    <td><xsl:value-of select="name()"/>
                    </td><td><xsl:value-of select="@type"/>
                    </td><td><xsl:value-of select="@message"/>
                    </td><td><pre><xsl:value-of select="."/></pre>
                    </td>
                </xsl:for-each>
            </tr>
            </xsl:for-each>
        </table>

        <hr/>
        </xsl:for-each>
    </xsl:if>

</body>
</html>

</xsl:template>
</xsl:stylesheet>
