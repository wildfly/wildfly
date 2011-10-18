<xsl:stylesheet version="2.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:d="urn:jboss:domain:1.0">

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

  <!-- IP addresses -->
  <xsl:param name="managementIPAddress" select="'127.0.0.1'"/>
  <xsl:param name="publicIPAddress" select="'127.0.0.1'"/>

  <!-- mcast addresses -->
  <xsl:param name="udpMcastAddress" select="'230.0.0.4'"/>
  <xsl:param name="diagnosticsMcastAddress" select="'224.0.75.75'"/>
  <xsl:param name="mpingMcastAddress" select="'230.0.0.4'"/>
  <xsl:param name="modclusterMcastAddress" select="'224.0.1.105'"/>

  <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <!-- change the management and public IP addresses -->
  <xsl:template match="//d:interfaces/d:interface[@name='management']/d:inet-address">
    <xsl:copy>
      <xsl:attribute name="value">
	<xsl:value-of select="$managementIPAddress"/>
      </xsl:attribute>
    </xsl:copy>
  </xsl:template>

    <xsl:template match="//d:interfaces/d:interface[@name='public']/d:inet-address">
      <xsl:copy>
        <xsl:attribute name="value">
          <xsl:value-of select="$publicIPAddress"/>
        </xsl:attribute>
      </xsl:copy>
    </xsl:template>

  <!-- change udp multicast addresses -->
  <xsl:template match="//d:socket-binding-group[@name='standard-sockets']/d:socket-binding[@name='jgroups-udp']">
    <xsl:copy>
      <xsl:attribute name="multicast-address">
	    <xsl:value-of select="$udpMcastAddress"/>
      </xsl:attribute>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <!-- change diagnostics multicast addresses -->
  <xsl:template match="//d:socket-binding-group[@name='standard-sockets']/d:socket-binding[@name='jgroups-diagnostics']">
    <xsl:copy>
      <xsl:attribute name="multicast-address">
        <xsl:value-of select="$diagnosticsMcastAddress"/>
      </xsl:attribute>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <!-- change MPING multicast addresses -->
  <xsl:template match="//d:socket-binding-group[@name='standard-sockets']/d:socket-binding[@name='jgroups-mping']">
    <xsl:copy>
      <xsl:attribute name="multicast-address">
        <xsl:value-of select="$mpingMcastAddress"/>
      </xsl:attribute>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <!-- change modcluster multicast addresses -->
  <xsl:template match="//d:socket-binding-group[@name='standard-sockets']/d:socket-binding[@name='modcluster']">
    <xsl:copy>
      <xsl:attribute name="multicast-address">
         <xsl:value-of select="$modclusterMcastAddress"/>
      </xsl:attribute>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
