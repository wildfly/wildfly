/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.mail.extension;


import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * A binding description for {@link MailSessionDefinition} annotations.
 * <p/>
 * The referenced mail session must be directly visible to the
 * component declaring the annotation.
 *
 * @author Tomaz Cerar
 * @author Eduardo Martins
 */
class MailSessionDefinitionInjectionSource extends ResourceDefinitionInjectionSource {

    private final SessionProvider provider;

    public MailSessionDefinitionInjectionSource(final String jndiName, final SessionProvider provider) {
        super(jndiName);
        this.provider = provider;
    }

    public void getResourceValue(final ResolutionContext context, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);

        try {
            MailSessionService mailSessionService = new DirectMailSessionService(provider);
            startMailSession(mailSessionService, jndiName, eeModuleDescription, context, phaseContext.getServiceTarget(), serviceBuilder, injector);

        } catch (Exception e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    private void startMailSession(final MailSessionService mailSessionService,
                                  final String jndiName,
                                  final EEModuleDescription moduleDescription,
                                  final ResolutionContext context,
                                  final ServiceTarget serviceTarget,
                                  final ServiceBuilder<?> valueSourceServiceBuilder, final Injector<ManagedReferenceFactory> injector) {


        final ServiceName mailSessionServiceName = MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName().append("MailSessionDefinition", moduleDescription.getApplicationName(), moduleDescription.getModuleName(), jndiName);
        final ServiceBuilder<?> mailSessionServiceBuilder = serviceTarget
                .addService(mailSessionServiceName, mailSessionService);

        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoForEnvEntry(context.getApplicationName(), context.getModuleName(), context.getComponentName(), !context.isCompUsesModule(), jndiName);

        final MailSessionManagedReferenceFactory referenceFactoryService = new MailSessionManagedReferenceFactory(mailSessionService);

        final BinderService binderService = new BinderService(bindInfo.getBindName(), this);
        final ServiceBuilder<ManagedReferenceFactory> binderBuilder = serviceTarget
                .addService(bindInfo.getBinderServiceName(), binderService)
                .addInjection(binderService.getManagedObjectInjector(), referenceFactoryService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector()).addListener(new LifecycleListener() {
                    @Override
                    public void handleEvent(final ServiceController<?> controller, final LifecycleEvent event) {
                        switch (event) {
                            case UP: {
                                MailLogger.ROOT_LOGGER.boundMailSession(jndiName);
                                break;
                            }
                            case DOWN: {
                                MailLogger.ROOT_LOGGER.unboundMailSession(jndiName);
                                break;
                            }
                            case REMOVED: {
                                MailLogger.ROOT_LOGGER.debugf("Removed Mail Session [%s]", jndiName);
                                break;
                            }
                        }
                    }
                });

        mailSessionServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();
        binderBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();

        valueSourceServiceBuilder.addDependency(bindInfo.getBinderServiceName(), ManagedReferenceFactory.class, injector);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MailSessionDefinitionInjectionSource that = (MailSessionDefinitionInjectionSource) o;

        if (provider != null ? !provider.equals(that.provider) : that.provider != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (provider != null ? provider.hashCode() : 0);
        return result;
    }
}
