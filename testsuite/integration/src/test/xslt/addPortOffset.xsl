<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:d="urn:jboss:domain:1.3">

    <!--
        An XSLT style sheet which will add a port offset to a standalone-ha.xml config.
        This is done by changing:
        <server>
            ...
            <management>
                <management-interfaces>
                    <native-interface interface="management" port="$nativeInterfaceManagementPort"/>
                    <http-interface interface="management" port="$nativeInterfaceManagementPort"/>
                </management-interfaces>
            </management>
            ...
            <socket-binding-group name="standard-sockets" default-interface="public" port-offset="$portOffset">
            ...
        </server>
    -->

    <!-- Template parameters and default values. -->
    <xsl:param name="portOffset" select="'100'"/>
    <xsl:param name="nativeInterfaceManagementPort" select="'9999'"/>
    <xsl:param name="httpInterfaceManagementPort" select="'9990'"/>

    <!-- Set the native management port. -->
    <xsl:template match="//d:socket-binding-group/d:socket-binding[@name='management-native']/@port">
        <xsl:attribute name="port">${jboss.management.native.port:<xsl:value-of select="$nativeInterfaceManagementPort"/>}</xsl:attribute>
    </xsl:template>

    <!-- Set the HTTP management port. -->
    <xsl:template match="//d:socket-binding-group/d:socket-binding[@name='management-http']/@port">
        <xsl:attribute name="port">
            <xsl:value-of select="$httpInterfaceManagementPort"/>
        </xsl:attribute>
    </xsl:template>

    <!-- Set the port offset. -->
    <xsl:template match="//d:socket-binding-group[@name='standard-sockets']/@port-offset">
        <xsl:attribute name="port-offset">
            <xsl:value-of select="$portOffset"/>
        </xsl:attribute>
    </xsl:template>

    <!-- Copy everything else -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
