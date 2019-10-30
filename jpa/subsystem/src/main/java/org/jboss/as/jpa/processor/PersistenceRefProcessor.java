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

package org.jboss.as.jpa.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import javax.persistence.SynchronizationType;

import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.LookupInjectionSource;
import org.jboss.as.ee.component.ResourceInjectionTarget;
import org.jboss.as.ee.component.deployers.AbstractDeploymentDescriptorBindingsProcessor;
import org.jboss.as.jpa.config.JPADeploymentSettings;
import org.jboss.as.jpa.container.PersistenceUnitSearch;
import org.jboss.as.jpa.injectors.PersistenceContextInjectionSource;
import org.jboss.as.jpa.injectors.PersistenceUnitInjectionSource;
import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.JPADeploymentMarker;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.javaee.spec.Environment;
import org.jboss.metadata.javaee.spec.PersistenceContextReferenceMetaData;
import org.jboss.metadata.javaee.spec.PersistenceContextReferencesMetaData;
import org.jboss.metadata.javaee.spec.PersistenceContextSynchronizationType;
import org.jboss.metadata.javaee.spec.PersistenceContextTypeDescription;
import org.jboss.metadata.javaee.spec.PersistenceUnitReferenceMetaData;
import org.jboss.metadata.javaee.spec.PersistenceUnitReferencesMetaData;
import org.jboss.metadata.javaee.spec.PropertiesMetaData;
import org.jboss.metadata.javaee.spec.PropertyMetaData;
import org.jboss.metadata.javaee.spec.RemoteEnvironment;
import org.jboss.msc.service.ServiceName;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * Deployment processor responsible for processing persistence unit / context references from deployment descriptors.
 *
 * @author Stuart Douglas
 */
public class PersistenceRefProcessor extends AbstractDeploymentDescriptorBindingsProcessor {


    @Override
    protected List<BindingConfiguration> processDescriptorEntries(DeploymentUnit deploymentUnit, DeploymentDescriptorEnvironment environment, ResourceInjectionTarget resourceInjectionTarget, final ComponentDescription componentDescription, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, final EEApplicationClasses applicationClasses) throws
        DeploymentUnitProcessingException {
        List<BindingConfiguration> bindings = new ArrayList<BindingConfiguration>();
        bindings.addAll(getPersistenceUnitRefs(deploymentUnit, environment, classLoader, deploymentReflectionIndex, resourceInjectionTarget));
        bindings.addAll(getPersistenceContextRefs(deploymentUnit, environment, classLoader, deploymentReflectionIndex, resourceInjectionTarget));
        return bindings;
    }


    /**
     * Resolves persistence-unit-ref
     *
     * @param environment               The environment to resolve the elements for
     * @param classLoader               The deployment class loader
     * @param deploymentReflectionIndex The reflection index
     * @return The bindings for the environment entries
     */
    private List<BindingConfiguration> getPersistenceUnitRefs(DeploymentUnit deploymentUnit, DeploymentDescriptorEnvironment environment, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, ResourceInjectionTarget resourceInjectionTarget) throws
        DeploymentUnitProcessingException {

        final List<BindingConfiguration> bindingConfigurations = new ArrayList<BindingConfiguration>();
        if (environment.getEnvironment() == null) {
            return bindingConfigurations;
        }
        PersistenceUnitReferencesMetaData persistenceUnitRefs = environment.getEnvironment().getPersistenceUnitRefs();

        if (persistenceUnitRefs != null) {
            if (persistenceUnitRefs.size() > 0) {
                JPADeploymentMarker.mark(deploymentUnit);
            }
            for (PersistenceUnitReferenceMetaData puRef : persistenceUnitRefs) {
                String name = puRef.getName();
                String persistenceUnitName = puRef.getPersistenceUnitName();
                String lookup = puRef.getLookupName();

                if (!isEmpty(lookup) && !isEmpty(persistenceUnitName)) {
                    throw JpaLogger.ROOT_LOGGER.cannotSpecifyBoth("<lookup-name>", lookup, "persistence-unit-name", persistenceUnitName, "<persistence-unit-ref/>", resourceInjectionTarget);
                }
                if (!name.startsWith("java:")) {
                    name = environment.getDefaultContext() + name;
                }

                // our injection (source) comes from the local (ENC) lookup, no matter what.
                LookupInjectionSource injectionSource = new LookupInjectionSource(name);

                //add any injection targets
                processInjectionTargets(resourceInjectionTarget, injectionSource, classLoader, deploymentReflectionIndex, puRef, EntityManagerFactory.class);

                BindingConfiguration bindingConfiguration = null;
                if (!isEmpty(lookup)) {
                    bindingConfiguration = new BindingConfiguration(name, new LookupInjectionSource(lookup));
                } else {
                    InjectionSource puBindingSource = this.getPersistenceUnitBindingSource(deploymentUnit, persistenceUnitName);
                    bindingConfiguration = new BindingConfiguration(name, puBindingSource);
                }
                bindingConfigurations.add(bindingConfiguration);
            }
        }
        return bindingConfigurations;
    }

