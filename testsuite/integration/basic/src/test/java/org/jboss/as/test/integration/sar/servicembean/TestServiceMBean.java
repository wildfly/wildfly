/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.servicembean;

/**
 * MBean interface for {@link TestService}.
 *
 * @author Eduardo Martins
 */
public interface TestServiceMBean {

    int getState();
}
