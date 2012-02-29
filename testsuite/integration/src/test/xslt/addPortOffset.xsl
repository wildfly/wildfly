<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:d="urn:jboss:domain:1.2">

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

    <!-- port changes -->
    <xsl:param name="portOffset" select="'100'"/>
    <xsl:param name="nativeInterfaceManagementPort" select="'9999'"/>
    <xsl:param name="httpInterfaceManagementPort" select="'9990'"/>

    <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <!-- change the native interface management port -->
    <xsl:template match="//d:socket-binding-group/d:socket-binding[@name='management-native']">
        <xsl:copy>
            <xsl:attribute name="name">
                <xsl:value-of select="@name"/>
            </xsl:attribute>
            <xsl:attribute name="interface">
                <xsl:value-of select="@interface"/>
            </xsl:attribute>
            <xsl:attribute name="port">
                <xsl:value-of select="$nativeInterfaceManagementPort"/>
            </xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <!-- change the HTTP interface management port -->
    <xsl:template match="//d:socket-binding-group/d:socket-binding[@name='management-http']">
        <xsl:copy>
            <xsl:attribute name="name">
                <xsl:value-of select="@name"/>
            </xsl:attribute>
            <xsl:attribute name="interface">
                <xsl:value-of select="@interface"/>
            </xsl:attribute>
            <xsl:attribute name="port">
                <xsl:value-of select="$httpInterfaceManagementPort"/>
            </xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <!-- add the port offset -->
    <xsl:template match="//d:socket-binding-group[@name='standard-sockets']">
        <xsl:copy>
            <xsl:attribute name="name">
                <xsl:value-of select="'standard-sockets'"/>
            </xsl:attribute>
            <xsl:attribute name="default-interface">
                <xsl:value-of select="'public'"/>
            </xsl:attribute>
            <xsl:attribute name="port-offset">
                <xsl:value-of select="$portOffset"/>
            </xsl:attribute>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
