/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.deployment;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.ServiceNameProvider;

/**
 * @author Paul Ferraro
 */
public interface OpenAPIServiceNameProvider extends ServiceNameProvider {

    default String getPath() {
        return this.getServiceName().getSimpleName();
    }

    default ServiceName getHostServiceName() {
        return this.getServiceName().getParent();
    }
}
