package org.jboss.as.ee.component.deployers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.interceptor.Interceptors;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InterceptorDescription;
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ee.metadata.MethodAnnotationAggregator;
import org.jboss.as.ee.metadata.RuntimeAnnotationInformation;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.DeploymentClassIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;

/**
 * Processor that takes interceptor information from the class description and applies it to components
 *
 * @author Stuart Douglas
 */
public class InterceptorAnnotationProcessor implements DeploymentUnitProcessor {


    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final DeploymentClassIndex module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.CLASS_INDEX);
        final Collection<ComponentDescription> componentConfigurations = eeModuleDescription.getComponentDescriptions();
        final DeploymentReflectionIndex deploymentReflectionIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);

        if (MetadataCompleteMarker.isMetadataComplete(deploymentUnit)) {
            return;
        }
        if (componentConfigurations == null || componentConfigurations.isEmpty()) {
            return;
        }

        for (final ComponentDescription description : componentConfigurations) {
            processComponentConfig(applicationClasses, module, deploymentReflectionIndex, description);
        }
    }

    private void processComponentConfig(final EEApplicationClasses applicationClasses, final DeploymentClassIndex classIndex, final DeploymentReflectionIndex deploymentReflectionIndex, final ComponentDescription description) throws DeploymentUnitProcessingException {

        final Class<?> componentClass;
        try {
            componentClass = classIndex.classIndex(description.getComponentClassName()).getModuleClass();
        } catch (ClassNotFoundException e) {
            //just ignore the class for now.
            //if it is an optional component this is ok, if it is not an optional component
            //it will fail at configure time anyway
            return;
        }
        handleAnnotations(applicationClasses, deploymentReflectionIndex, componentClass, description);
    }


    private void handleAnnotations(final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final ComponentDescription description) {

        final List<Class> heirachy = new ArrayList<Class>();
        Class c = componentClass;
        while (c != Object.class && c != null) {
            heirachy.add(c);
            c = c.getSuperclass();
        }
        Collections.reverse(heirachy);


        final RuntimeAnnotationInformation<Boolean> excludeDefaultInterceptors = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, ExcludeDefaultInterceptors.class);
        if (excludeDefaultInterceptors.getClassAnnotations().containsKey(componentClass.getName())) {
            description.setExcludeDefaultInterceptors(true);
        }
        for (final Map.Entry<Method, List<Boolean>> entry : excludeDefaultInterceptors.getMethodAnnotations().entrySet()) {
            description.excludeDefaultInterceptors(MethodIdentifier.getIdentifierForMethod(entry.getKey()));
        }
        final RuntimeAnnotationInformation<Boolean> excludeClassInterceptors = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, ExcludeClassInterceptors.class);
        for (final Map.Entry<Method, List<Boolean>> entry : excludeClassInterceptors.getMethodAnnotations().entrySet()) {
            description.excludeClassInterceptors(MethodIdentifier.getIdentifierForMethod(entry.getKey()));
        }

        final RuntimeAnnotationInformation<String[]> interceptors = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, Interceptors.class);

        //walk the class heirachy in reverse
        for (final Class<?> clazz : heirachy) {
            final List<String[]> classInterceptors = interceptors.getClassAnnotations().get(clazz.getName());
            if (classInterceptors != null) {
                for (final String interceptor : classInterceptors.get(0)) {
                    description.addClassInterceptor(new InterceptorDescription(interceptor));
                }
            }
        }

        for (final Map.Entry<Method, List<String[]>> entry : interceptors.getMethodAnnotations().entrySet()) {
            final MethodIdentifier method = MethodIdentifier.getIdentifierForMethod(entry.getKey());
            for (final String interceptor : entry.getValue().get(0)) {
                description.addMethodInterceptor(method, new InterceptorDescription(interceptor));
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }

}
