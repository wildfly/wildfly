<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:variable name="nsInf" select="'urn:jboss:domain:logging:3.0'"/>

    <!--
      An XSLT style sheet which will enable trace logging for the test suite.
      This can be enabled via -Dtrace=org.jboss.as.category1,org.jboss.as.category2
    -->
    <xsl:param name="trace" />

    <xsl:template name="output-loggers">
        <xsl:param name="list" />
        <xsl:variable name="first" select="substring-before(concat($list,','), ',')" />
        <xsl:variable name="remaining" select="substring-after($list, ',')" />
        <xsl:element name="logger"  namespace="{$nsInf}">
            <xsl:attribute name="category" >
                <xsl:value-of select="$first"/>
            </xsl:attribute>
            <level >
                <xsl:attribute name="name" >
                    <xsl:value-of select="'TRACE'"/>
                </xsl:attribute>
            </level>
        </xsl:element>
        <xsl:if test="string-length($remaining) > 0">
            <xsl:call-template name="output-loggers">
                <xsl:with-param name="list" select="$remaining" />
            </xsl:call-template>
        </xsl:if>
    </xsl:template>


    <!-- <xsl:template match="//l:subsystem/l:console-handler"> -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsInf)]
   						/*[local-name()='console-handler' and starts-with(namespace-uri(), $nsInf)]/@name">
   						       
        <xsl:choose>
            <xsl:when test="$trace='none'">
                <xsl:copy>
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:copy>
            </xsl:when>
            <xsl:otherwise>
  				<xsl:attribute name="name"> 
        			<xsl:value-of select="'CONSOLE'"/>
        		</xsl:attribute>                 
            </xsl:otherwise>
        </xsl:choose>

     </xsl:template>
     
	<xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsInf)]
   						/*[local-name()='console-handler' and starts-with(namespace-uri(), $nsInf)]
   						/*[local-name()='level' and starts-with(namespace-uri(), $nsInf)]/@name">
   						
        <xsl:choose>
            <xsl:when test="$trace='none'">
                <xsl:copy>
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:copy>
            </xsl:when>
            <xsl:otherwise>
				<xsl:attribute name="name"> 
        			<xsl:value-of select="'TRACE'" />
        		</xsl:attribute>
            </xsl:otherwise>
        </xsl:choose>

	</xsl:template>
     
    <!-- <xsl:template match="//l:subsystem/l:periodic-rotating-file-handler" use-when="$trace"> -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsInf)]
   						/*[local-name()='periodic-rotating-file-handler' and starts-with(namespace-uri(), $nsInf)]" use-when="$trace">
        <xsl:choose>
            <xsl:when test="$trace='none'">
                <xsl:copy>
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:copy>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy>
                    <xsl:attribute name="name">
                        <xsl:value-of select="'FILE'"/>
                    </xsl:attribute>
                    <level name="TRACE"/>
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:copy>
                <xsl:call-template name="output-loggers">
                    <xsl:with-param name="list" select="$trace" />
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
