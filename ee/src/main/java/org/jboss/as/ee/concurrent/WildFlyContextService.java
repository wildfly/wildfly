/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import jakarta.enterprise.concurrent.ContextService;

/**
 *
 * @author Eduardo Martins
 */
public interface WildFlyContextService extends ContextService {

    ContextServiceTypesConfiguration getContextServiceTypesConfiguration();
}
