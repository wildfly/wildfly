<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:d="urn:jboss:domain:1.1"
                xmlns:jms="urn:jboss:domain:messaging:1.1"
    >
    <xsl:output method="xml" indent="yes"/>

    <xsl:param name="queueName"/>
    <xsl:param name="topicName"/>

    <xsl:variable name="newTopicAndQueue">
                   <jms:jms-queue>
                       <xsl:attribute name="name"><xsl:value-of select="$queueName"/></xsl:attribute>
                       <jms:entry name="queue/test"/>
                       <jms:entry name="java:jboss/exported/jms/queue/test"/>
                   </jms:jms-queue>
                   <jms:jms-topic>      
                       <xsl:attribute name="name"><xsl:value-of select="$topicName"/></xsl:attribute>
                       <jms:entry name="topic/test"/>
                       <jms:entry name="java:jboss/exported/jms/topic/test"/>
                   </jms:jms-topic>
    </xsl:variable>
    
    <!-- If jms-destinations exists, append the queue and topic. -->
    <xsl:template match="//jms:hornetq-server/jms:jms-destinations">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <xsl:copy-of select="$newTopicAndQueue"/>
        </xsl:copy>
    </xsl:template>

    <!-- If it does not exist, create it and insert them. -->
    <xsl:template match="//jms:hornetq-server[not(jms:jms-destinations)]">
                <xsl:copy>
                    <xsl:apply-templates select="node()|@*"/>
                    <jms:jms-destinations>
                        <xsl:copy-of select="$newTopicAndQueue"/>
                    </jms:jms-destinations>
                </xsl:copy>
    </xsl:template>
    
    <!-- Prevent duplicates. -->
    <xsl:template match="//jms:hornetq-server/jms:jms-destinations/jms:jms-queue[@name=$queueName]"/>
    <xsl:template match="//jms:hornetq-server/jms:jms-destinations/jms:jms-topic[@name=$topicName]"/>

    <!-- Copy everything else. -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
     
</xsl:stylesheet>
