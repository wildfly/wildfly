/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.deployment.processors;

import java.util.Collections;
import java.util.Properties;

import org.jboss.as.ee.metadata.EJBClientDescriptorMetaData;
import org.jboss.as.ee.structure.Attachments;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.EJBClientContextService;
import org.jboss.as.ejb3.remote.LocalTransportProvider;
import org.jboss.as.ejb3.remote.RemotingProfileService;
import org.jboss.as.ejb3.subsystem.EJBClientConfiguratorService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.ejb.client.EJBTransportProvider;
import org.jboss.modules.Module;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.xnio.Option;
import org.xnio.OptionMap;

/**
 * A deployment unit processor which processing only top level deployment units and checks for the presence of a
 * {@link Attachments#EJB_CLIENT_METADATA} key corresponding to {@link EJBClientDescriptorMetaData}, in the deployment unit.
 * <p/>
 * If a {@link EJBClientDescriptorMetaData} is available then this deployment unit processor creates and installs a
 * {@link EJBClientContextService}.
 *
 * TODO Elytron emulate old configuration using discovery, clustering
 *
 * @author Jaikiran Pai
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class EJBClientDescriptorMetaDataProcessor implements DeploymentUnitProcessor {

    private static final String INTERNAL_REMOTING_PROFILE = "internal-remoting-profile";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        // we only process top level deployment units
        if (deploymentUnit.getParent() != null) {
            return;
        }
        final EJBClientDescriptorMetaData ejbClientDescriptorMetaData = deploymentUnit
                .getAttachment(Attachments.EJB_CLIENT_METADATA);
        // no explicit EJB client configuration in this deployment, so nothing to do
        if (ejbClientDescriptorMetaData == null) {
            return;
        }
        // profile and remoting-ejb-receivers cannot be used together
        checkDescriptorConfiguration(ejbClientDescriptorMetaData);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if (module == null) {
            return;
        }
        // install the descriptor based EJB client context service
        final ServiceName ejbClientContextServiceName = EJBClientContextService.DEPLOYMENT_BASE_SERVICE_NAME.append(deploymentUnit.getName());
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        // create the service
        final EJBClientContextService service = new EJBClientContextService();
        // add the service
        final ServiceBuilder<EJBClientContextService> serviceBuilder = serviceTarget.addService(ejbClientContextServiceName, service);

        serviceBuilder.addDependency(EJBClientConfiguratorService.SERVICE_NAME, EJBClientConfiguratorService.class, service.getConfiguratorServiceInjector());

        final Injector<RemotingProfileService> profileServiceInjector = new Injector<RemotingProfileService>() {
            final Injector<EJBTransportProvider> injector = service.getLocalProviderInjector();
            boolean injected = false;

            public void inject(final RemotingProfileService value) throws InjectionException {
                final EJBTransportProvider provider = value.getLocalTransportProviderInjector().getOptionalValue();
                if (provider != null) {
                    injected = true;
                    injector.inject(provider);
                }
            }

            public void uninject() {
                if (injected) {
                    injected = false;
                    injector.uninject();
                }
            }
        };
        final String profile = ejbClientDescriptorMetaData.getProfile();
        final ServiceName profileServiceName;
        if (profile != null) {
            profileServiceName = RemotingProfileService.BASE_SERVICE_NAME.append(profile);
            serviceBuilder.addDependency(profileServiceName, RemotingProfileService.class, profileServiceInjector);
        } else {
            // if descriptor defines list of ejb-receivers instead of profile then we create internal ProfileService for this
            // application which contains defined receivers
            profileServiceName = ejbClientContextServiceName.append(INTERNAL_REMOTING_PROFILE);
            createInternalRemotingProfileService(profileServiceName, serviceTarget, ejbClientDescriptorMetaData);
            serviceBuilder.addDependency(profileServiceName, RemotingProfileService.class, profileServiceInjector);
        }
        // install the service
        serviceBuilder.install();
        EjbLogger.DEPLOYMENT_LOGGER.debugf("Deployment unit %s will use %s as the EJB client context service", deploymentUnit,
                ejbClientContextServiceName);

        // attach the service name of this EJB client context to the deployment unit
        phaseContext.addDeploymentDependency(ejbClientContextServiceName, EjbDeploymentAttachmentKeys.EJB_CLIENT_CONTEXT_SERVICE);
        deploymentUnit.putAttachment(EjbDeploymentAttachmentKeys.EJB_CLIENT_CONTEXT_SERVICE_NAME, ejbClientContextServiceName);
        deploymentUnit.putAttachment(EjbDeploymentAttachmentKeys.EJB_REMOTING_PROFILE_SERVICE_NAME, profileServiceName);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void checkDescriptorConfiguration(final EJBClientDescriptorMetaData ejbClientDescriptorMetaData)
            throws DeploymentUnitProcessingException {
        final boolean profileDefined = ejbClientDescriptorMetaData.getProfile() != null;
        final boolean receiversDefined = (!ejbClientDescriptorMetaData.getRemotingReceiverConfigurations().isEmpty())
                || (ejbClientDescriptorMetaData.isLocalReceiverExcluded() != null)
                || (ejbClientDescriptorMetaData.isLocalReceiverPassByValue() != null);
        if (profileDefined && receiversDefined) {
            throw EjbLogger.ROOT_LOGGER.profileAndRemotingEjbReceiversUsedTogether();
        }
    }

    private RemotingProfileService createInternalRemotingProfileService(final ServiceName profileServiceName,
            final ServiceTarget serviceTarget, final EJBClientDescriptorMetaData ejbClientDescriptorMetaData) {
        final RemotingProfileService profileService = new RemotingProfileService(Collections.emptyList());
        final ServiceBuilder<RemotingProfileService> profileServiceBuilder = serviceTarget.addService(profileServiceName,
                profileService);
        if (ejbClientDescriptorMetaData.isLocalReceiverExcluded() != Boolean.TRUE) {
            final Boolean passByValue = ejbClientDescriptorMetaData.isLocalReceiverPassByValue();
            profileServiceBuilder.addDependency(passByValue == Boolean.TRUE ? LocalTransportProvider.BY_VALUE_SERVICE_NAME : LocalTransportProvider.BY_REFERENCE_SERVICE_NAME, EJBTransportProvider.class, profileService.getLocalTransportProviderInjector());
        }
        profileServiceBuilder.install();
        //TODO Elytron old configuration emulation
        return profileService;
    }

    private OptionMap getOptionMapFromProperties(final Properties properties, final ClassLoader classLoader) {
        final OptionMap.Builder optionMapBuilder = OptionMap.builder();
        for (final String propertyName : properties.stringPropertyNames()) {
            try {
                final Option<?> option = Option.fromString(propertyName, classLoader);
                optionMapBuilder.parse(option, properties.getProperty(propertyName), classLoader);
            } catch (IllegalArgumentException e) {
                EjbLogger.DEPLOYMENT_LOGGER.failedToCreateOptionForProperty(propertyName, e.getMessage());
            }
        }
        return optionMapBuilder.getMap();
    }

}
