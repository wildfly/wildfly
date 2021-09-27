/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
        map.put(ModelTestControllerVersion.EAP_7_3_0, new String[] {
                "org.apache.activemq:artemis-commons:2.9.0.redhat-00005",
                "org.apache.activemq:artemis-journal:2.9.0.redhat-00005",
                "org.apache.activemq:artemis-server:2.9.0.redhat-00005",
                "org.apache.activemq:artemis-jms-server:2.9.0.redhat-00005",
                "org.apache.activemq:artemis-core-client:2.9.0.redhat-00005",
                "org.apache.activemq:artemis-jms-client:2.9.0.redhat-00005",
                "org.apache.activemq:artemis-ra:2.9.0.redhat-00005",
        });
        map.put(ModelTestControllerVersion.EAP_7_2_0, new String[] {
                "org.apache.activemq:artemis-commons:2.6.3.redhat-00014",
                "org.apache.activemq:artemis-journal:2.6.3.redhat-00014",
                "org.apache.activemq:artemis-server:2.6.3.redhat-00014",
                "org.apache.activemq:artemis-jms-server:2.6.3.redhat-00014",
                "org.apache.activemq:artemis-core-client:2.6.3.redhat-00014",
                "org.apache.activemq:artemis-jms-client:2.6.3.redhat-00014",
                "org.apache.activemq:artemis-ra:2.6.3.redhat-00014",
        });
        map.put(ModelTestControllerVersion.EAP_7_1_0, new String[] {
                "org.apache.activemq:artemis-commons:1.5.5.008-redhat-1",
                "org.apache.activemq:artemis-journal:1.5.5.008-redhat-1",
                "org.apache.activemq:artemis-server:1.5.5.008-redhat-1",
                "org.apache.activemq:artemis-jms-server:1.5.5.008-redhat-1",
                "org.apache.activemq:artemis-core-client:1.5.5.008-redhat-1",
                "org.apache.activemq:artemis-jms-client:1.5.5.008-redhat-1",
                "org.apache.activemq:artemis-ra:1.5.5.008-redhat-1",
        });

        map.put(ModelTestControllerVersion.EAP_7_0_0, new String[]{
                "org.apache.activemq:artemis-commons:1.1.0.SP16-redhat-1",
                "org.apache.activemq:artemis-journal:1.1.0.SP16-redhat-1",
                "org.apache.activemq:artemis-server:1.1.0.SP16-redhat-1",
                "org.apache.activemq:artemis-jms-server:1.1.0.SP16-redhat-1",
                "org.apache.activemq:artemis-core-client:1.1.0.SP16-redhat-1",
                "org.apache.activemq:artemis-jms-client:1.1.0.SP16-redhat-1",
                "org.apache.activemq:artemis-ra:1.1.0.SP16-redhat-1",
        });

        ACTIVEMQ_DEPENDENCIES = Collections.unmodifiableMap(map);
    }

    static String[] getActiveMQDependencies(ModelTestControllerVersion controllerVersion) {
        return ACTIVEMQ_DEPENDENCIES.get(controllerVersion);
    }

    static String getMessagingActiveMQGAV(ModelTestControllerVersion version) {
        if (version.isEap()) {
            return "org.jboss.eap:wildfly-messaging-activemq:" + version.getMavenGavVersion();
        }
        return "org.wildfly:wildfly-messaging-activemq:" + version.getMavenGavVersion();
    }

    static String[] getJGroupsDependencies(ModelTestControllerVersion version) {
        String groupId = version.isEap() ? "org.jboss.eap" : "org.wildfly";
        return new String[] {
                String.format("%s:wildfly-clustering-common:%s", groupId, version.getMavenGavVersion()),
                String.format("%s:wildfly-clustering-jgroups-api:%s", groupId, version.getMavenGavVersion()),
                String.format("%s:wildfly-clustering-jgroups-extension:%s", groupId, version.getMavenGavVersion()),
                String.format("%s:wildfly-clustering-jgroups-spi:%s", groupId, version.getMavenGavVersion()),
                String.format("%s:wildfly-clustering-service:%s", groupId, version.getMavenGavVersion()),
                "org.jgroups:jgroups:3.6.12.Final-redhat-1",
        };
    }
}
