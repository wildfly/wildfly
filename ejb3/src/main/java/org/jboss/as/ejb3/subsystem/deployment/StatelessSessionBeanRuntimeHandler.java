/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem.deployment;

import org.jboss.as.ejb3.component.stateless.StatelessSessionComponent;

/**
 * Handles operations that provide runtime management of a {@link StatelessSessionComponent}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class StatelessSessionBeanRuntimeHandler extends AbstractEJBComponentRuntimeHandler<StatelessSessionComponent> {

    public static final StatelessSessionBeanRuntimeHandler INSTANCE = new StatelessSessionBeanRuntimeHandler();

    private StatelessSessionBeanRuntimeHandler() {
        super(EJBComponentType.STATELESS, StatelessSessionComponent.class);
    }
}
