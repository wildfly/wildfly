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
package org.jboss.as.webservices.deployers;

import org.jboss.as.ee.component.AbstractComponentConfigProcessor;
import org.jboss.as.ee.component.AbstractComponentDescription;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingDescription;
import org.jboss.as.ee.component.BindingSourceDescription;
import org.jboss.as.ee.component.EEApplicationDescription;
import org.jboss.as.ee.component.InjectionTargetDescription;
import org.jboss.as.ee.component.InterceptorDescription;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedObject;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.webservices.deployers.annotation.AbstractWebServiceRefAnnotation;
import org.jboss.as.webservices.deployers.annotation.WebServiceRefFieldAnnotation;
import org.jboss.as.webservices.deployers.annotation.WebServiceRefMethodAnnotation;
import org.jboss.as.webservices.util.VirtualFileAdaptor;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.value.MethodValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;
import org.jboss.util.NotImplementedException;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.classloading.ClassLoaderProvider;
import org.jboss.wsf.spi.deployment.UnifiedVirtualFile;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedServiceRefMetaData;
import org.jboss.wsf.spi.serviceref.ServiceRefHandler;
import org.jboss.wsf.spi.serviceref.ServiceRefHandlerFactory;
import org.jboss.wsf.stack.cxf.client.serviceref.CXFServiceObjectFactoryJAXWS;

