/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component.deployers;

import java.util.Set;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.component.EEApplicationDescription;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;

import static org.jboss.as.ee.component.Attachments.EE_APPLICATION_DESCRIPTION;

/**
 * Implementation of {@link org.jboss.as.ee.component.InjectionSource} responsible for finding a message destination
 *
 * @author Stuart Douglas
 */
public class MessageDestinationInjectionSource extends InjectionSource {
    private final String bindingName;
    private final String messageDestinationName;
    private volatile String resolvedLookupName;
    private volatile String error = null;

    public MessageDestinationInjectionSource(final String messageDestinationName, final String bindingName) {
        this.messageDestinationName = messageDestinationName;
        this.bindingName = bindingName;
    }

    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        if (error != null) {
            throw new DeploymentUnitProcessingException(error);
        }
        final String applicationName = resolutionContext.getApplicationName();
        final String moduleName = resolutionContext.getModuleName();
        final String componentName = resolutionContext.getComponentName();
        final boolean compUsesModule = resolutionContext.isCompUsesModule();
        final String lookupName;
        if (!this.resolvedLookupName.contains(":")) {
            if (componentName != null && !compUsesModule) {
                lookupName = "java:comp/env/" + this.resolvedLookupName;
            } else if (compUsesModule) {
                lookupName = "java:module/env/" + this.resolvedLookupName;
            } else {
                lookupName = "java:jboss/env" + this.resolvedLookupName;
            }
        } else if (this.resolvedLookupName.startsWith("java:comp/") && compUsesModule) {
            lookupName = "java:module/" + this.resolvedLookupName.substring(10);
        } else {
            lookupName = this.resolvedLookupName;
        }
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(applicationName, moduleName, componentName, lookupName);
        if (lookupName.startsWith("java:")) {
            serviceBuilder.addDependency(bindInfo.getBinderServiceName(), ManagedReferenceFactory.class, injector);
        }
    }

    public void resolve(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEApplicationDescription applicationDescription = deploymentUnit.getAttachment(EE_APPLICATION_DESCRIPTION);
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final Set<String> names = applicationDescription.resolveMessageDestination(messageDestinationName, deploymentRoot.getRoot());

        if (names.isEmpty()) {
            error = EeLogger.ROOT_LOGGER.noMessageDestination(messageDestinationName, bindingName);
            return;
        }
        if (names.size() > 1) {
            error = EeLogger.ROOT_LOGGER.moreThanOneMessageDestination(messageDestinationName, bindingName, names);
            return;
        }
        resolvedLookupName = names.iterator().next();
    }


    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof MessageDestinationInjectionSource))
            return false;
        if (error != null) {
            //we can't do a real equals comparison in this case, so throw the original error
            throw new RuntimeException(error);
        }
        if (resolvedLookupName == null) {
            throw EeLogger.ROOT_LOGGER.errorEqualsCannotBeCalledBeforeResolve();
        }

        final MessageDestinationInjectionSource other = (MessageDestinationInjectionSource) o;
        return eq(resolvedLookupName, other.resolvedLookupName);
    }

    public int hashCode() {
        return messageDestinationName.hashCode();
    }

    private static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}
