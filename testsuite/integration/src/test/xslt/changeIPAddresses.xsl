
<xsl:stylesheet version="2.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns="urn:jboss:domain:1.2"
		xmlns:d="urn:jboss:domain:1.2"
        xmlns:ws11="urn:jboss:domain:webservices:1.1"
        xmlns:xts="urn:jboss:domain:xts:1.0"
                >

    <!--
      An XSLT style sheet which will change IP unicast and multicast addresses for standalone.xml and standalone-ha.xml.
      This is done by changing:
      <server>
        ...
        <interfaces>
	    <interface name="management">
	      <inet-address value="$managementIPAddress"/>
	    </interface>
	    <interface name="public">
	      <inet-address value="$publicIPAddress"/>
	    </interface>
        </interfaces>
	...
        <socket-binding-group name="standard-sockets" default-interface="public">
	  ...
	  <socket-binding name="jgroups-udp" port="..." multicast-address="$udpMcastAddress" multicast-port="..."/>
	  <socket-binding name="jgroups-diagnostics" port="..." multicast-address="diagnosticsMcastAddress" multicast-port="..."/>
	  <socket-binding name="jgroups-mping" port="..." multicast-address="mpingMcastAddress" multicast-port="..."/>
	  <socket-binding name="modcluster" port="..." multicast-address="modclusterMcastAddress" multicast-port="..."/>
        </socket-binding-group>
	...
      </server>
    -->

    <!-- IP addresses  select="..." is default value. -->
    <xsl:param name="managementIPAddress" select="'127.0.0.1'"/>
    <xsl:param name="publicIPAddress"     select="'127.0.0.1'"/>

    <!-- Multi-cast addresses. -->
    <xsl:param name="udpMcastAddress"         select="'230.0.0.4'"/>
    <xsl:param name="diagnosticsMcastAddress" select="$udpMcastAddress"/>
    <xsl:param name="mpingMcastAddress"       select="$udpMcastAddress"/>
    <xsl:param name="modclusterMcastAddress"  select="$udpMcastAddress"/>


    <!-- Change the management and public IP addresses. -->
    <xsl:template match="//d:interfaces/d:interface[@name='management']/d:inet-address">
        <xsl:copy>
            <xsl:attribute name="value">
                <xsl:value-of select="$managementIPAddress"/>
            </xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//d:interfaces/d:interface[@name='public']/d:inet-address">
        <xsl:copy><xsl:attribute name="value">${jboss.bind.address:<xsl:value-of select="$publicIPAddress"/>}</xsl:attribute></xsl:copy>
    </xsl:template>
    <xsl:template match="//d:interfaces/d:interface[@name='unsecure']/d:inet-address">
        <xsl:copy><xsl:attribute name="value">${jboss.bind.address.unsecure:<xsl:value-of select="$publicIPAddress"/>}</xsl:attribute></xsl:copy>
    </xsl:template>

    <!-- Change UDP multicast addresses. -->
    <xsl:template match="//d:socket-binding-group[@name='standard-sockets']/d:socket-binding[@name='jgroups-udp']/@multicast-address">
        <xsl:attribute name="multicast-address">${jboss.default.multicast.address:<xsl:value-of select="$udpMcastAddress"/>}</xsl:attribute>
    </xsl:template>

    <!-- Change diagnostics multicast addresses. -->
    <xsl:template match="//d:socket-binding-group[@name='standard-sockets']/d:socket-binding[@name='jgroups-diagnostics']/@multicast-address">
        <xsl:attribute name="multicast-address">
            <xsl:value-of select="$diagnosticsMcastAddress"/>
        </xsl:attribute>
    </xsl:template>

    <!-- Change MPING multicast addresses. -->
    <xsl:template match="//d:socket-binding-group[@name='standard-sockets']/d:socket-binding[@name='jgroups-mping']/@multicast-address">
        <xsl:attribute name="multicast-address">${jboss.default.multicast.address:<xsl:value-of select="$mpingMcastAddress"/>}</xsl:attribute>
    </xsl:template>

    <!-- Change modcluster multicast addresses. -->
    <xsl:template match="//d:socket-binding-group[@name='standard-sockets']/d:socket-binding[@name='modcluster']/@multicast-address">
        <xsl:attribute name="multicast-address">
            <xsl:value-of select="$modclusterMcastAddress"/>
        </xsl:attribute>
    </xsl:template>

    <!-- Change WSDL host. -->
    <xsl:template match="//ws11:wsdl-host">
        <xsl:copy>${jboss.bind.address:<xsl:value-of select="$publicIPAddress"/>}</xsl:copy>
    </xsl:template>

    <!-- Change XTS Coordinator -->
    <xsl:template match="//xts:xts-environment/@url">
        <xsl:attribute name="url">
            <xsl:choose>
                <xsl:when test="contains($publicIPAddress,':')">
                    <xsl:value-of select="concat('http://[', $publicIPAddress, ']:8080/ws-c11/ActivationService')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat('http://', $publicIPAddress, ':8080/ws-c11/ActivationService')"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:attribute>
    </xsl:template>

    <!-- Mail SMTP -->
    <xsl:template match="//d:outbound-socket-binding[@name='mail-smtp']/d:remote-destination/@host">
        <xsl:attribute name="host"><xsl:value-of select="$publicIPAddress"/></xsl:attribute>
    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
