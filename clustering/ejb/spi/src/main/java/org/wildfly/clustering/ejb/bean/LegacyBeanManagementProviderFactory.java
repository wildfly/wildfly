/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.bean;

/**
 * interface for obtaining BeanManagementProvider instances in the legacy case where no distributable-ejb subsystem is present.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
@Deprecated
public interface LegacyBeanManagementProviderFactory {
    BeanManagementProvider createBeanManagementProvider(String name, LegacyBeanManagementConfiguration config);
}
