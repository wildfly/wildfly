/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */
package org.jboss.as.subsystem.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.ModelVersion;
/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class KnownVersions {

    static final Map<String, ModelVersion> AS_CORE_MODEL_VERSION_BY_AS_VERSION;
    private static final Map<String, Map<ModelVersion, ModelVersion>> KNOWN_SUBSYSTEM_VERSIONS;
    static {
        Map<String, Map<ModelVersion, ModelVersion>> map = new HashMap<String, Map<ModelVersion,ModelVersion>>();

        //At the time of writing the main usage for this is to know if a given subsystem model version belongs to
        //the 7.1.x series or above. From 7.2.x the host registration process includes which resources are ignored,
        //meaning that resource transformers can fail (e.g. RejectExpressionValuesTransformer). In 7.1.x we will have
        //no idea so we need to log a warning instead.
        //The core model versions are 1.2.0 for AS 7.1.2 and 1.3.0 for AS 7.1.3
        //7.2.x starts on core model version 1.4.0

        //Keep this list in alphabetical and subsystem version order

        final String CORE_MODEL_7_1_2 = "1.2.0";
        final String CORE_MODEL_7_1_3 = "1.3.0";
        addSubsystemVersion(map, "configadmin", "1.0.0", CORE_MODEL_7_1_3);
        addSubsystemVersion(map, "jacorb", "1.1.0", CORE_MODEL_7_1_3);
        addSubsystemVersion(map, "ejb3", "1.1.0", CORE_MODEL_7_1_2);
        addSubsystemVersion(map, "infinispan", "1.3.0", CORE_MODEL_7_1_3);
        addSubsystemVersion(map, "jacorb", "1.1.0", CORE_MODEL_7_1_3);
        addSubsystemVersion(map, "jgroups", "1.1.0", CORE_MODEL_7_1_3);
        addSubsystemVersion(map, "jmx", "1.0.0", CORE_MODEL_7_1_2);
        addSubsystemVersion(map, "jmx", "1.1.0", CORE_MODEL_7_1_3);
        addSubsystemVersion(map, "jgroups", "1.1.0", CORE_MODEL_7_1_3);
        addSubsystemVersion(map, "jpa", "1.1.0", CORE_MODEL_7_1_3);
        addSubsystemVersion(map, "logging", "1.1.0", CORE_MODEL_7_1_3);
        addSubsystemVersion(map, "mail", "1.1.0", CORE_MODEL_7_1_3);
        addSubsystemVersion(map, "messaging", "1.1.0", CORE_MODEL_7_1_3);
        addSubsystemVersion(map, "modcluster", "1.2.0", CORE_MODEL_7_1_3);
        addSubsystemVersion(map, "naming", "1.1.0", CORE_MODEL_7_1_3);
        addSubsystemVersion(map, "osgi", "1.0.0", CORE_MODEL_7_1_3);
        addSubsystemVersion(map, "remoting", "1.1.0", CORE_MODEL_7_1_3);
        addSubsystemVersion(map, "security", "1.1.0", CORE_MODEL_7_1_2);
        addSubsystemVersion(map, "threads", "1.0.0", CORE_MODEL_7_1_2);
        addSubsystemVersion(map, "security", "1.1.0", CORE_MODEL_7_1_2);
        addSubsystemVersion(map, "remoting", "1.1.0", CORE_MODEL_7_1_3);
        addSubsystemVersion(map, "threads", "1.0.0", CORE_MODEL_7_1_2);
        addSubsystemVersion(map, "transactions", "1.1.0", CORE_MODEL_7_1_2);
        addSubsystemVersion(map, "transactions", "1.1.1", CORE_MODEL_7_1_3);
        addSubsystemVersion(map, "web", "1.1.0", CORE_MODEL_7_1_2);
        addSubsystemVersion(map, "web", "1.1.1", CORE_MODEL_7_1_3);
        addSubsystemVersion(map, "webservices", "1.1.0", CORE_MODEL_7_1_3);

        KNOWN_SUBSYSTEM_VERSIONS = Collections.unmodifiableMap(map);


        Map<String, ModelVersion> map2 = new HashMap<String, ModelVersion>();
        map2.put("7.1.2", ModelVersion.create(1, 2, 0));
        map2.put("7.1.3", ModelVersion.create(1, 3, 0));
        AS_CORE_MODEL_VERSION_BY_AS_VERSION = Collections.unmodifiableMap(map2);
    }



    static ModelVersion getCoreModelVersionForSubsystemVersion(String subsystemName, ModelVersion subsystemVersion) {
        Map<ModelVersion, ModelVersion> versionMap = KNOWN_SUBSYSTEM_VERSIONS.get(subsystemName);
        if (versionMap == null) {
            return null;
        }
        return versionMap.get(subsystemVersion);
    }


    private static void addSubsystemVersion(Map<String, Map<ModelVersion, ModelVersion>> map, String subsystem, String subsystemVersion, String coreVersion) {
        ModelVersion subsystemModelVersion = ModelVersion.fromString(subsystemVersion);
        ModelVersion coreModelVersion = ModelVersion.fromString(coreVersion);
        Map<ModelVersion, ModelVersion> versionMap = map.get(subsystem);
        if (versionMap == null) {
            versionMap = new HashMap<ModelVersion, ModelVersion>();
            map.put(subsystem, versionMap);
        }
        versionMap.put(subsystemModelVersion, coreModelVersion);
    }

}
