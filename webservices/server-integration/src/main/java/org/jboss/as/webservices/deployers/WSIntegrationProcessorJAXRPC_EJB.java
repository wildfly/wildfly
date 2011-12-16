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
import static org.jboss.as.ee.component.interceptors.InterceptorOrder.Component.WS_HANDLERS_INTERCEPTOR;
import static org.jboss.as.webservices.util.ASHelper.getJaxrpcDeployment;
import static org.jboss.as.webservices.util.ASHelper.getJaxwsEjbs;
import static org.jboss.as.webservices.util.ASHelper.getOptionalAttachment;
import static org.jboss.as.webservices.util.ASHelper.getRequiredAttachment;
import static org.jboss.as.webservices.util.WSAttachmentKeys.WEBSERVICES_METADATA_KEY;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.rpc.handler.MessageContext;
import javax.xml.rpc.handler.soap.SOAPMessageContext;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.webservices.metadata.model.EJBEndpoint;
import org.jboss.as.webservices.metadata.model.JAXRPCDeployment;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.msc.service.ServiceName;
import org.jboss.wsf.spi.invocation.HandlerCallback;
import org.jboss.wsf.spi.invocation.Invocation;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData.HandlerType;
import org.jboss.wsf.spi.metadata.webservices.PortComponentMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebserviceDescriptionMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSIntegrationProcessorJAXRPC_EJB implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (isJaxwsEjbDeployment(unit)) return;
        final EjbJarMetaData ejbJarMD = getOptionalAttachment(unit, EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        final WebservicesMetaData webservicesMD = getOptionalAttachment(unit, WEBSERVICES_METADATA_KEY);
        if (ejbJarMD != null && webservicesMD != null) {
            final EEModuleDescription moduleDescription = getRequiredAttachment(unit, EE_MODULE_DESCRIPTION);
            createJaxrpcDeployment(unit, webservicesMD, moduleDescription);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
        // does nothing
    }

    private static boolean isJaxwsEjbDeployment(final DeploymentUnit unit) {
        return getJaxwsEjbs(unit).size() > 0;
    }

    private static void createJaxrpcDeployment(final DeploymentUnit unit, final WebservicesMetaData webservicesMD, final EEModuleDescription moduleDescription) {
        final JAXRPCDeployment jaxrpcDeployment = getJaxrpcDeployment(unit);
        final Set<String> securityRoles = getSecurityRoles(unit);

        for (final WebserviceDescriptionMetaData wsDescriptionMD : webservicesMD.getWebserviceDescriptions()) {
            for (final PortComponentMetaData portComponentMD : wsDescriptionMD.getPortComponents()) {
                final EJBEndpoint ejbEndpoint = newEjbEndpoint(portComponentMD, moduleDescription, securityRoles);
                jaxrpcDeployment.addEndpoint(ejbEndpoint);
            }
        }
    }

    private static EJBEndpoint newEjbEndpoint(final PortComponentMetaData portComponentMD, final EEModuleDescription moduleDescription, final Set<String> securityRoles) {
        final String ejbName = portComponentMD.getEjbLink();
        final SessionBeanComponentDescription sessionBean = (SessionBeanComponentDescription)moduleDescription.getComponentByName(ejbName);
        final String seiIfaceClassName = portComponentMD.getServiceEndpointInterface();
        final EJBViewDescription ejbViewDescription = sessionBean.addWebserviceEndpointView(seiIfaceClassName);
        // JSR 109 - Version 1.3 - 6.2.2.4 Security
        // For EJB based service implementations, Handlers run after method level authorization has occurred.
        // JSR 109 - Version 1.3 - 6.2.2.5 Transaction
        // Handlers run under the transaction context of the component they are associated with.
        sessionBean.getConfigurators().addLast(new JAXRPCHandlersConfigurator());
        final ServiceName ejbViewName = ejbViewDescription.getServiceName();

        return new EJBEndpoint(sessionBean, ejbViewName, securityRoles, null, false, null);
    }

    private static Set<String> getSecurityRoles(final DeploymentUnit unit) {
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

        return (securityRoles.size() > 0) ? Collections.unmodifiableSet(securityRoles) : Collections.<String>emptySet();
    }

    private static final class JAXRPCHandlersConfigurator implements ComponentConfigurator {
        @Override
        public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
            configuration.addComponentInterceptor(new ImmediateInterceptorFactory(JAXRPCHandlersInterceptor.SINGLETON), WS_HANDLERS_INTERCEPTOR, true);
        }
    }

    private static final class JAXRPCHandlersInterceptor implements Interceptor {

        private static final Interceptor SINGLETON = new JAXRPCHandlersInterceptor();

        private JAXRPCHandlersInterceptor() {}

        @Override
        public Object processInvocation(final InterceptorContext context) throws Exception {
            final SOAPMessageContext msgContext = (SOAPMessageContext) context.getPrivateData(MessageContext.class);
            final Invocation wsInvocation = (Invocation) context.getPrivateData(Invocation.class);
            final HandlerCallback callback = (HandlerCallback) context.getPrivateData(HandlerCallback.class);
            if (msgContext == null || callback == null || wsInvocation == null) {
                // not for us
                return context.proceed();
            }

            // Handlers need to be Tx. Therefore we must invoke the handler chain after the TransactionInterceptor.
            try {
                // call the request handlers
                boolean handlersPass = callback.callRequestHandlerChain(wsInvocation, HandlerType.ENDPOINT);
                handlersPass = handlersPass && callback.callRequestHandlerChain(wsInvocation, HandlerType.POST);

                // Call the next interceptor in the chain
                if (handlersPass) {
                    // The SOAPContentElements stored in the EndpointInvocation might have changed after
                    // handler processing. Get the updated request payload. This should be a noop if request
                    // handlers did not modify the incomming SOAP message.
                    final Object[] reqParams = wsInvocation.getArgs();
                    context.setParameters(reqParams);
                    final Object resObj = context.proceed();

                    // Setting the message to null should trigger binding of the response message
                    msgContext.setMessage(null);
                    wsInvocation.setReturnValue(resObj);
                }

                // call the response handlers
                handlersPass = callback.callResponseHandlerChain(wsInvocation, HandlerType.POST);
                handlersPass = handlersPass && callback.callResponseHandlerChain(wsInvocation, HandlerType.ENDPOINT);

                // update the return value after response handler processing
                return wsInvocation.getReturnValue();
            }
            catch (final Exception ex) {
                try {
                    // call the fault handlers
                    boolean handlersPass = callback.callFaultHandlerChain(wsInvocation, HandlerType.POST, ex);
                    handlersPass = handlersPass && callback.callFaultHandlerChain(wsInvocation, HandlerType.ENDPOINT, ex);
                }
                catch (Exception ignore) {}
                throw ex;
            }
        }

    }

}
