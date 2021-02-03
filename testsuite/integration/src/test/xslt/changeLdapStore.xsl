<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <!--
        An XSLT style sheet which will set a read-only attribute to false in ldap store when is-identifier is false
        Result looks like this:
        <server>
            ...
                <identity-configuration name="multiple.store">
                    <ldap-store>
                        <mappings>
                            <mapping>
                                <attribute name="loginName" ldap-name="uid" is-identifier="true"/>
                                <attribute ldap-name="createTimeStamp" read-only="true"/>
                                <attribute ldap-name="createTimeStamp" is-identifier="false" read-only="true"/>
                            </mapping>
                        </mappings>
                    </ldap-store>
                </identity-configuration>
            ...
        </server>
    -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), 'urn:jboss:domain:picketlink-identity-management:')]
                         //*[local-name()='ldap-store']
                         //*[local-name()='mapping']
                         //*[local-name()='attribute' and (@is-identifier='false' or not(@is-identifier))]">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:attribute name="read-only">false</xsl:attribute>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Copy everything else -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
