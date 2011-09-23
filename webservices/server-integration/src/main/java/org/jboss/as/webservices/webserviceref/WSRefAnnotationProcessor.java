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
package org.jboss.as.webservices.webserviceref;

import static org.jboss.as.ee.utils.InjectionUtils.getInjectionTarget;
import static org.jboss.as.webservices.util.ASHelper.getWSRefRegistry;
import static org.jboss.as.webservices.webserviceref.WSRefUtils.processAnnotatedElement;

import java.lang.reflect.AccessibleObject;
import java.util.List;

import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.WebServiceRefs;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.BindingConfigurator;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.FieldInjectionTarget;
import org.jboss.as.ee.component.InjectionConfigurator;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.InjectionTarget;
import org.jboss.as.ee.component.LookupInjectionSource;
import org.jboss.as.ee.component.MethodInjectionTarget;
import org.jboss.as.ee.component.ResourceInjectionConfiguration;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.webservices.util.VirtualFileAdaptor;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.modules.Module;
import org.jboss.wsf.spi.deployment.UnifiedVirtualFile;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedServiceRefMetaData;
import org.jboss.wsf.spi.serviceref.ServiceRefHandler;

/**
 * TODO: javadoc
 * Deployment processor responsible for analyzing each attached {@link org.jboss.as.ee.component.ComponentDescription} instance to configure
 * required {@link WebServiceRef} injection.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @author John Bailey
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class WSRefAnnotationProcessor implements DeploymentUnitProcessor {
    private static final DotName WEB_SERVICE_REF_ANNOTATION_NAME = DotName.createSimple(WebServiceRef.class.getName());
    private static final DotName WEB_SERVICE_REFS_ANNOTATION_NAME = DotName.createSimple(WebServiceRefs.class.getName());

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
        final List<AnnotationInstance> resourceAnnotations = index.getAnnotations(WEB_SERVICE_REF_ANNOTATION_NAME);
        // TODO: we removed EEComponetClasses & EEapplicationClasses from parameters passing - fix it in all AS7 processors as well
        for (AnnotationInstance annotation : resourceAnnotations) {
            final AnnotationTarget annotationTarget = annotation.target();
            final WSRefAnnotationWrapper annotationWrapper = new WSRefAnnotationWrapper(annotation);

            if (annotationTarget instanceof FieldInfo) {
                processFieldRef(deploymentUnit, annotationWrapper, (FieldInfo) annotationTarget);
            } else if (annotationTarget instanceof MethodInfo) {
                processMethodRef(deploymentUnit, annotationWrapper, (MethodInfo) annotationTarget);
            } else if (annotationTarget instanceof ClassInfo) {
                processClassRef(deploymentUnit, annotationWrapper, (ClassInfo) annotationTarget);
            }
        }
        final List<AnnotationInstance> resourcesAnnotations = index.getAnnotations(WEB_SERVICE_REFS_ANNOTATION_NAME);
        for (AnnotationInstance outerAnnotation : resourcesAnnotations) {
            final AnnotationTarget annotationTarget = outerAnnotation.target();
            if (annotationTarget instanceof ClassInfo) {
                final AnnotationInstance[] values = outerAnnotation.value("value").asNestedArray();
                for (AnnotationInstance annotation : values) {
                    processClassRef(deploymentUnit, new WSRefAnnotationWrapper(annotation), (ClassInfo) annotationTarget);
                }
            }
        }
    }

    public void undeploy(final DeploymentUnit context) {
        // NOOP
    }

    private void processFieldRef(final DeploymentUnit deploymentUnit, final WSRefAnnotationWrapper annotation, final FieldInfo fieldInfo) throws DeploymentUnitProcessingException {
        final String fieldName = fieldInfo.name();
        final String injectionType = isEmpty(annotation.type()) || annotation.type().equals(Object.class.getName()) ? fieldInfo.type().name().toString() : annotation.type();
        final InjectionTarget targetDescription = new FieldInjectionTarget(fieldInfo.declaringClass().name().toString(),  fieldName, injectionType);
        final String localContextName = isEmpty(annotation.name()) ? fieldInfo.declaringClass().name().toString() + "/" + fieldInfo.name() : annotation.name();
        processRef(deploymentUnit, localContextName, injectionType, annotation.value(), annotation.wsdlLocation(), fieldInfo.declaringClass(), targetDescription, localContextName);
    }

    private void processMethodRef(final DeploymentUnit deploymentUnit, final WSRefAnnotationWrapper annotation, final MethodInfo methodInfo) throws DeploymentUnitProcessingException {
        final String methodName = methodInfo.name();
        if (!methodName.startsWith("set") || methodInfo.args().length != 1) {
            throw new IllegalArgumentException("@WebServiceRef injection target is invalid.  Only setter methods are allowed: " + methodInfo);
        }
        final String injectionType = isEmpty(annotation.type()) || annotation.type().equals(Object.class.getName()) ? methodInfo.args()[0].name().toString() : annotation.type();
        final InjectionTarget targetDescription = new MethodInjectionTarget(methodInfo.declaringClass().name().toString(), methodName, injectionType);

        final String localContextName = isEmpty(annotation.name()) ? methodInfo.declaringClass().name().toString() + "/" + methodName.substring(3, 4).toLowerCase() + methodName.substring(4) : annotation.name();
        processRef(deploymentUnit, localContextName, injectionType, annotation.value(), annotation.wsdlLocation(), methodInfo.declaringClass(), targetDescription, localContextName);
    }

    private void processClassRef(final DeploymentUnit deploymentUnit, final WSRefAnnotationWrapper annotation, final ClassInfo classInfo) throws DeploymentUnitProcessingException {
        if (isEmpty(annotation.name())) {
            throw new DeploymentUnitProcessingException("@WebServiceRef attribute 'name' is required fo class level annotations.");
        }
        if (isEmpty(annotation.type())) {
            throw new DeploymentUnitProcessingException("@WebServiceRef attribute 'type' is required fo class level annotations.");
        }
        processRef(deploymentUnit, annotation.name(), annotation.type(), annotation.value(), annotation.wsdlLocation(), classInfo, null, annotation.name());
    }

    private void processRef(final DeploymentUnit deploymentUnit, final String name, final String type, final String value, final String wsdlLocation, final ClassInfo classInfo, final InjectionTarget targetDescription, final String localContextName) throws DeploymentUnitProcessingException {
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        final EEModuleClassDescription classDescription = applicationClasses.getOrAddClassByName(classInfo.name().toString());

        // our injection comes from the local lookup, no matter what.
        final ResourceInjectionConfiguration injectionConfiguration = targetDescription != null ?
            new ResourceInjectionConfiguration(targetDescription, new LookupInjectionSource(localContextName)) : null;

        // Create the binding from whence our injection comes.
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final ClassLoader classLoader = module.getClassLoader();
        final WSReferences wsRefRegistry = getWSRefRegistry(deploymentUnit);
        UnifiedServiceRefMetaData serviceRefUMDM = wsRefRegistry.get(name);
        if (serviceRefUMDM == null) {
            serviceRefUMDM = new UnifiedServiceRefMetaData(getUnifiedVirtualFile(deploymentUnit));
            serviceRefUMDM.setServiceRefName(name);
            wsRefRegistry.add(name, serviceRefUMDM);
        }
        processServiceRef(deploymentUnit, serviceRefUMDM, classLoader, name, type, value, wsdlLocation);
        processInjectionTargets(deploymentUnit, targetDescription, serviceRefUMDM);
        final InjectionSource valueSource = new WSRefValueSource(serviceRefUMDM);
        final BindingConfiguration bindingConfiguration = new BindingConfiguration(localContextName, valueSource);

        // TODO: class hierarchies? shared bindings?
        classDescription.getConfigurators().add(new BindingConfigurator(bindingConfiguration));
        if (injectionConfiguration != null) {
            classDescription.getConfigurators().add(new InjectionConfigurator(injectionConfiguration));
        }
    }

    private static void processInjectionTargets(final DeploymentUnit unit, final InjectionTarget injectionTarget, final UnifiedServiceRefMetaData serviceRefUMDM) throws DeploymentUnitProcessingException {
        if (injectionTarget == null) return;
        final Module module = unit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final DeploymentReflectionIndex deploymentReflectionIndex = unit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
        final String injectionTargetClassName = injectionTarget.getClassName();
        final String injectionTargetName = getInjectionTargetName(injectionTarget);
        final AccessibleObject fieldOrMethod = getInjectionTarget(injectionTargetClassName, injectionTargetName, module.getClassLoader(), deploymentReflectionIndex);
        processAnnotatedElement(fieldOrMethod, serviceRefUMDM);
    }

    private static String getInjectionTargetName(final InjectionTarget injectionTarget) {
        final String name = injectionTarget.getName();
        if (injectionTarget instanceof FieldInjectionTarget) {
            return name;
        } else if (injectionTarget instanceof MethodInjectionTarget) {
            return name.substring(3, 4).toUpperCase() + name.substring(4);
        }
        throw new UnsupportedOperationException();
    }

    private UnifiedServiceRefMetaData processServiceRef(final DeploymentUnit unit, final UnifiedServiceRefMetaData serviceRefUMDM, final ClassLoader classLoader, final String name, final String type, final String value, final String wsdlLocation) {
        // TODO handle mappedName
        final Class<?> typeClass;
        try {
            typeClass = classLoader.loadClass(type);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load class " + type);
        }

        if (wsdlLocation != null && wsdlLocation.length() > 0) {
            serviceRefUMDM.setWsdlFile(wsdlLocation);
        }
        serviceRefUMDM.setServiceRefType(type);
        if (!isEmpty(value)) {
            serviceRefUMDM.setServiceInterface(value);
        } else if (Service.class.isAssignableFrom(typeClass)) {
            serviceRefUMDM.setServiceInterface(type);
        } else {
            serviceRefUMDM.setServiceInterface(Service.class.getName());
        }

        final boolean isJAXRPC = serviceRefUMDM.getMappingFile() != null // TODO: is mappingFile check required?
                || "javax.xml.rpc.Service".equals(serviceRefUMDM.getServiceInterface());
        serviceRefUMDM.setType(isJAXRPC ? ServiceRefHandler.Type.JAXRPC : ServiceRefHandler.Type.JAXWS);

        return serviceRefUMDM;
    }

    private static UnifiedVirtualFile getUnifiedVirtualFile(final DeploymentUnit deploymentUnit) { // TODO: refactor to common code
        final ResourceRoot resourceRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
        return new VirtualFileAdaptor(resourceRoot.getRoot());
    }

    private static boolean isEmpty(final String string) { // TODO: some common class - StringUtils ?
        return string == null || string.isEmpty();
    }

}
