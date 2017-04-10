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
    <xsl:variable name="pl-fed" select="'urn:jboss:domain:picketlink-federation:'"/>
    <xsl:variable name="pl-idm" select="'urn:jboss:domain:picketlink-identity-management:'"/>
    <xsl:variable name="pl-default-ip" select="'${jboss.bind.address:127.0.0.1}'"/>

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
            <xsl:attribute name="value"><xsl:value-of select="$managementIPAddress"/></xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='interfaces' and starts-with(namespace-uri(), $jboss)]
                          /*[local-name()='interface' and @name='public']
                          /*[local-name()='inet-address']">
        <xsl:copy>
            <xsl:attribute name="value">${jboss.bind.address:<xsl:value-of select="$publicIPAddress"/>}</xsl:attribute>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="//*[local-name()='interfaces' and starts-with(namespace-uri(), $jboss)]
                          /*[local-name()='interface' and @name='private']
                          /*[local-name()='inet-address']">
        <xsl:copy>
            <xsl:attribute name="value">${jboss.bind.address.private:<xsl:value-of select="$publicIPAddress"/>}</xsl:attribute>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="//*[local-name()='interfaces' and starts-with(namespace-uri(), $jboss)]
                          /*[local-name()='interface' and @name='unsecure']
                          /*[local-name()='inet-address']">
        <xsl:copy>
            <xsl:attribute name="value">${jboss.bind.address.unsecure:<xsl:value-of select="$publicIPAddress"/>}</xsl:attribute>
        </xsl:copy>
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

    <!-- Change PicketLink Federation -->
    <xsl:template match="//*[local-name()='identity-provider' and starts-with(namespace-uri(), $pl-fed)]/@url">
        <xsl:variable name="origin" select="current()"/>
        <xsl:attribute name="url">
            <xsl:choose>
              <xsl:when test="contains($origin, $pl-default-ip)">
	            <xsl:choose>
	                <xsl:when test="contains($publicIPAddress,':')">
	                    <xsl:value-of select="concat(substring-before($origin, $pl-default-ip), '[', $publicIPAddress, ']', substring-after($origin, $pl-default-ip))"/>
	                </xsl:when>
	                <xsl:otherwise>
	                    <xsl:value-of select="concat(substring-before($origin, $pl-default-ip), $publicIPAddress, substring-after($origin, $pl-default-ip))"/>
	                </xsl:otherwise>
	            </xsl:choose>
              </xsl:when>
              <xsl:otherwise>
              	<xsl:value-of select="$origin"/>
              </xsl:otherwise>
            </xsl:choose>
        </xsl:attribute>
    </xsl:template>

    <xsl:template match="//*[local-name()='service-provider' and starts-with(namespace-uri(), $pl-fed)]/@url">
        <xsl:variable name="origin" select="current()"/>
        <xsl:attribute name="url">
            <xsl:choose>
              <xsl:when test="contains($origin, $pl-default-ip)">
	            <xsl:choose>
	                <xsl:when test="contains($publicIPAddress,':')">
	                    <xsl:value-of select="concat(substring-before($origin, $pl-default-ip), '[', $publicIPAddress, ']', substring-after($origin, $pl-default-ip))"/>
	                </xsl:when>
	                <xsl:otherwise>
	                    <xsl:value-of select="concat(substring-before($origin, $pl-default-ip), $publicIPAddress, substring-after($origin, $pl-default-ip))"/>
	                </xsl:otherwise>
	            </xsl:choose>
              </xsl:when>
              <xsl:otherwise>
              	<xsl:value-of select="$origin"/>
              </xsl:otherwise>
            </xsl:choose>
        </xsl:attribute>
    </xsl:template>

    <xsl:template match="//*[local-name()='trust-domain' and starts-with(namespace-uri(), $pl-fed)]/@name">
        <xsl:attribute name="name">
            <xsl:choose>
                <xsl:when test="contains($publicIPAddress,':')">
                    <xsl:value-of select="concat('[', $publicIPAddress, ']')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$publicIPAddress"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:attribute>
    </xsl:template>

	<!-- PicketLink Identity Management -->
    <xsl:template match="//*[local-name()='ldap-store' and starts-with(namespace-uri(), $pl-idm)]/@url">
        <xsl:variable name="origin" select="current()"/>
        <xsl:attribute name="url">
            <xsl:choose>
              <xsl:when test="contains($origin, $pl-default-ip)">
	            <xsl:choose>
	                <xsl:when test="contains($publicIPAddress,':')">
	                    <xsl:value-of select="concat(substring-before($origin, $pl-default-ip), '[', $publicIPAddress, ']', substring-after($origin, $pl-default-ip))"/>
	                </xsl:when>
	                <xsl:otherwise>
	                    <xsl:value-of select="concat(substring-before($origin, $pl-default-ip), $publicIPAddress, substring-after($origin, $pl-default-ip))"/>
	                </xsl:otherwise>
	            </xsl:choose>
              </xsl:when>
              <xsl:otherwise>
              	<xsl:value-of select="$origin"/>
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
