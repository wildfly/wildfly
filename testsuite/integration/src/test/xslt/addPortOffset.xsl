<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

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

    <xsl:variable name="jboss" select="'urn:jboss:domain:'"/>

    <!-- Template parameters and default values. -->
    <xsl:param name="portOffset" select="'100'"/>
    <xsl:param name="nativeInterfaceManagementPort" select="'9999'"/>
    <xsl:param name="httpInterfaceManagementPort" select="'9990'"/>

    <!-- Set the native management port. -->
    <xsl:template match="//*[local-name()='socket-binding-group' and starts-with(namespace-uri(), $jboss)]
    				 	  /*[local-name()='socket-binding' and @name='management-native']/@port">
        <xsl:attribute name="port">${jboss.management.native.port:<xsl:value-of select="$nativeInterfaceManagementPort"/>}</xsl:attribute>
    </xsl:template>

    <!-- Set the HTTP management port. -->
    <xsl:template match="//*[local-name()='socket-binding-group' and starts-with(namespace-uri(), $jboss)]
    				 	  /*[local-name()='socket-binding' and @name='management-http']/@port">
        <xsl:attribute name="port">
            <xsl:value-of select="$httpInterfaceManagementPort"/>
        </xsl:attribute>
    </xsl:template>

    <!-- Set the port offset. -->
    <xsl:template match="//*[local-name()='socket-binding-group' and @name='standard-sockets' and starts-with(namespace-uri(), $jboss)]/@port-offset">
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
