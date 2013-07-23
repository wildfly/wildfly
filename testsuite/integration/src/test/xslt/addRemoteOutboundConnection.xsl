<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                
    <xsl:variable name="jboss" select="'urn:jboss:domain:'"/>
    <xsl:variable name="remoting" select="'urn:jboss:domain:remoting:'"/>
                
    <xsl:output method="xml" indent="yes"/>

    <!--
        An XSLT style sheet which will enable EJB Remote calling to another server,
        by adding an outbound-socket-binding and remote-outbound-connection.
    -->

    <!-- remote outbound connection parameters -->
    <xsl:param name="connectionName" select="'remote-ejb-connection'"/>
    <xsl:param name="node" select="'localhost'"/>
    <xsl:param name="remotePort" select="'4447'"/>
    <xsl:param name="securityRealm" select="NOT_DEFINED"/>
    <xsl:param name="userName" select="NOT_DEFINED"/>
    <xsl:param name="protocol" select="http-remoting"/>

    <xsl:variable name="newRemoteOutboundConnection">
        <remote-outbound-connection>
            <xsl:attribute name="name"><xsl:value-of select="$connectionName"/></xsl:attribute>
            <xsl:attribute name="outbound-socket-binding-ref">binding-<xsl:value-of select="$connectionName"/></xsl:attribute>
            <xsl:attribute name="protocol"><xsl:value-of select="$protocol"/></xsl:attribute>
            <xsl:if test="$securityRealm != 'NOT_DEFINED'">
                <xsl:attribute name="security-realm"><xsl:value-of select="$securityRealm"/></xsl:attribute>
            </xsl:if>
            <xsl:if test="$userName != 'NOT_DEFINED'">
                <xsl:attribute name="username"><xsl:value-of select="$userName"/></xsl:attribute>
            </xsl:if>
            <properties>
                <property name="SASL_POLICY_NOANONYMOUS" value="false"/>
                <property name="SSL_ENABLED" value="false"/>
            </properties>
        </remote-outbound-connection>
    </xsl:variable>

    <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>


    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $remoting)]">
        <xsl:choose>
            <xsl:when test="not(//*[local-name()='subsystem' and starts-with(namespace-uri(), $remoting)]
            					 /*[local-name()='outbound-connections'])">
                <xsl:copy>
                    <xsl:apply-templates select="node()|@*"/>
                    <outbound-connections>
                        <xsl:copy-of select="$newRemoteOutboundConnection"/>
                    </outbound-connections>
                </xsl:copy>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy>
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:copy>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $remoting)]
            			  /*[local-name()='outbound-connections']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <xsl:copy-of select="$newRemoteOutboundConnection"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='socket-binding-group' and starts-with(namespace-uri(), $jboss) and @name='standard-sockets']">
        <xsl:copy>
            <xsl:attribute name="name">
                <xsl:value-of select="'standard-sockets'"/>
            </xsl:attribute>
            <xsl:attribute name="default-interface">
                <xsl:value-of select="@default-interface"/>
            </xsl:attribute>
            <xsl:attribute name="port-offset">
                <xsl:value-of select="@port-offset"/>
            </xsl:attribute>
            <xsl:apply-templates select="node()"/>
            <outbound-socket-binding>
                <xsl:attribute name="name">binding-<xsl:value-of select="$connectionName"/></xsl:attribute>
                <remote-destination>
                    <xsl:attribute name="host"><xsl:value-of select="$node"/></xsl:attribute>
                    <xsl:attribute name="port"><xsl:value-of select="$remotePort"/></xsl:attribute>
                </remote-destination>
            </outbound-socket-binding>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
