/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

import static org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION;
import static org.jboss.as.webservices.util.ASHelper.getAnnotations;
import static org.jboss.as.webservices.util.ASHelper.getJaxwsDeployment;
import static org.jboss.as.webservices.util.ASHelper.getRequiredAttachment;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_PROVIDER_ANNOTATION;

import java.util.LinkedList;
import java.util.List;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.webservices.metadata.model.EJBEndpoint;
import org.jboss.as.webservices.metadata.model.JAXWSDeployment;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSIntegrationProcessorJAXWS_EJB implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        processAnnotation(unit, WEB_SERVICE_ANNOTATION);
        processAnnotation(unit, WEB_SERVICE_PROVIDER_ANNOTATION);
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
        // does nothing
    }

    private static void processAnnotation(final DeploymentUnit unit, final DotName annotation) {
        final List<AnnotationInstance> webServiceAnnotations = getAnnotations(unit, annotation);
        final EEModuleDescription moduleDescription = getRequiredAttachment(unit, EE_MODULE_DESCRIPTION);
        final JAXWSDeployment jaxwsDeployment = getJaxwsDeployment(unit);

        for (final AnnotationInstance webServiceAnnotation : webServiceAnnotations) {
            final AnnotationTarget target = webServiceAnnotation.target();
            final ClassInfo webServiceClassInfo = (ClassInfo) target;
            final String webServiceClassName = webServiceClassInfo.name().toString();
            final List<ComponentDescription> componentDescriptions = moduleDescription.getComponentsByClassName(webServiceClassName);
            final List<SessionBeanComponentDescription> sessionBeans = getSessionBeans(componentDescriptions);

            for (final SessionBeanComponentDescription sessionBean : sessionBeans) {
                if (sessionBean.isStateless() || sessionBean.isSingleton()) {
                    final EJBViewDescription ejbViewDescription = sessionBean.addWebserviceEndpointView();
                    final String ejbViewName = ejbViewDescription.getServiceName().getCanonicalName();
                    jaxwsDeployment.addEndpoint(new EJBEndpoint(sessionBean, webServiceClassInfo, ejbViewName));
                }
            }
        }
    }

    private static List<SessionBeanComponentDescription> getSessionBeans(final List<ComponentDescription> componentDescriptions) {
        final List<SessionBeanComponentDescription> sessionBeans = new LinkedList<SessionBeanComponentDescription>();
        for (final ComponentDescription componentDescription : componentDescriptions) {
            if (componentDescription instanceof SessionBeanComponentDescription) {
                sessionBeans.add((SessionBeanComponentDescription) componentDescription);
            }
        }
        return sessionBeans;
    }

}
