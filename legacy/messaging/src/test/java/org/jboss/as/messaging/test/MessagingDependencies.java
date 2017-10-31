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
package org.jboss.as.messaging.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.model.test.ModelTestControllerVersion;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MessagingDependencies {

    private static final Map<ModelTestControllerVersion, String[]> HORNETQ_DEPENDENCIES;
    static {
        Map<ModelTestControllerVersion, String[]> map = new HashMap<ModelTestControllerVersion, String[]>();

        map.put(ModelTestControllerVersion.EAP_6_2_0, new String[] {
                "org.hornetq:hornetq-commons:2.3.12.Final-redhat-1",
                "org.hornetq:hornetq-journal:2.3.12.Final-redhat-1",
                "org.hornetq:hornetq-server:2.3.12.Final-redhat-1",
                "org.hornetq:hornetq-jms-server:2.3.12.Final-redhat-1",
                "org.hornetq:hornetq-core-client:2.3.12.Final-redhat-1",
                "org.hornetq:hornetq-jms-client:2.3.12.Final-redhat-1",
                "org.hornetq:hornetq-ra:2.3.12.Final-redhat-1",
        });

        map.put(ModelTestControllerVersion.EAP_6_3_0, new String[]{
                "org.hornetq:hornetq-commons:2.3.20.Final-redhat-1",
                "org.hornetq:hornetq-journal:2.3.20.Final-redhat-1",
                "org.hornetq:hornetq-server:2.3.20.Final-redhat-1",
                "org.hornetq:hornetq-jms-server:2.3.20.Final-redhat-1",
                "org.hornetq:hornetq-core-client:2.3.20.Final-redhat-1",
                "org.hornetq:hornetq-jms-client:2.3.20.Final-redhat-1",
                "org.hornetq:hornetq-ra:2.3.20.Final-redhat-1",
        });

        map.put(ModelTestControllerVersion.EAP_6_4_0, new String[]{
                "org.hornetq:hornetq-commons:2.3.25.Final-redhat-1",
                "org.hornetq:hornetq-journal:2.3.25.Final-redhat-1",
                "org.hornetq:hornetq-server:2.3.25.Final-redhat-1",
                "org.hornetq:hornetq-jms-server:2.3.25.Final-redhat-1",
                "org.hornetq:hornetq-core-client:2.3.25.Final-redhat-1",
                "org.hornetq:hornetq-jms-client:2.3.25.Final-redhat-1",
                "org.hornetq:hornetq-ra:2.3.25.Final-redhat-1",
        });

        HORNETQ_DEPENDENCIES = Collections.unmodifiableMap(map);
    }

    static String[] getHornetQDependencies(ModelTestControllerVersion controllerVersion) {
        return HORNETQ_DEPENDENCIES.get(controllerVersion);
    }

    static String getMessagingGAV(ModelTestControllerVersion version) {
        return version.getMavenGroupId() + ":jboss-as-messaging:" + version.getMavenGavVersion();
    }
}
