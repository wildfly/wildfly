<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

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
	  <socket-binding name="jgroups-mping" port="..." multicast-address="mpingMcastAddress" multicast-port="..."/>
	  <socket-binding name="modcluster" port="..." multicast-address="modclusterMcastAddress" multicast-port="..."/>
        </socket-binding-group>
	...
      </server>
    -->
                
    <xsl:variable name="jboss" select="'urn:jboss:domain:'"/>
    <xsl:variable name="webservices" select="'urn:jboss:domain:webservices:'"/>
    <xsl:variable name="xts" select="'urn:jboss:domain:xts:'"/>

    <!-- IP addresses  select="..." is default value. -->
    <xsl:param name="managementIPAddress" select="'127.0.0.1'"/>
    <xsl:param name="publicIPAddress"     select="'127.0.0.1'"/>

    <!-- Multi-cast addresses. -->
    <xsl:param name="udpMcastAddress"         select="'230.0.0.4'"/>
    <xsl:param name="mpingMcastAddress"       select="$udpMcastAddress"/>
    <xsl:param name="modclusterMcastAddress"  select="$udpMcastAddress"/>


    <!-- Change the management and public IP addresses. -->
    <xsl:template match="//*[local-name()='interfaces' and starts-with(namespace-uri(), $jboss)]
    				 	  /*[local-name()='interface' and @name='management']
    				 	  /*[local-name()='inet-address']">
        <xsl:copy>
            <xsl:attribute name="value">
                <xsl:value-of select="$managementIPAddress"/>
            </xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='interfaces' and starts-with(namespace-uri(), $jboss)]
    				 	  /*[local-name()='interface' and @name='public']
    				 	  /*[local-name()='inet-address']">
        <xsl:copy><xsl:attribute name="value">${jboss.bind.address:<xsl:value-of select="$publicIPAddress"/>}</xsl:attribute></xsl:copy>
    </xsl:template>
    <xsl:template match="//*[local-name()='interfaces' and starts-with(namespace-uri(), $jboss)]
    				 	  /*[local-name()='interface' and @name='unsecure']
    				 	  /*[local-name()='inet-address']">
        <xsl:copy><xsl:attribute name="value">${jboss.bind.address.unsecure:<xsl:value-of select="$publicIPAddress"/>}</xsl:attribute></xsl:copy>
    </xsl:template>

    <!-- Change UDP multicast addresses. -->
    <xsl:template match="//*[local-name()='socket-binding-group' and @name='standard-sockets' and starts-with(namespace-uri(), $jboss)]
    				 	  /*[local-name()='socket-binding' and @name='jgroups-udp']/@multicast-address">
        <xsl:attribute name="multicast-address">${jboss.default.multicast.address:<xsl:value-of select="$udpMcastAddress"/>}</xsl:attribute>
    </xsl:template>

    <!-- Change MPING multicast addresses. -->
    <xsl:template match="//*[local-name()='socket-binding-group' and @name='standard-sockets' and starts-with(namespace-uri(), $jboss)]
    				 	  /*[local-name()='socket-binding' and @name='jgroups-mping']/@multicast-address">
        <xsl:attribute name="multicast-address">${jboss.default.multicast.address:<xsl:value-of select="$mpingMcastAddress"/>}</xsl:attribute>
    </xsl:template>

    <!-- Change modcluster multicast addresses. -->
    <xsl:template match="//*[local-name()='socket-binding-group' and @name='standard-sockets' and starts-with(namespace-uri(), $jboss)]
    				 	  /*[local-name()='socket-binding' and @name='modcluster']/@multicast-address">
        <xsl:attribute name="multicast-address">
            <xsl:value-of select="$modclusterMcastAddress"/>
        </xsl:attribute>
    </xsl:template>

    <!-- Change WSDL host. -->
    <xsl:template match="//*[local-name()='wsdl-host' and starts-with(namespace-uri(), $webservices)]">
        <xsl:copy>${jboss.bind.address:<xsl:value-of select="$publicIPAddress"/>}</xsl:copy>
    </xsl:template>

    <!-- Change XTS Coordinator -->
    <xsl:template match="//*[local-name()='xts-environment' and starts-with(namespace-uri(), $xts)]/@url">
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
    <xsl:template match="//*[local-name()='outbound-socket-binding' and @name='mail-smtp' and starts-with(namespace-uri(), $jboss)]
    					  /*[local-name()='remote-destination']/@host">
        <xsl:attribute name="host"><xsl:value-of select="$publicIPAddress"/></xsl:attribute>
    </xsl:template>

    <!-- Copy everything else. -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
