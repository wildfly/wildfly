/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ejb3.subsystem.deployment;

import static org.jboss.as.ejb3.subsystem.deployment.StatefulSessionBeanDeploymentResourceDefinition.AFTER_BEGIN_METHOD;
import static org.jboss.as.ejb3.subsystem.deployment.StatefulSessionBeanDeploymentResourceDefinition.AFTER_COMPLETION_METHOD;
import static org.jboss.as.ejb3.subsystem.deployment.StatefulSessionBeanDeploymentResourceDefinition.BEAN_METHOD;
import static org.jboss.as.ejb3.subsystem.deployment.StatefulSessionBeanDeploymentResourceDefinition.BEFORE_COMPLETION_METHOD;
import static org.jboss.as.ejb3.subsystem.deployment.StatefulSessionBeanDeploymentResourceDefinition.PASSIVATION_CAPABLE;
import static org.jboss.as.ejb3.subsystem.deployment.StatefulSessionBeanDeploymentResourceDefinition.REMOVE_METHODS;
import static org.jboss.as.ejb3.subsystem.deployment.StatefulSessionBeanDeploymentResourceDefinition.RETAIN_IF_EXCEPTION;
import static org.jboss.as.ejb3.subsystem.deployment.StatefulSessionBeanDeploymentResourceDefinition.STATEFUL_TIMEOUT;

import java.lang.reflect.Method;
import java.util.Collection;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.dmr.ModelNode;
import org.jboss.invocation.proxy.MethodIdentifier;

/**
 * Handles operations that provide runtime management of a {@link StatefulSessionComponent}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class StatefulSessionBeanRuntimeHandler extends AbstractEJBComponentRuntimeHandler<StatefulSessionComponent> {

    public static final StatefulSessionBeanRuntimeHandler INSTANCE = new StatefulSessionBeanRuntimeHandler();

    private StatefulSessionBeanRuntimeHandler() {
        super(EJBComponentType.STATEFUL, StatefulSessionComponent.class);
    }

    @Override
    protected void executeReadAttribute(String attributeName, OperationContext context, StatefulSessionComponent component, PathAddress address) {
        final StatefulComponentDescription componentDescription = (StatefulComponentDescription) component.getComponentDescription();
        final ModelNode result = context.getResult();

        if (STATEFUL_TIMEOUT.getName().equals(attributeName)) {
            final StatefulTimeoutInfo statefulTimeout = componentDescription.getStatefulTimeout();
            if (statefulTimeout != null) {
                result.set(statefulTimeout.getValue() + ' ' + statefulTimeout.getTimeUnit().toString());
            }
        } else if (AFTER_BEGIN_METHOD.getName().equals(attributeName)) {
            final Method afterBeginMethod = component.getAfterBeginMethod();
            if (afterBeginMethod != null) {
                result.set(afterBeginMethod.toString());
            }
        } else if (BEFORE_COMPLETION_METHOD.getName().equals(attributeName)) {
            final Method beforeCompletionMethod = component.getBeforeCompletionMethod();
            if (beforeCompletionMethod != null) {
                result.set(beforeCompletionMethod.toString());
            }
        } else if (AFTER_COMPLETION_METHOD.getName().equals(attributeName)) {
            final Method afterCompletionMethod = component.getAfterCompletionMethod();
            if (afterCompletionMethod != null) {
                result.set(afterCompletionMethod.toString());
            }
        } else if (PASSIVATION_CAPABLE.getName().equals(attributeName)) {
            result.set(componentDescription.isPassivationApplicable());
        } else if (REMOVE_METHODS.getName().equals(attributeName)) {
            final Collection<StatefulComponentDescription.StatefulRemoveMethod> removeMethods = componentDescription.getRemoveMethods();
            for (StatefulComponentDescription.StatefulRemoveMethod removeMethod : removeMethods) {
                ModelNode removeMethodNode = result.add();
                final ModelNode beanMethodNode = removeMethodNode.get(BEAN_METHOD.getName());
                final MethodIdentifier methodIdentifier = removeMethod.getMethodIdentifier();
                beanMethodNode.set(methodIdentifier.getReturnType() + ' ' + methodIdentifier.getName() + '(' + String.join(", ", methodIdentifier.getParameterTypes()) + ')');
                final ModelNode retainIfExceptionNode = removeMethodNode.get(RETAIN_IF_EXCEPTION.getName());
                retainIfExceptionNode.set(removeMethod.getRetainIfException());
            }
        } else {
            super.executeReadAttribute(attributeName, context, component, address);
        }
        //TODO expose the cache
    }
}
