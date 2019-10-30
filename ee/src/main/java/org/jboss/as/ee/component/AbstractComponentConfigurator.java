package org.jboss.as.ee.component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndexUtil;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.Interceptors;
import org.jboss.msc.value.InjectedValue;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

/**
 * @author Stuart Douglas
 */
public class AbstractComponentConfigurator {

    /**
     * The weaved interceptor factory results in a lot of runtime allocations, and should not be used
     * @param interceptorFactories The interceptor factories
     * @return
     */
    @Deprecated
    protected static InterceptorFactory weaved(final Collection<InterceptorFactory> interceptorFactories) {
        if(interceptorFactories == null) {
            return null;
        }
        return new InterceptorFactory() {
            @Override
            public Interceptor create(InterceptorFactoryContext context) {
                final Interceptor[] interceptors = new Interceptor[interceptorFactories.size()];
                final Iterator<InterceptorFactory> factories = interceptorFactories.iterator();
                for (int i = 0; i < interceptors.length; i++) {
                    interceptors[i] = factories.next().create(context);
                }
                return Interceptors.getWeavedInterceptor(interceptors);
            }
        };
    }

    /**
     * Sets up all resource injections for a class. This takes into account injections that have been specified in the module and component deployment descriptors
     * <p/>
     * Note that this does not take superclasses into consideration, only injections on the current class
     *
     * @param clazz             The class or superclass to perform injection for
     * @param actualClass       The actual component or interceptor class
     * @param classDescription  The class description, may be null
     * @param moduleDescription The module description
     * @param description       The component description
     * @param configuration     The component configuration
     * @param context           The phase context
     * @param injectors         The list of injectors for the current component
     * @param instanceKey       The key that identifies the instance to inject in the interceptor context
     * @param uninjectors       The list of uninjections for the current component
     * @throws org.jboss.as.server.deployment.DeploymentUnitProcessingException
     *
     */
    protected void mergeInjectionsForClass(final Class<?> clazz, final Class<?> actualClass, final EEModuleClassDescription classDescription, final EEModuleDescription moduleDescription, final DeploymentReflectionIndex deploymentReflectionIndex, final ComponentDescription description, final ComponentConfiguration configuration, final DeploymentPhaseContext context, final Deque<InterceptorFactory> injectors, final Object instanceKey, final Deque<InterceptorFactory> uninjectors, boolean metadataComplete) throws DeploymentUnitProcessingException {
        final Map<InjectionTarget, ResourceInjectionConfiguration> mergedInjections = new HashMap<InjectionTarget, ResourceInjectionConfiguration>();
        if (classDescription != null && !metadataComplete) {
            mergedInjections.putAll(classDescription.getInjectionConfigurations());
        }
        mergedInjections.putAll(moduleDescription.getResourceInjections(clazz.getName()));
        mergedInjections.putAll(description.getResourceInjections(clazz.getName()));

        for (final ResourceInjectionConfiguration injectionConfiguration : mergedInjections.values()) {
            if(!moduleDescription.isAppClient() && injectionConfiguration.getTarget().isStatic(context.getDeploymentUnit())) {
                ROOT_LOGGER.debugf("Injection for a member with static modifier is only acceptable on application clients, ignoring injection for target %s",injectionConfiguration.getTarget());
                continue;
            }
            if(injectionConfiguration.getTarget() instanceof MethodInjectionTarget) {
                //we need to make sure that if this is a method injection it has not been overriden
                final MethodInjectionTarget mt = (MethodInjectionTarget)injectionConfiguration.getTarget();
                Method method = mt.getMethod(deploymentReflectionIndex, clazz);
                if(!isNotOverriden(clazz, method, actualClass, deploymentReflectionIndex)) {
                    continue;
                }
            }

            final Object valueContextKey = new Object();
            final InjectedValue<ManagedReferenceFactory> managedReferenceFactoryValue = new InjectedValue<ManagedReferenceFactory>();
            configuration.getStartDependencies().add(new ComponentDescription.InjectedConfigurator(injectionConfiguration, configuration, context, managedReferenceFactoryValue));
            injectors.addFirst(injectionConfiguration.getTarget().createInjectionInterceptorFactory(instanceKey, valueContextKey, managedReferenceFactoryValue, context.getDeploymentUnit(), injectionConfiguration.isOptional()));
            uninjectors.addLast(new ImmediateInterceptorFactory(new ManagedReferenceReleaseInterceptor(valueContextKey)));
        }
    }

    protected boolean isNotOverriden(final Class<?> clazz, final Method method, final Class<?> actualClass, final DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
        return Modifier.isPrivate(method.getModifiers()) || ClassReflectionIndexUtil.findRequiredMethod(deploymentReflectionIndex, actualClass, method).getDeclaringClass() == clazz;
    }
}
