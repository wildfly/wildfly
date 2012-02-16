<?xml version="1.0" encoding="UTF-8"?>
<!-- See http://www.w3.org/TR/xslt -->

      <!--
      An XSLT style sheet which will change the existing datasource definition to the desired database.
      This is done by changing:
      <server>
        ...
        <profile>
	  ...
          <subsystem xmlns="urn:jboss:domain:datasources:1.0">
	    <datasource jndi-name="java:jboss/datasources/ExampleDS enabled="true" use-java-context="true" pool-name="H2DS"/>
	      <connection-url>jdbc:h2:mem:test;DB_CLOSE_DELAY=-1</connection-url>
	      <driver>h2</driver>
	      <pool></pool>
	      <security>
	        <user-name>sa</user-name>
	        <passsword>sa</password>
	    </datasource>
	    <drivers>
	      <driver name="h2" module="com.h2database.h2">
	        <xa-datasource-class>org.h2.jdcx.JdbcDataSource</xa-datasource-class>
	      </driver>
	    </drivers>
	  </subsystem>
	  ...
	  </profile>
	...
      </server>

      to the following, in which the existing datasource definition is completely removed and replaced by the
      new datasource definition (where the driver is deployed into standalone/deployments and not loaded as a
      module):

      <server>
        ...
        <profile>
	  ...
          <subsystem xmlns="urn:jboss:domain:datasources:1.0">
	    <datasource jndi-name="java:jboss/datasources/ExampleDS enabled="true" use-java-context="true" pool-name="H2DS"/>
	      <connection-url>${ds.jdbc.url}</connection-url>
	      <driver>${ds.jdbc.driver}</driver>
	      <pool></pool>
	      <security>
	        <user-name>${ds.jdbc.user}</user-name>
	        <passsword>${ds.jdbc.pass}</password>
	    </datasource>
	  </subsystem>
	  ...
	  </profile>
	...
      </server>

      -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ds="urn:jboss:domain:datasources:1.0">
    <xsl:output method="xml" indent="yes"/>

    <xsl:param name="ds.jdbc.driver.jar" select="'fred'"/>
    <xsl:param name="ds.jdbc.url" select="'wilma'"/>
    <xsl:param name="ds.jdbc.user" select="'test'"/>
    <xsl:param name="ds.jdbc.pass" select="'test'"/>

    <xsl:variable name="newDatasourceDefinition">
            <ds:datasource jndi-name="java:jboss/datasources/ExampleDS" pool-name="ExamplePool" enabled="true" jta="true"
                       use-java-context="true">
                <ds:connection-url><xsl:value-of select="$ds.jdbc.url"/></ds:connection-url>
                <ds:driver><xsl:value-of select="$ds.jdbc.driver.jar"/></ds:driver>
                <ds:security>
                    <ds:user-name><xsl:value-of select="$ds.jdbc.user"/></ds:user-name>
                    <ds:password><xsl:value-of select="$ds.jdbc.pass"/></ds:password>
                </ds:security>
            </ds:datasource>
    </xsl:variable>

    <!-- Replace the old datasource with the new. -->
    <xsl:template match="//ds:subsystem/ds:datasources/ds:datasource[@jndi-name='java:jboss/datasources/ExampleDS']">
        <!-- http://docs.jboss.org/ironjacamar/userguide/1.0/en-US/html/deployment.html#deployingds_descriptor -->
        <xsl:copy-of select="$newDatasourceDefinition"/>
    </xsl:template>

    <!-- Get rid of the default driver defs. -->
    <xsl:template match="//ds:subsystem/ds:datasources/ds:drivers"/>

    <!-- Copy everything else. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>

