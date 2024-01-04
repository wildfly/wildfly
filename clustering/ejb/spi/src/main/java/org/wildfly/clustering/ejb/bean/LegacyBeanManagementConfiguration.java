/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.bean;

/**
 * Configuration for legacy bean management.
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyBeanManagementConfiguration extends BeanPassivationConfiguration {
    String DEFAULT_CONTAINER_NAME = "ejb";

    String getContainerName();
    String getCacheName();
}
