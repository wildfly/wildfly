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
import static org.jboss.as.webservices.util.ASHelper.getJaxwsDeployment;
import static org.jboss.as.webservices.util.ASHelper.getRequiredAttachment;
import static org.jboss.as.webservices.util.DotNames.DECLARE_ROLES_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.PERMIT_ALL_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.ROLES_ALLOWED_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.WEB_CONTEXT_ANNOTATION;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jws.WebService;
import javax.xml.ws.WebServiceProvider;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.webservices.metadata.model.EJBEndpoint;
import org.jboss.as.webservices.metadata.model.JAXWSDeployment;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 *
 */
public final class WSIntegrationProcessorJAXWS_EJB implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        processAnnotation(unit, WebService.class);
        processAnnotation(unit, WebServiceProvider.class);
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
        // does nothing
    }
    @SuppressWarnings("rawtypes")
    private static void processAnnotation(final DeploymentUnit unit,  final Class annotationType) {

        final EEModuleDescription moduleDescription = getRequiredAttachment(unit, EE_MODULE_DESCRIPTION);
        final JAXWSDeployment jaxwsDeployment = getJaxwsDeployment(unit);
        for (EEModuleClassDescription description : moduleDescription.getClassDescriptions()) {
            @SuppressWarnings("unchecked")
            ClassAnnotationInformation classAnnotationInfo = description.getAnnotationInformation(annotationType);
            if (classAnnotationInfo != null && !classAnnotationInfo.getClassLevelAnnotations().isEmpty()) {
                Object obj = classAnnotationInfo.getClassLevelAnnotations().get(0);
                AnnotationTarget target = null;
                if (obj instanceof WebServiceAnnotationInfo) {
                    target = ((WebServiceAnnotationInfo)obj).getTarget();
                } else if (obj instanceof WebServiceProviderAnnotationInfo) {
                    target = ((WebServiceProviderAnnotationInfo)obj).getTarget();
                } else {
                    return;
                }
                final ClassInfo webServiceClassInfo = (ClassInfo) target;
                final String webServiceClassName = webServiceClassInfo.name().toString();
                final List<ComponentDescription> componentDescriptions = moduleDescription.getComponentsByClassName(webServiceClassName);
                final List<SessionBeanComponentDescription> sessionBeans = getSessionBeans(componentDescriptions);
                final Set<String> securityRoles = getDeclaredSecurityRoles(unit, webServiceClassInfo); // TODO: assembly processed for each endpoint!
                final WebContextAnnotationWrapper webCtx = getWebContextWrapper(webServiceClassInfo);
                final String authMethod = webCtx.getAuthMethod();
                final boolean isSecureWsdlAccess = webCtx.isSecureWsdlAccess();
                final String transportGuarantee = webCtx.getTransportGuarantee();
                final String realmName = webCtx.getRealmName();
                for (final SessionBeanComponentDescription sessionBean : sessionBeans) {
                    if (sessionBean.isStateless() || sessionBean.isSingleton()) {
                        final EJBViewDescription ejbViewDescription = sessionBean.addWebserviceEndpointView();
                        final ServiceName ejbViewName = ejbViewDescription.getServiceName();
                        jaxwsDeployment.addEndpoint(new EJBEndpoint(sessionBean, ejbViewName, securityRoles, authMethod, realmName, isSecureWsdlAccess, transportGuarantee));
                    }
                }
            }
        }
    }

    private static WebContextAnnotationWrapper getWebContextWrapper(final ClassInfo webServiceClassInfo) {
        if (!webServiceClassInfo.annotations().containsKey(WEB_CONTEXT_ANNOTATION)) return new WebContextAnnotationWrapper(null);
        final AnnotationInstance webContextAnnotation = webServiceClassInfo.annotations().get(WEB_CONTEXT_ANNOTATION).get(0);
        return new WebContextAnnotationWrapper(webContextAnnotation);
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

    private static Set<String> getDeclaredSecurityRoles(final DeploymentUnit unit, final ClassInfo webServiceClassInfo) {
        final Set<String> securityRoles = new HashSet<String>();

        // process assembly-descriptor DD section
        final EjbJarMetaData ejbJarMD = unit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (ejbJarMD != null && ejbJarMD.getAssemblyDescriptor() != null) {
            final List<SecurityRoleMetaData> securityRoleMetaDatas = ejbJarMD.getAssemblyDescriptor().getAny(SecurityRoleMetaData.class);
            if (securityRoleMetaDatas != null) {
                for (final SecurityRoleMetaData securityRoleMetaData : securityRoleMetaDatas) {
                    securityRoles.add(securityRoleMetaData.getRoleName());
                }
            }
            final SecurityRolesMetaData securityRolesMD = ejbJarMD.getAssemblyDescriptor().getSecurityRoles();
            if (securityRolesMD != null && securityRolesMD.size() > 0) {
                for (final SecurityRoleMetaData securityRoleMD : securityRolesMD) {
                    securityRoles.add(securityRoleMD.getRoleName());
                }
            }
        }

        // process @RolesAllowed annotation
        if (webServiceClassInfo.annotations().containsKey(ROLES_ALLOWED_ANNOTATION)) {
            final List<AnnotationInstance> allowedRoles = webServiceClassInfo.annotations().get(ROLES_ALLOWED_ANNOTATION);
            for (final AnnotationInstance allowedRole : allowedRoles) {
                if (allowedRole.target().equals(webServiceClassInfo)) {
                   for (final String roleName : allowedRole.value().asStringArray()) {
                      securityRoles.add(roleName);
                   }
                }
            }
        }

        // process @DeclareRoles annotation
        if (webServiceClassInfo.annotations().containsKey(DECLARE_ROLES_ANNOTATION)) {
            final List<AnnotationInstance> declareRoles = webServiceClassInfo.annotations().get(DECLARE_ROLES_ANNOTATION);
            for (final AnnotationInstance declareRole : declareRoles) {
                if (declareRole.target().equals(webServiceClassInfo)) {
                   for (final String roleName : declareRole.value().asStringArray()) {
                      securityRoles.add(roleName);
                   }
                }
            }
        }

        // process @PermitAll annotation
        if (webServiceClassInfo.annotations().containsKey(PERMIT_ALL_ANNOTATION)) {
            for (AnnotationInstance permitAll : webServiceClassInfo.annotations().get(PERMIT_ALL_ANNOTATION)) {
                if (permitAll.target().equals(webServiceClassInfo)) {
                    securityRoles.add("*");
                    break;
                }
            }
        }
        //if there is no class level security annotation, it will delegate to ejb's security check
        if (securityRoles.isEmpty()) {
            securityRoles.add("*");
        }
        return Collections.unmodifiableSet(securityRoles);
    }

    private static final class WebContextAnnotationWrapper {
        private final String authMethod;
        private final String transportGuarantee;
        private final boolean secureWsdlAccess;
        private final String realmName;

        WebContextAnnotationWrapper(final AnnotationInstance annotation) {
            authMethod = stringValueOrNull(annotation, "authMethod");
            transportGuarantee = stringValueOrNull(annotation, "transportGuarantee");
            realmName = stringValueOrNull(annotation, "realmName");
            secureWsdlAccess = booleanValue(annotation, "secureWSDLAccess");
        }

        String getAuthMethod() {
            return authMethod;
        }

        String getTransportGuarantee() {
            return transportGuarantee;
        }

        boolean isSecureWsdlAccess() {
            return secureWsdlAccess;
        }

        String getRealmName() {
           return realmName;
        }

        private String stringValueOrNull(final AnnotationInstance annotation, final String attribute) {
            if (annotation == null) return null;
            final AnnotationValue value = annotation.value(attribute);
            return value != null ? value.asString() : null;
        }

        private boolean booleanValue(final AnnotationInstance annotation, final String attribute) {
            if (annotation == null) return false;
            final AnnotationValue value = annotation.value(attribute);
            return value != null ? value.asBoolean() : false;
        }
    }

}
