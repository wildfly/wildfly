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

package org.jboss.as.webservices.deployers;

import static org.jboss.as.ee.component.Attachments.EE_APPLICATION_CLASSES_DESCRIPTION;
import static org.jboss.as.ee.structure.DeploymentType.WAR;
import static org.jboss.as.ee.structure.DeploymentTypeMarker.isType;
import static org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX;
import static org.jboss.as.server.deployment.Attachments.SUB_DEPLOYMENTS;
import static org.jboss.as.webservices.util.ASHelper.getJaxwsEjbs;
import static org.jboss.as.webservices.util.ASHelper.getJaxwsServlets;
import static org.jboss.as.webservices.util.ASHelper.isWebServiceDeployment;
import static org.jboss.as.webservices.util.ASHelper.getRequiredAttachment;
import static org.jboss.as.webservices.util.ASHelper.getOptionalAttachment;
import static org.jboss.as.webservices.util.WSAttachmentKeys.WS_ENDPOINT_HANDLERS_MAPPING_KEY;
import static org.jboss.as.webservices.util.WSAttachmentKeys.WS_DEPLOYMENT_TYPE_KEY;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.jboss.as.ee.component.ClassConfigurator;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.webservices.injection.WSEndpointHandlersMapping;
import org.jboss.as.webservices.metadata.WebServiceDeclaration;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.wsf.spi.deployment.Deployment.DeploymentType;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSJndiBindingsDeploymentUnitProcessor implements DeploymentUnitProcessor {

    private static final DotName JAVA_OBJECT_NAME = DotName.createSimple(Object.class.getName());

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (!isWebServiceDeployment(unit))
            return;

        final Set<String> jaxwsEndpoints = getJAXWSEndpoints(unit);
        for (final String jaxwsEndpoint : jaxwsEndpoints) {
            setupJNDIBindings(jaxwsEndpoint, unit);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
        // noop
    }

    private static Set<String> getJAXWSEndpoints(final DeploymentUnit unit) {
        final Set<String> endpoints = new HashSet<String>();
        final DeploymentType wsDeploymentType = getRequiredAttachment(unit, WS_DEPLOYMENT_TYPE_KEY);
        if (DeploymentType.JAXWS_EJB3 == wsDeploymentType) {
            // collect EJB3 WS endpoints
            final List<WebServiceDeclaration> ejb3Endpoints = getJaxwsEjbs(unit);
            for (final WebServiceDeclaration endpoint : ejb3Endpoints) {
                endpoints.add(endpoint.getComponentClassName());
            }
        }
        if (DeploymentType.JAXWS_JSE == wsDeploymentType) {
            // collect POJO WS endpoints
            final List<ServletMetaData> pojoEndpoints = getJaxwsServlets(unit);
            for (final ServletMetaData endpoint : pojoEndpoints) {
                endpoints.add(endpoint.getServletClass());
            }
        }
        // all known JAXWS endpoints
        return endpoints;
    }

    private static void setupJNDIBindings(final String endpointBean, final DeploymentUnit unit)
            throws DeploymentUnitProcessingException {
        final DotName endpointBeanName = DotName.createSimple(endpointBean);
        // propagate inherited JNDI bindings
        final Queue<DotName> predecessors = getPredecessors(endpointBeanName, unit, false);
        propagateBindingsAcrossDeployments(endpointBeanName, predecessors, unit);
        // propagate handler JNDI bindings
        final Set<String> handlers = getHandlers(endpointBean, unit);
        for (final String handler : handlers) {
            final DotName handlerBeanName = DotName.createSimple(handler);
            final Queue<DotName> handlerAndItsPredecessors = getPredecessors(handlerBeanName, unit, true);
            propagateBindingsAcrossDeployments(endpointBeanName, handlerAndItsPredecessors, unit);
        }
    }

    private static Set<String> getHandlers(final String endpointClass, final DeploymentUnit unit) {
        final WSEndpointHandlersMapping mapping = getOptionalAttachment(unit, WS_ENDPOINT_HANDLERS_MAPPING_KEY);
        final Set<String> handlers = mapping != null ? mapping.getHandlers(endpointClass) : null;
        return (handlers != null) ? handlers : Collections.<String>emptySet();
    }

    private static Queue<DotName> getPredecessors(final DotName childName, final DeploymentUnit unit, final boolean includeChild) {
        final Queue<DotName> predecessors = new LinkedList<DotName>();
        if (includeChild) {
            predecessors.add(childName);
        }
        DotName parentName = getParent(childName, unit);
        while (parentName != null) {
            predecessors.add(parentName);
            parentName = getParent(parentName, unit);
        }
        return predecessors;
    }

    private static void propagateBindingsAcrossDeployments(final DotName endpointName, final Collection<DotName> predecessors, final DeploymentUnit unit) {
        if (predecessors.size() == 0) return;
        final EEModuleClassDescription sessionBeanDescription = getDescription(endpointName, unit, true);
        DeploymentUnit predecessorUnit = null;
        EEModuleClassDescription predecessorDescription = null;
        for (final DotName predecessorName : predecessors) {
            predecessorUnit = getUnit(predecessorName, unit);
            if (predecessorUnit != null) {
                predecessorDescription = getDescription(predecessorName, predecessorUnit, false);
                if (predecessorDescription != null) {
                    final Deque<ClassConfigurator> predecessorConfigurators = predecessorDescription.getConfigurators();
                    sessionBeanDescription.getConfigurators().addAll(predecessorConfigurators);
                }
            }
        }
    }

    private static EEModuleClassDescription getDescription(final DotName dotName, final DeploymentUnit unit, final boolean addClass) {
        final EEApplicationClasses applicationClasses = unit.getAttachment(EE_APPLICATION_CLASSES_DESCRIPTION);
        if (addClass) {
            return applicationClasses.getOrAddClassByName(dotName.toString());
        } else {
            return applicationClasses.getClassByName(dotName.toString());
        }
    }

    private static DeploymentUnit getUnit(final DotName dotName, final DeploymentUnit unit) {
        final ClassInfo classInfo = getClassInfo(dotName, unit);
        if (classInfo != null) {
            return unit;
        } else {
            ClassInfo dotClass = null;
            for (final DeploymentUnit neighbourUnit : getNeighbours(unit)) {
                dotClass = getClassInfo(dotName, neighbourUnit);
                if (dotClass != null) {
                    return neighbourUnit;
                }
            }
        }

        return null;
    }

    private static DotName getParent(final DotName child, final DeploymentUnit unit) {
        ClassInfo childClass = getClassInfo(child, unit);
        if (childClass != null) {
            final DotName parentName = childClass.superName();
            if (JAVA_OBJECT_NAME.equals(parentName))
                return null;
            return parentName;
        }

        for (final DeploymentUnit neighbourUnit : getNeighbours(unit)) {
            childClass = getClassInfo(child, neighbourUnit);
            if (childClass != null) {
                final DotName parentName = childClass.superName();
                if (JAVA_OBJECT_NAME.equals(parentName))
                    return null;
                return parentName;
            }
        }

        return null;
    }

    private static ClassInfo getClassInfo(final DotName className, final DeploymentUnit unit) {
        final CompositeIndex index = unit.getAttachment(COMPOSITE_ANNOTATION_INDEX);
        return index != null ? index.getClassByName(className) : null;
    }

    private static List<DeploymentUnit> getNeighbours(final DeploymentUnit unit) {
        final DeploymentUnit parent = unit.getParent();
        if (parent == null)
            return Collections.emptyList();

        final AttachmentList<DeploymentUnit> neighbours = parent.getAttachment(SUB_DEPLOYMENTS);
        final List<DeploymentUnit> retVal = new AttachmentList<DeploymentUnit>(DeploymentUnit.class);
        for (final DeploymentUnit neighbour : neighbours) {
            if (neighbour == unit)
                continue;
            if (isType(WAR, neighbour))
                continue;
            retVal.add(neighbour);
        }

        return retVal;
    }

}
