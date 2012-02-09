<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:d="urn:jboss:domain:1.1"
                xmlns:r="urn:jboss:domain:remoting:1.1">
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

    <xsl:variable name="newRemoteOutboundConnection">
        <r:remote-outbound-connection>
            <xsl:attribute name="name"><xsl:value-of select="$connectionName"/></xsl:attribute>
            <xsl:attribute name="outbound-socket-binding-ref">binding-<xsl:value-of select="$connectionName"/></xsl:attribute>
            <xsl:if test="$securityRealm != 'NOT_DEFINED'">
                <xsl:attribute name="security-realm"><xsl:value-of select="$securityRealm"/></xsl:attribute>
            </xsl:if>
            <xsl:if test="$userName != 'NOT_DEFINED'">
                <xsl:attribute name="username"><xsl:value-of select="$userName"/></xsl:attribute>
            </xsl:if>
            <r:properties>
                <r:property name="SASL_POLICY_NOANONYMOUS" value="false"/>
                <r:property name="SSL_ENABLED" value="false"/>
            </r:properties>
        </r:remote-outbound-connection>
    </xsl:variable>

    <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//r:subsystem">
        <xsl:choose>
            <xsl:when test="not(//r:subsystem/r:outbound-connections)">
                <xsl:copy>
                    <xsl:apply-templates select="node()|@*"/>
                    <r:outbound-connections>
                        <xsl:copy-of select="$newRemoteOutboundConnection"/>
                    </r:outbound-connections>
                </xsl:copy>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy>
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:copy>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="//r:subsystem/r:outbound-connections">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <xsl:copy-of select="$newRemoteOutboundConnection"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//d:socket-binding-group[@name='standard-sockets']">
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
            <d:outbound-socket-binding>
                <xsl:attribute name="name">binding-<xsl:value-of select="$connectionName"/></xsl:attribute>
                <d:remote-destination>
                    <xsl:attribute name="host"><xsl:value-of select="$node"/></xsl:attribute>
                    <xsl:attribute name="port"><xsl:value-of select="$remotePort"/></xsl:attribute>
                </d:remote-destination>
            </d:outbound-socket-binding>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
