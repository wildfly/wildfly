<?xml version="1.0" encoding="UTF-8"?>

<!-- This file overwrites given pom.xml's dependencySourceIncludes and dependencySourceExcludes
     with those from from dependencySourceInclExcl.xml. -->

<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:pom="http://maven.apache.org/POM/4.0.0"
>
    <xsl:output method="xml" indent="no"/>

    <!-- Copy <dependencySourceIncludes> and <dependencySourceExcludes> from dependencySourceInclExcl.xml.
         Only works if the elements are already present.
    -->
    <xsl:template match="//pom:profile[pom:id='javadocDist']/pom:build/pom:plugins/pom:plugin[pom:artifactId='maven-javadoc-plugin']/pom:executions/pom:execution/pom:configuration/pom:dependencySourceIncludes">
        <xsl:copy-of select="document('target/dependencySourceInclExcl.xml')/root/dependencySourceIncludes">
            <!--
            <xsl:attribute name="xmlns">X</xsl:attribute>
            <xsl:attribute name="xmlns" namespace="pom">Y</xsl:attribute>
            <xsl:attribute name="xmlns" namespace="http://maven.apache.org/POM/4.0.0">Z</xsl:attribute>
            -->
        </xsl:copy-of>
    </xsl:template>

    <xsl:template match="//pom:profile[pom:id='javadocDist']/pom:build/pom:plugins/pom:plugin[pom:artifactId='maven-javadoc-plugin']/pom:executions/pom:execution/pom:configuration/pom:dependencySourceExcludes">
        <xsl:copy-of select="document('target/dependencySourceInclExcl.xml')/root/dependencySourceExcludes"/>
    </xsl:template>

    <xsl:template match="//pom:profile[pom:id='javadocDist']/pom:build/pom:plugins/pom:plugin[pom:artifactId='maven-javadoc-plugin']/pom:executions/pom:execution/pom:configuration/pom:groups">
        <xsl:copy-of select="document('target/groups.xml')/groups"/>
    </xsl:template>


    <!-- Copy everything else. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
