/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

import java.util.function.Supplier;

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
class MailSessionDefinitionInjectionSource extends ResourceDefinitionInjectionSource implements Supplier<SessionProvider> {

    private final SessionProvider provider;

    public MailSessionDefinitionInjectionSource(final String jndiName, final SessionProvider provider) {
        super(jndiName);
        this.provider = provider;
    }

    @Override
    public SessionProvider get() {
        return this.provider;
    }

    @Override
    public void getResourceValue(final ResolutionContext context, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);

        try {
            startMailSession(jndiName, eeModuleDescription, context, phaseContext.getServiceTarget(), serviceBuilder, injector);

        } catch (Exception e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    private void startMailSession(final String jndiName,
                                  final EEModuleDescription moduleDescription,
                                  final ResolutionContext context,
                                  final ServiceTarget serviceTarget,
                                  final ServiceBuilder<?> valueSourceServiceBuilder, final Injector<ManagedReferenceFactory> injector) {

        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoForEnvEntry(context.getApplicationName(), context.getModuleName(), context.getComponentName(), !context.isCompUsesModule(), jndiName);
        final BinderService binderService = new BinderService(bindInfo.getBindName(), this);

        ServiceBuilder<ManagedReferenceFactory> binderBuilder = serviceTarget.addService(bindInfo.getBinderServiceName(), binderService);
        binderService.getManagedObjectInjector().inject(new MailSessionManagedReferenceFactory(this));
        binderBuilder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector()).addListener(new LifecycleListener() {
                    private volatile boolean bound;
                    @Override
                    public void handleEvent(final ServiceController<?> controller, final LifecycleEvent event) {
                        switch (event) {
                            case UP: {
                                MailLogger.ROOT_LOGGER.boundMailSession(jndiName);
                                bound = true;
                                break;
                            }
                            case DOWN: {
                                if (bound) {
                                    MailLogger.ROOT_LOGGER.unboundMailSession(jndiName);
                                }
                                break;
                            }
                            case REMOVED: {
                                MailLogger.ROOT_LOGGER.debugf("Removed Mail Session [%s]", jndiName);
                                break;
                            }
                        }
                    }
                });

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