    /**
     * Resolves persistence-unit-ref
     *
     * @param environment               The environment to resolve the elements for
     * @param classLoader               The deployment class loader
     * @param deploymentReflectionIndex The reflection index
     * @return The bindings for the environment entries
     */
    private List<BindingConfiguration> getPersistenceContextRefs(DeploymentUnit deploymentUnit, DeploymentDescriptorEnvironment environment, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, ResourceInjectionTarget resourceInjectionTarget) throws
        DeploymentUnitProcessingException {

        List<BindingConfiguration> bindingConfigurations = new ArrayList<BindingConfiguration>();
        final RemoteEnvironment remoteEnvironment = environment.getEnvironment();
        if (remoteEnvironment == null) {
            return bindingConfigurations;
        }

        if (remoteEnvironment instanceof Environment) {
            PersistenceContextReferencesMetaData persistenceUnitRefs = ((Environment) remoteEnvironment).getPersistenceContextRefs();

            if (persistenceUnitRefs != null) {
                for (PersistenceContextReferenceMetaData puRef : persistenceUnitRefs) {
                    String name = puRef.getName();
                    String persistenceUnitName = puRef.getPersistenceUnitName();
                    String lookup = puRef.getLookupName();

                    if (!isEmpty(lookup) && !isEmpty(persistenceUnitName)) {
                        throw JpaLogger.ROOT_LOGGER.cannotSpecifyBoth("<lookup-name>", lookup, "persistence-unit-name", persistenceUnitName, "<persistence-context-ref/>", resourceInjectionTarget);
                    }
                    if (!name.startsWith("java:")) {
                        name = environment.getDefaultContext() + name;
                    }

                    // our injection (source) comes from the local (ENC) lookup, no matter what.
                    LookupInjectionSource injectionSource = new LookupInjectionSource(name);
                    //add any injection targets
                    processInjectionTargets(resourceInjectionTarget, injectionSource, classLoader, deploymentReflectionIndex, puRef, EntityManager.class);

                    BindingConfiguration bindingConfiguration = null;
                    if (!isEmpty(lookup)) {
                        bindingConfiguration = new BindingConfiguration(name, new LookupInjectionSource(lookup));
                    } else {
                        PropertiesMetaData properties = puRef.getProperties();
                        Map<String, String> map = new HashMap<>();
                        if (properties != null) {
                            for (PropertyMetaData prop : properties) {
                                map.put(prop.getKey(), prop.getValue());
                            }
                        }
                        PersistenceContextType type = (puRef.getPersistenceContextType() == null || puRef.getPersistenceContextType() == PersistenceContextTypeDescription.TRANSACTION) ? PersistenceContextType.TRANSACTION : PersistenceContextType.EXTENDED ;
                        SynchronizationType synchronizationType =
                                (puRef.getPersistenceContextSynchronization() == null || PersistenceContextSynchronizationType.Synchronized.equals(puRef.getPersistenceContextSynchronization()))?
                                        SynchronizationType.SYNCHRONIZED: SynchronizationType.UNSYNCHRONIZED;
                        InjectionSource pcBindingSource = this.getPersistenceContextBindingSource(deploymentUnit, persistenceUnitName, type, synchronizationType, map);
                        bindingConfiguration = new BindingConfiguration(name, pcBindingSource);
                    }
                    bindingConfigurations.add(bindingConfiguration);
                }
            }
        }
        return bindingConfigurations;
    }


    private InjectionSource getPersistenceUnitBindingSource(final DeploymentUnit deploymentUnit, final String unitName) throws
        DeploymentUnitProcessingException {
        final String searchName;
        if (isEmpty(unitName)) {
            searchName = null;
        } else {
            searchName = unitName;
        }
        final PersistenceUnitMetadata pu = PersistenceUnitSearch.resolvePersistenceUnitSupplier(deploymentUnit, searchName);
        if (null == pu) {
            throw new DeploymentUnitProcessingException(JpaLogger.ROOT_LOGGER.persistenceUnitNotFound(searchName, deploymentUnit));
        }
        String scopedPuName = pu.getScopedPersistenceUnitName();
        ServiceName puServiceName = getPuServiceName(scopedPuName);
        return new PersistenceUnitInjectionSource(puServiceName, deploymentUnit.getServiceRegistry(), EntityManagerFactory.class.getName(), pu);
    }

    private InjectionSource getPersistenceContextBindingSource(
            final DeploymentUnit deploymentUnit,
            final String unitName,
            PersistenceContextType type,
            SynchronizationType synchronizationType,
            Map properties) throws
        DeploymentUnitProcessingException {
        PersistenceUnitMetadata pu = getPersistenceUnit(deploymentUnit, unitName);
        String scopedPuName = pu.getScopedPersistenceUnitName();
        ServiceName puServiceName = getPuServiceName(scopedPuName);
        // get deployment settings from top level du (jboss-all.xml is only parsed at the top level).
        final JPADeploymentSettings jpaDeploymentSettings = DeploymentUtils.getTopDeploymentUnit(deploymentUnit).getAttachment(JpaAttachments.DEPLOYMENT_SETTINGS_KEY);
        return new PersistenceContextInjectionSource(type, synchronizationType, properties, puServiceName, deploymentUnit.getServiceRegistry(), scopedPuName, EntityManager.class.getName(), pu, jpaDeploymentSettings);
    }

    private PersistenceUnitMetadata getPersistenceUnit(final DeploymentUnit deploymentUnit, final String puName)
        throws DeploymentUnitProcessingException {

        PersistenceUnitMetadata pu = PersistenceUnitSearch.resolvePersistenceUnitSupplier(deploymentUnit, puName);
        if (null == pu) {
            throw new DeploymentUnitProcessingException(JpaLogger.ROOT_LOGGER.persistenceUnitNotFound(puName, deploymentUnit));
        }
        return pu;
    }

    private ServiceName getPuServiceName(String scopedPuName)
        throws DeploymentUnitProcessingException {

        return PersistenceUnitServiceImpl.getPUServiceName(scopedPuName);
    }

    private boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

}
