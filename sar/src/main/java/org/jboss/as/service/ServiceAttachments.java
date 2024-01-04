/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.service;

import java.util.Map;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.service.component.ServiceComponentInstantiator;

/**
 * @author Eduardo Martins
 */
public class ServiceAttachments {

    public static final AttachmentKey<Map<String, ServiceComponentInstantiator>> SERVICE_COMPONENT_INSTANTIATORS = AttachmentKey
            .<Map<String, ServiceComponentInstantiator>> create(Map.class);

    private ServiceAttachments() {

    }
}
