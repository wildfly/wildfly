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
package org.jboss.as.ejb3.deployment.processors.merging;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;
import org.jboss.modules.Module;

import javax.ejb.LocalHome;
import javax.ejb.RemoteHome;
import java.lang.reflect.Method;
import java.util.Collection;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;
/**
 * Merging processor for home and local home views
 *
 * @author Stuart Douglas
 */
public class HomeViewMergingProcessor implements DeploymentUnitProcessor {

    private final boolean appclient;

    public HomeViewMergingProcessor(final boolean appclient) {
        this.appclient = appclient;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);

        if(eeModuleDescription == null) {
             return;
        }

        final Collection<ComponentDescription> componentConfigurations = eeModuleDescription.getComponentDescriptions();


        final DeploymentReflectionIndex deploymentReflectionIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);

        for (ComponentDescription componentConfiguration : componentConfigurations) {
            if (componentConfiguration instanceof SessionBeanComponentDescription) {
                try {
                    processComponentConfig(deploymentUnit, applicationClasses, module, deploymentReflectionIndex, (SessionBeanComponentDescription) componentConfiguration);
                } catch (Exception e) {
                    throw MESSAGES.failToMergeData(componentConfiguration.getComponentName(),e);
                }
            }
        }
        if (appclient) {
            for (ComponentDescription componentDescription : deploymentUnit.getAttachmentList(Attachments.ADDITIONAL_RESOLVABLE_COMPONENTS)) {
                if (componentDescription instanceof SessionBeanComponentDescription) {
                    try {
                        processComponentConfig(deploymentUnit, applicationClasses, module, deploymentReflectionIndex, (SessionBeanComponentDescription) componentDescription);
                    } catch (Exception e) {
                        throw MESSAGES.failToMergeData(componentDescription.getComponentName(),e);
                    }
                }
            }
        }
    }

    private void processComponentConfig(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final Module module, final DeploymentReflectionIndex deploymentReflectionIndex, final SessionBeanComponentDescription description) throws DeploymentUnitProcessingException, ClassNotFoundException {
        String home = null;
        String localHome = null;

        //first check for annotations
        if (!MetadataCompleteMarker.isMetadataComplete(deploymentUnit)) {
            final EEModuleClassDescription clazz = applicationClasses.getClassByName(description.getComponentClassName());
            //we only care about annotations on the bean class itself
            if (clazz != null) {
                final ClassAnnotationInformation<LocalHome, String> localAnnotations = clazz.getAnnotationInformation(LocalHome.class);
                if (localAnnotations != null) {
                    if (!localAnnotations.getClassLevelAnnotations().isEmpty()) {
                        localHome = localAnnotations.getClassLevelAnnotations().get(0);

                        if (description.getEjbLocalView() == null) {
                            //If the local home is specified via annotation then the corresponding business interface is implied
                            //by the signature of the create method
                            //See EJB 3.1 21.4.5

                            final Class localHomeClass = module.getClassLoader().loadClass(localHome);
                            final ClassReflectionIndex<?> index = deploymentReflectionIndex.getClassIndex(localHomeClass);
                            Class<?> localClass = null;
                            for (final Method method : index.getMethods()) {
                                if (method.getName().startsWith("create")) {
                                    if (localClass != null && localClass != method.getReturnType()) {
                                        throw MESSAGES.multipleCreateMethod(localHomeClass);
                                    }
                                    localClass = method.getReturnType();
                                }
                            }
                            description.addEjbLocalObjectView(localClass.getName());
                        }
                    }
                }
                final ClassAnnotationInformation<RemoteHome, String> remoteAnnotations = clazz.getAnnotationInformation(RemoteHome.class);
                if (remoteAnnotations != null) {
                    if (!remoteAnnotations.getClassLevelAnnotations().isEmpty()) {
                        home = remoteAnnotations.getClassLevelAnnotations().get(0);
                        if (description.getEjbRemoteView() == null) {
                            //If the remote home is specified via annotation then the corresponding business interface is implied
                            //by the signature of the create method
                            //See EJB 3.1 21.4.5

                            final Class homeClass = module.getClassLoader().loadClass(home);
                            final ClassReflectionIndex<?> index = deploymentReflectionIndex.getClassIndex(homeClass);
                            Class<?> remote = null;
                            for (final Method method : index.getMethods()) {
                                if (method.getName().startsWith("create")) {
                                    if (remote != null && remote != method.getReturnType()) {
                                        throw MESSAGES.multipleCreateMethod(homeClass);
                                    }
                                    remote = method.getReturnType();
                                }
                            }
                            if(remote == null) {
                                throw new DeploymentUnitProcessingException("Could not determine remove interface type from home " + homeClass + " for bean " + description.getEJBName());
                            }
                            description.addEjbObjectView(remote.getName());
                        }
                    }
                }
            }
        }
        //now allow the annotations to be overridden by the DD
        final SessionBeanMetaData descriptorData = description.getDescriptorData();
        if (descriptorData != null) {

            if (descriptorData.getHome() != null) {
                home = descriptorData.getHome();
            }
            if (descriptorData.getLocalHome() != null) {
                localHome = descriptorData.getLocalHome();
            }
        }
        if (localHome != null) {
            description.addLocalHome(localHome);
        }
        if (home != null) {
            description.addRemoteHome(home);
        }
    }


    @Override
    public void undeploy(final DeploymentUnit context) {

    }

}
