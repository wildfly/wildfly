/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;

/**
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public interface EESubsystemModel {

    String EAR_SUBDEPLOYMENTS_ISOLATED = "ear-subdeployments-isolated";
    String SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT = "spec-descriptor-property-replacement";
    String JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT = "jboss-descriptor-property-replacement";
    String ANNOTATION_PROPERTY_REPLACEMENT = "annotation-property-replacement";

    String DEFAULT_BINDINGS = "default-bindings";

    String CONTEXT_SERVICE = "context-service";
    String MANAGED_THREAD_FACTORY = "managed-thread-factory";
    String MANAGED_EXECUTOR_SERVICE = "managed-executor-service";
    String MANAGED_SCHEDULED_EXECUTOR_SERVICE = "managed-scheduled-executor-service";
    String SERVICE = "service";

    PathElement DEFAULT_BINDINGS_PATH = PathElement.pathElement(SERVICE,DEFAULT_BINDINGS);
    String GLOBAL_DIRECTORY = "global-directory";

    /**
     * Versions of the EE subsystem model
     */
    interface Version {
        ModelVersion v1_0_0 = ModelVersion.create(1, 0, 0); //EAP 6.2.0
        ModelVersion v1_1_0 = ModelVersion.create(1, 1, 0);
        ModelVersion v3_0_0 = ModelVersion.create(3, 0, 0);
        ModelVersion v4_0_0 = ModelVersion.create(4, 0, 0);
        ModelVersion v5_0_0 = ModelVersion.create(5, 0, 0);
        ModelVersion v6_0_0 = ModelVersion.create(6, 0, 0);
    }

}
