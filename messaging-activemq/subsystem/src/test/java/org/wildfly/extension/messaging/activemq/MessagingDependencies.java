/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.model.test.ModelTestControllerVersion;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MessagingDependencies {

    private static final Map<ModelTestControllerVersion, String[]> ACTIVEMQ_DEPENDENCIES;

    static {
        Map<ModelTestControllerVersion, String[]> map = new HashMap<>();
        map.put(ModelTestControllerVersion.EAP_7_4_0, new String[]{
            "org.apache.activemq:artemis-commons:2.16.0.redhat-00022",
            "org.apache.activemq:artemis-journal:2.16.0.redhat-00022",
            "org.apache.activemq:artemis-server:2.16.0.redhat-00022",
            "org.apache.activemq:artemis-jms-server:2.16.0.redhat-00022",
            "org.apache.activemq:artemis-core-client:2.16.0.redhat-00022",
            "org.apache.activemq:artemis-jms-client:2.16.0.redhat-00022",
            "org.apache.activemq:artemis-ra:2.16.0.redhat-00022",
            "org.jboss.spec.javax.jms:jboss-jms-api_2.0_spec:2.0.0.Final"
        });
        map.put(ModelTestControllerVersion.EAP_8_0_0, new String[]{
            "org.apache.activemq:artemis-commons:2.21.0.redhat-00045",
            "org.apache.activemq:artemis-journal:2.21.0.redhat-00045",
            "org.apache.activemq:artemis-server:2.21.0.redhat-00045",
            "org.apache.activemq:artemis-jakarta-server:2.21.0.redhat-00045",
            "org.apache.activemq:artemis-core-client:2.21.0.redhat-00045",
            "org.apache.activemq:artemis-jakarta-client:2.21.0.redhat-00045",
            "org.apache.activemq:artemis-jakarta-ra:2.21.0.redhat-00045",
            "jakarta.jms:jakarta.jms-api:3.1.0.redhat-00002"
        });
        ACTIVEMQ_DEPENDENCIES = Collections.unmodifiableMap(map);
    }

    static String[] getActiveMQDependencies(ModelTestControllerVersion controllerVersion) {
        return ACTIVEMQ_DEPENDENCIES.get(controllerVersion);
    }

    static String[] getMessagingActiveMQGAV(ModelTestControllerVersion version) {
        if (version.isEap()) {
            if(ModelTestControllerVersion.EAP_8_0_0.equals(version)) {
                return new String[]{
                    "org.jboss.eap:wildfly-messaging-activemq-subsystem:" + version.getMavenGavVersion(),
                    "org.jboss.eap:wildfly-messaging-activemq-injection:" + version.getMavenGavVersion(),
                };
            }
            return new String[]{"org.jboss.eap:wildfly-messaging-activemq:" + version.getMavenGavVersion()};
        }
        return new String[]{"org.wildfly:wildfly-messaging-activemq:" + version.getMavenGavVersion()};
    }

    static String[] getJGroupsDependencies(ModelTestControllerVersion version) {
        String groupId = version.isEap() ? "org.jboss.eap" : "org.wildfly";
        if(ModelTestControllerVersion.EAP_8_0_0.equals(version)) {
            return new String[]{
            String.format("%s:wildfly-clustering-common:%s", groupId, version.getMavenGavVersion()),
            String.format("%s:wildfly-clustering-jgroups-api:%s", groupId, version.getMavenGavVersion()),
            String.format("%s:wildfly-clustering-jgroups-extension:%s", groupId, version.getMavenGavVersion()),
            String.format("%s:wildfly-clustering-jgroups-spi:%s", groupId, version.getMavenGavVersion()),
            String.format("%s:wildfly-clustering-server-service:%s", groupId, version.getMavenGavVersion()),
            String.format("%s:wildfly-clustering-server-api:%s", groupId, version.getMavenGavVersion()),
            String.format("%s:wildfly-clustering-server-spi:%s", groupId, version.getMavenGavVersion()),
            "org.jgroups:jgroups:5.2.18.Final-redhat-00001",};
        }
        return new String[]{
            String.format("%s:wildfly-clustering-common:%s", groupId, version.getMavenGavVersion()),
            String.format("%s:wildfly-clustering-jgroups-api:%s", groupId, version.getMavenGavVersion()),
            String.format("%s:wildfly-clustering-jgroups-extension:%s", groupId, version.getMavenGavVersion()),
            String.format("%s:wildfly-clustering-jgroups-spi:%s", groupId, version.getMavenGavVersion()),
            String.format("%s:wildfly-clustering-service:%s", groupId, version.getMavenGavVersion()),
            String.format("%s:wildfly-clustering-api:%s", groupId, version.getMavenGavVersion()),
            String.format("%s:wildfly-clustering-spi:%s", groupId, version.getMavenGavVersion()),
            "org.jgroups:jgroups:3.6.12.Final-redhat-1",};
    }
}
