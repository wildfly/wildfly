/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import java.util.Map;

import org.jboss.as.model.test.ModelTestControllerVersion;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MessagingDependencies {

    private static final Map<ModelTestControllerVersion, String[]> ACTIVEMQ_DEPENDENCIES;

    static {
        ACTIVEMQ_DEPENDENCIES = Map.of(ModelTestControllerVersion.EAP_7_4_0, new String[] {
                "org.apache.activemq:artemis-commons:2.16.0.redhat-00022",
                "org.apache.activemq:artemis-journal:2.16.0.redhat-00022",
                "org.apache.activemq:artemis-server:2.16.0.redhat-00022",
                "org.apache.activemq:artemis-jms-server:2.16.0.redhat-00022",
                "org.apache.activemq:artemis-core-client:2.16.0.redhat-00022",
                "org.apache.activemq:artemis-jms-client:2.16.0.redhat-00022",
                "org.apache.activemq:artemis-ra:2.16.0.redhat-00022",
                "org.jboss.spec.javax.jms:jboss-jms-api_2.0_spec:2.0.0.Final",
        }, ModelTestControllerVersion.EAP_8_0_0, new String[] {
                "org.apache.activemq:artemis-commons:2.21.0.redhat-00045",
                "org.apache.activemq:artemis-journal:2.21.0.redhat-00045",
                "org.apache.activemq:artemis-server:2.21.0.redhat-00045",
                "org.apache.activemq:artemis-jakarta-server:2.21.0.redhat-00045",
                "org.apache.activemq:artemis-core-client:2.21.0.redhat-00045",
                "org.apache.activemq:artemis-jakarta-client:2.21.0.redhat-00045",
                "org.apache.activemq:artemis-jakarta-ra:2.21.0.redhat-00045",
                "jakarta.jms:jakarta.jms-api:3.1.0.redhat-00002",
        });
    }

    static String[] getActiveMQDependencies(ModelTestControllerVersion controllerVersion) {
        return ACTIVEMQ_DEPENDENCIES.get(controllerVersion);
    }

    static String[] getMessagingActiveMQGAV(ModelTestControllerVersion version) {
        if (version.isEap()) {
            if (ModelTestControllerVersion.EAP_8_0_0.equals(version)) {
                return new String[] {
                        version.createGAV("wildfly-messaging-activemq-subsystem"),
                        version.createGAV("wildfly-messaging-activemq-injection"),
                };
            }
            return new String[] {
                    version.createGAV("wildfly-messaging-activemq"),
            };
        }
        return new String[] {
                version.createGAV("wildfly-messaging-activemq"),
        };
    }

    static String[] getJGroupsDependencies(ModelTestControllerVersion version) {
        if (ModelTestControllerVersion.EAP_8_0_0.equals(version)) {
            return new String[] {
                    version.createGAV("wildfly-clustering-common"),
                    version.createGAV("wildfly-clustering-jgroups-api"),
                    version.createGAV("wildfly-clustering-jgroups-extension"),
                    version.createGAV("wildfly-clustering-jgroups-spi"),
                    version.createGAV("wildfly-clustering-server-service"),
                    version.createGAV("wildfly-clustering-server-api"),
                    version.createGAV("wildfly-clustering-server-spi"),
                    "org.jgroups:jgroups:5.2.18.Final-redhat-00001",
            };
        }
        return new String[] {
                version.createGAV("wildfly-clustering-common"),
                version.createGAV("wildfly-clustering-jgroups-api"),
                version.createGAV("wildfly-clustering-jgroups-extension"),
                version.createGAV("wildfly-clustering-jgroups-spi"),
                version.createGAV("wildfly-clustering-service"),
                version.createGAV("wildfly-clustering-api"),
                version.createGAV("wildfly-clustering-spi"),
                "org.jgroups:jgroups:3.6.12.Final-redhat-1",
        };
    }
}
