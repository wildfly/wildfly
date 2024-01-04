/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.container.interceptor;

/**
 * @author Jaikiran Pai
 */
public interface FlowTracker {

    String echo(String message);
}
