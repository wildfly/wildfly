jboss-client.jar is a combined client jar for WildFly, for use in non-maven environments. This jar should be used
with standalone clients only, not with deployments that are deployed to a WildFly instance.

This jar contains the classes required for remote JMS and EJB usage, and consists of the following shaded artifacts:

jakarta.jms:jakarta.jms-api
jakarta.transaction:jakarta.transaction-api
jakarta.ejb:jakarta.ejb-api
jakarta.resource:jakarta.resource-api

com.google.guava:guava
com.google.guava:failureaccess
commons-beanutils:commons-beanutils
commons-collections:commons-collections
io.netty:netty-buffer
io.netty:netty-resolver
io.netty:netty-transport
io.netty:netty-handler
io.netty:netty-handler-proxy
io.netty:netty-codec
io.netty:netty-codec-socks
io.netty:netty-common
io.netty:netty-transport-native-unix-common
io.netty:netty-transport-native-unix-common:linux-x86_64
io.netty:netty-transport-native-unix-common:linux-aarch_64
io.netty:netty-transport-classes-epoll
io.netty:netty-transport-native-epoll:linux-x86_64
io.netty:netty-transport-native-epoll:linux-aarch_64
io.netty:netty-transport-classes-kqueue
io.netty:netty-codec-http
io.undertow:undertow-core
org.apache.activemq:artemis-commons
org.apache.activemq:artemis-core-client
org.apache.activemq:artemis-hqclient-protocol
org.apache.activemq:artemis-jakarta-client
org.apache.activemq:artemis-selector
org.jboss:jboss-ejb-client
org.jboss.logging:jboss-logging
org.jboss.marshalling:jboss-marshalling
org.jboss.marshalling:jboss-marshalling-river
org.jboss.remoting:jboss-remoting
org.jboss.remotingjmx:remoting-jmx
org.jgroups:jgroups
org.slf4j:slf4j-api
org.slf4j:jcl-over-slf4j
org.jboss.threads:jboss-threads
org.wildfly:wildfly-naming-client
org.wildfly.common:wildfly-common
org.wildfly.discovery:wildfly-discovery-client
org.wildfly.transaction:wildfly-transaction-client
org.wildfly.client:wildfly-client-config
org.wildfly.wildfly-http-client:wildfly-http-client-common
org.wildfly.wildfly-http-client:wildfly-http-ejb-client
org.wildfly.wildfly-http-client:wildfly-http-naming-client
org.wildfly.wildfly-http-client:wildfly-http-transaction-client
org.wildfly.security:wildfly-elytron-digest
org.wildfly.security:wildfly-elytron-mechanism-digest
org.wildfly.security:wildfly-elytron-sasl-digest
org.wildfly.security:wildfly-elytron-mechanism-oauth2
org.wildfly.security:wildfly-elytron-sasl-oauth2
org.wildfly.security:wildfly-elytron-mechanism-scram
org.wildfly.security:wildfly-elytron-sasl-scram
org.wildfly.security:wildfly-elytron-sasl-entity
org.wildfly.security:wildfly-elytron-sasl-external
org.wildfly.security:wildfly-elytron-sasl-gs2
org.wildfly.security:wildfly-elytron-mechanism-gssapi
org.wildfly.security:wildfly-elytron-sasl-gssapi
org.wildfly.security:wildfly-elytron-sasl-otp
org.wildfly.security:wildfly-elytron-sasl-plain
org.wildfly.security:wildfly-elytron-x500


Maven users should not use this jar, but should use the following BOM dependencies instead

    <dependencies>
        <dependency>
            <groupId>org.wildfly</groupId>
            <artifactId>wildfly-ejb-client-bom</artifactId>
            <type>pom</type>
        </dependency>
        <dependency>
            <groupId>org.wildfly</groupId>
            <artifactId>wildfly-jms-client-bom</artifactId>
            <type>pom</type>
        </dependency>
    </dependencies>

This is because using maven with a shaded jar has a very high chance of causing class version conflicts, which is why
we do not publish this jar to the maven repository.