import javax.naming.Referenceable;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.WebServiceRefs;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * Deployment processor responsible for analyzing each attached {@link AbstractComponentDescription} instance to configure
 * required {@link WebServiceRef} injection.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class WebServiceRefAnnotationParsingProcessor extends AbstractComponentConfigProcessor {
    private static final DotName WEB_SERVICE_REF_ANNOTATION_NAME = DotName.createSimple(WebServiceRef.class.getName());
    private static final DotName WEB_SERVICE_REFS_ANNOTATION_NAME = DotName.createSimple(WebServiceRefs.class.getName());

    /** {@inheritDoc} **/
    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final CompositeIndex index, final AbstractComponentDescription description) throws DeploymentUnitProcessingException {
        final ClassInfo classInfo = index.getClassByName(DotName.createSimple(description.getComponentClassName()));
        if(classInfo == null) {
            return; // We can't continue without the annotation index info.
        }
        description.addAnnotationBindings(getWebServiceConfigurations(deploymentUnit, classInfo, description));
        final Collection<InterceptorDescription> interceptorConfigurations = description.getAllInterceptors().values();
        for (InterceptorDescription interceptorConfiguration : interceptorConfigurations) {
            final ClassInfo interceptorClassInfo = index.getClassByName(DotName.createSimple(interceptorConfiguration.getInterceptorClassName()));
            if(interceptorClassInfo == null) {
                continue;
            }
            description.addAnnotationBindings(getWebServiceConfigurations(deploymentUnit, interceptorClassInfo, description));
        }
    }

    private List<BindingDescription> getWebServiceConfigurations(final DeploymentUnit deploymentUnit, final ClassInfo classInfo, final AbstractComponentDescription componentDescription) {
        final List<BindingDescription> configurations = new ArrayList<BindingDescription>();
        final Map<DotName, List<AnnotationInstance>> classAnnotations = classInfo.annotations();
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
        UnifiedVirtualFile vfs = getUnifiedVirtualFile(deploymentUnit);
        if (classAnnotations != null) {
            final List<AnnotationInstance> resourceAnnotations = classAnnotations.get(WEB_SERVICE_REF_ANNOTATION_NAME);
            if (resourceAnnotations != null) {
                for (AnnotationInstance annotation : resourceAnnotations) {
                    configurations.add(getWebServiceConfiguration(annotation, vfs, module, index, componentDescription));
                }
            }
        }
        configurations.addAll(processClass(classAnnotations, vfs, module, index));
        return configurations;
    }

    private BindingDescription getWebServiceConfiguration(final AnnotationInstance annotation, final UnifiedVirtualFile vfs, final Module module, final CompositeIndex index, final AbstractComponentDescription componentDescription) {
        final AnnotationTarget annotationTarget = annotation.target();
        final BindingDescription resourceConfiguration;
        if (annotationTarget instanceof FieldInfo) {
            resourceConfiguration = processField(annotation, FieldInfo.class.cast(annotationTarget), vfs, module, index, componentDescription);
        } else if (annotationTarget instanceof MethodInfo) {
            resourceConfiguration = processMethod(annotation, MethodInfo.class.cast(annotationTarget), vfs, module, index, componentDescription);
        } else if (annotationTarget instanceof ClassInfo) {
            resourceConfiguration = processClass(annotation, ClassInfo.class.cast(annotationTarget), vfs, module, index);
        } else {
            resourceConfiguration = null;
        }
        return resourceConfiguration;
    }

    private BindingDescription processField(final AnnotationInstance annotation, final FieldInfo fieldInfo, final UnifiedVirtualFile vfs, final Module duModule, final CompositeIndex index, final AbstractComponentDescription componentDescription) {
        WebServiceRefFieldAnnotation fieldProcessor = new WebServiceRefFieldAnnotation(index);
        return processWebServiceRef(fieldProcessor, annotation, fieldInfo, vfs, duModule, fieldInfo.name(), fieldProcessor.getDeclaringClass(fieldInfo), componentDescription);
    }

    private BindingDescription processMethod(final AnnotationInstance annotation, final MethodInfo methodInfo, final UnifiedVirtualFile vfs, final Module duModule, final CompositeIndex index, final AbstractComponentDescription componentDescription) {
        final String methodName = methodInfo.name();
        if (!methodName.startsWith("set") || methodInfo.args().length != 1) {
            throw new IllegalArgumentException("@WebServiceRef injection target is invalid.  Only setter methods are allowed: " + methodInfo);
        }
        WebServiceRefMethodAnnotation methodProcessor = new WebServiceRefMethodAnnotation(index);
        return processWebServiceRef(methodProcessor, annotation, methodInfo, vfs, duModule, methodInfo.name(), methodProcessor.getDeclaringClass(methodInfo), componentDescription);
    }

    private BindingDescription processClass(final AnnotationInstance annotation, final ClassInfo classInfo, final UnifiedVirtualFile vfs, final Module duModule,final CompositeIndex index) {
        throw new NotImplementedException("Only @WebServiceRef annotations targeting fields and methods are supported at this time");
        /*final AnnotationValue nameValue = annotation.value("name");
        if (nameValue == null || nameValue.asString().isEmpty()) {
            throw new IllegalArgumentException("Class level @WebServiceRef annotations must provide a name.");
        }
        final String name = nameValue.asString();

        final AnnotationValue typeValue = annotation.value("type");
        if (typeValue == null || typeValue.asClass().name().toString().equals(Object.class.getName())) {
            throw new IllegalArgumentException("Class level @WebServiceRef annotations must provide a type.");
        }
        WebServiceRefClassProcessor7 classProcessor = new WebServiceRefClassProcessor7(index);
        return processWebServiceRef(classProcessor, annotation, classInfo, vfs, duModule, name, classInfo.name().toString());*/
    }

    private <E extends AnnotationTarget> BindingDescription  processWebServiceRef(
            final AbstractWebServiceRefAnnotation<E> processor, final AnnotationInstance annotation,
            final E annotated, final UnifiedVirtualFile vfs, final Module duModule, final String name,
            final String className, final AbstractComponentDescription componentDescription) {
        UnifiedServiceRefMetaData ref = processor.process(annotation, annotated, vfs);
        // FIXME SPIProviderResolver won't require a TCCL in the future
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final Referenceable referenceable;
        try {
            Thread.currentThread().setContextClassLoader(
                    ClassLoaderProvider.getDefaultProvider().getServerIntegrationClassLoader());
            final SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
            final ServiceRefHandler serviceRefHandler = spiProvider.getSPI(ServiceRefHandlerFactory.class).getServiceRefHandler();
            referenceable = serviceRefHandler.createReferenceable(ref);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
        // setup binding description
        BindingDescription bindingDescription = new BindingDescription(processor.getName(annotated), componentDescription);
        bindingDescription.setDependency(true);
        bindingDescription.setBindingType(ref.getServiceRefType());
        bindingDescription.setReferenceSourceDescription(new WebServiceRefSourceDescription(referenceable, duModule));
        //setup injection target description
        final InjectionTargetDescription targetDescription = new InjectionTargetDescription();
        targetDescription.setName(name);
        targetDescription.setClassName(className);
        targetDescription.setType(processor.getInjectionType());
        targetDescription.setDeclaredValueClassName(ref.getServiceRefType());
        bindingDescription.getInjectionTargetDescriptions().add(targetDescription);

        return bindingDescription;
    }

    private List<BindingDescription> processClass(final Map<DotName, List<AnnotationInstance>> classAnnotations, final UnifiedVirtualFile vfs, final Module duModule, final CompositeIndex index) {
        final List<AnnotationInstance> annotations = classAnnotations.get(WEB_SERVICE_REFS_ANNOTATION_NAME);
        if (annotations == null || annotations.isEmpty()) {
            return Collections.emptyList();
        }

        final AnnotationInstance annotationInstance = annotations.get(0);
        final AnnotationInstance[] resourceAnnotations = annotationInstance.value().asNestedArray();
        final ClassInfo classInfo = ClassInfo.class.cast(annotationInstance.target());
        final List<BindingDescription> resourceConfigurations = new ArrayList<BindingDescription>(resourceAnnotations.length);
        for (AnnotationInstance resource : resourceAnnotations) {
            resourceConfigurations.add(processClass(resource, classInfo, vfs, duModule, index));
        }
        return resourceConfigurations;
    }

    private static UnifiedVirtualFile getUnifiedVirtualFile(final DeploymentUnit deploymentUnit)
    {
        ResourceRoot resourceRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
        if (resourceRoot == null) {
            throw new IllegalStateException("Resource root not found for deployment " + deploymentUnit);
        }
        return new VirtualFileAdaptor(resourceRoot.getRoot());
    }

    public static class WebServiceRefSourceDescription extends BindingSourceDescription {
        private final Referenceable referenceable;
        private final Module duModule;

        private WebServiceRefSourceDescription(Referenceable referenceable, Module duModule) {
            this.referenceable = referenceable;
            this.duModule = duModule;
        }

        public Object getServiceRefValue() {
            // FIXME this is a workaround to class loader issues
            final ClassLoader tccl = ClassLoaderProvider.getDefaultProvider().getServerIntegrationClassLoader();
            final ClassLoader classLoader = new ClassLoader(this.getClass().getClassLoader()) {
                @Override
                public Class<?> loadClass(String className) throws ClassNotFoundException {
                    try {
                        return super.loadClass(className);
                    } catch (ClassNotFoundException cnfe) {
                        return duModule.getClassLoader().loadClass(className);
                    }
                }

                @Override
                public Enumeration<URL> getResources(String name) throws IOException {
                    final Enumeration<URL> superResources = super.getResources(name);
                    final Enumeration<URL> duModuleCLResources = duModule.getClassLoader().getResources(name);
                    if (superResources == null || !superResources.hasMoreElements()) {
                        return duModuleCLResources;
                    }
                    if (duModuleCLResources == null || !duModuleCLResources.hasMoreElements()) {
                        return superResources;
                    }
                    return new Enumeration<URL>() {
                        public boolean hasMoreElements() {
                            return superResources.hasMoreElements() || duModuleCLResources.hasMoreElements();
                        }

                        public URL nextElement() {
                            if (superResources.hasMoreElements()) {
                                return superResources.nextElement();
                            }
                            return duModuleCLResources.nextElement();
                        }
                    };
                }
            };
            Thread.currentThread().setContextClassLoader(classLoader);
            try {
                return new CXFServiceObjectFactoryJAXWS().getObjectInstance(referenceable.getReference(), null, null, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                Thread.currentThread().setContextClassLoader(tccl);
            }
        }

        public void getResourceValue(BindingDescription referenceDescription, ServiceBuilder<?> serviceBuilder, DeploymentPhaseContext phaseContext, Injector<ManagedReferenceFactory> injector) {
            final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
            final EEApplicationDescription applicationComponentDescription = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_DESCRIPTION);
            if (applicationComponentDescription == null) {
                return; // Not an EE deployment
            }

            Value<Object> getObjectInstanceValue;
            try {
                getObjectInstanceValue = new MethodValue<Object>(Values.immediateValue(
                        this.getClass().getMethod("getServiceRefValue")), Values.immediateValue(this), Values.emptyList());
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

            ManagedReferenceFactory factory = new ValueManagedObject(getObjectInstanceValue);
            serviceBuilder.addInjection(injector, factory);
        }

    }
}