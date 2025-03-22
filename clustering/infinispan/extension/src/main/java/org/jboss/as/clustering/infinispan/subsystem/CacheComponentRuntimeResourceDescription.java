/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Function;

import org.infinispan.Cache;
import org.jboss.as.controller.ResourceRegistration;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * @author Paul Ferraro
 */
public interface CacheComponentRuntimeResourceDescription extends ResourceRegistration, Function<FunctionExecutorRegistry<Cache<?, ?>>, ManagementResourceRegistrar> {

}
