<?xml version="1.0"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

   <!-- xsl:output method="xml" indent="yes"/ -->

   <xsl:template match="@*|node()">
      <xsl:copy>
         <xsl:apply-templates select="@*|node()" />
      </xsl:copy>
   </xsl:template>

   <xsl:template match="*[local-name() = 'subsystem' and contains(concat(text(), '&#xA;'), '/jgroups.xml&#xA;')]">
      <xsl:copy>
         <xsl:attribute name="supplement">
            <xsl:value-of select="'gossip'"/>
         </xsl:attribute>
         <xsl:apply-templates select="@*|node()" />
      </xsl:copy>
   </xsl:template>

</xsl:stylesheet>
