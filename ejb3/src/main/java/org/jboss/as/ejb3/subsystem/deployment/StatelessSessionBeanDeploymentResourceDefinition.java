/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem.deployment;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for a {@link org.jboss.as.ejb3.component.stateless.StatelessSessionComponent}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class StatelessSessionBeanDeploymentResourceDefinition extends AbstractEJBComponentResourceDefinition {

    public StatelessSessionBeanDeploymentResourceDefinition() {
        super(EJBComponentType.STATELESS);
    }
}
