/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.appclient.deployment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.appclient.component.ApplicationClientComponentDescription;
import org.jboss.as.appclient.logging.AppClientMessages;
import org.jboss.as.appclient.service.ApplicationClientDeploymentService;
import org.jboss.as.appclient.service.ApplicationClientStartService;
import org.jboss.as.appclient.service.DefaultApplicationClientCallbackHandler;
import org.jboss.as.appclient.service.RealmCallbackWrapper;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentClassIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.metadata.appclient.spec.ApplicationClientMetaData;
import org.jboss.modules.Module;

import static org.jboss.as.appclient.logging.AppClientMessages.MESSAGES;

/**
 * Processor that starts an application client deployment
 *
 * @author Stuart Douglas
 */
public class ApplicationClientStartProcessor implements DeploymentUnitProcessor {

    private final String[] parameters;
    private final String hostUrl;
    private final String connectionPropertiesUrl;

    public ApplicationClientStartProcessor(final String hostUrl, final String connectionPropertiesUrl, final String[] parameters) {
        this.hostUrl = hostUrl;
        this.parameters = parameters;
        this.connectionPropertiesUrl = connectionPropertiesUrl;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final ApplicationClientMetaData appClientData = deploymentUnit.getAttachment(AppClientAttachments.APPLICATION_CLIENT_META_DATA);
        final DeploymentReflectionIndex deploymentReflectionIndex = deploymentUnit.getAttachment(Attachments.REFLECTION_INDEX);
        final DeploymentClassIndex classIndex = deploymentUnit.getAttachment(Attachments.CLASS_INDEX);

        //setup the callback handler
        final CallbackHandler callbackHandler;
        if (appClientData != null && appClientData.getCallbackHandler() != null && !appClientData.getCallbackHandler().isEmpty()) {
            try {
                final Class<?> callbackClass = classIndex.classIndex(appClientData.getCallbackHandler()).getModuleClass();
                callbackHandler = new RealmCallbackWrapper((CallbackHandler) callbackClass.newInstance());
            } catch (ClassNotFoundException e) {
                throw AppClientMessages.MESSAGES.couldNotLoadCallbackClass(appClientData.getCallbackHandler());
            } catch (Exception e) {
                throw AppClientMessages.MESSAGES.couldNotCreateCallbackHandler(appClientData.getCallbackHandler());
            }
        } else {
            callbackHandler = new DefaultApplicationClientCallbackHandler();
        }

        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        Boolean activate = deploymentUnit.getAttachment(AppClientAttachments.START_APP_CLIENT);
        if (activate == null || !activate) {
            return;
        }
        final Class<?> mainClass = deploymentUnit.getAttachment(AppClientAttachments.MAIN_CLASS);
        if (mainClass == null) {
            throw MESSAGES.cannotStartAppClient(deploymentUnit.getName());
        }
        final ApplicationClientComponentDescription component = deploymentUnit.getAttachment(AppClientAttachments.APPLICATION_CLIENT_COMPONENT);

        ClassReflectionIndex<?> index = deploymentReflectionIndex.getClassIndex(mainClass);
        Method method = index.getMethod(void.class, "main", String[].class);
        if (method == null) {
            throw MESSAGES.cannotStartAppClient(deploymentUnit.getName(), mainClass);
        }
        final ApplicationClientStartService startService;


        final List<SetupAction> setupActions = deploymentUnit.getAttachmentList(org.jboss.as.ee.component.Attachments.OTHER_EE_SETUP_ACTIONS);

        if (connectionPropertiesUrl != null) {
            EJBClientConfiguration configuration;
            try {
                final File file = new File(connectionPropertiesUrl);
                final URL url;
                if (file.exists()) {
                    url = file.toURI().toURL();
                } else {
                    url = new URL(connectionPropertiesUrl);
                }
                Properties properties = new Properties();
                InputStream stream = null;
                try {
                    stream = url.openStream();
                    properties.load(stream);
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            //ignore
                        }
                    }
                }
                final ClassLoader oldTccl = SecurityActions.getContextClassLoader();
                try {
                    SecurityActions.setContextClassLoader(module.getClassLoader());
                    configuration = new PropertiesBasedEJBClientConfiguration(properties);

                    //if there is no username or callback handler specified in the ejb-client properties file
                    //we override the default
                    if (!properties.contains("username") && !properties.contains("callback.handler.class")) {
                        //no security config so we wrap the configuration
                        configuration = new ForwardingEjbClientConfiguration(configuration) {
                            @Override
                            public CallbackHandler getCallbackHandler() {
                                return callbackHandler;
                            }
                        };
                    }

                    startService = new ApplicationClientStartService(method, parameters, moduleDescription.getNamespaceContextSelector(), module.getClassLoader(), setupActions, configuration);
                } finally {
                    SecurityActions.setContextClassLoader(oldTccl);
                }
            } catch (Exception e) {
                throw AppClientMessages.MESSAGES.exceptionLoadingEjbClientPropertiesURL(connectionPropertiesUrl, e);
            }
        } else {

            startService = new ApplicationClientStartService(method, parameters, moduleDescription.getNamespaceContextSelector(), module.getClassLoader(), setupActions, hostUrl, callbackHandler);
        }

        phaseContext.getServiceTarget()
                .addService(deploymentUnit.getServiceName().append(ApplicationClientStartService.SERVICE_NAME), startService)
                .addDependency(ApplicationClientDeploymentService.SERVICE_NAME, ApplicationClientDeploymentService.class, startService.getApplicationClientDeploymentServiceInjectedValue())
                .addDependency(component.getCreateServiceName(), Component.class, startService.getApplicationClientComponent())
                .install();
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
