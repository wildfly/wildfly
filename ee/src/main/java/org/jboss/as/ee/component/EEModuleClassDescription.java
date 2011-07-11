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

package org.jboss.as.ee.component;

import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.msc.value.ConstructedValue;
import org.jboss.msc.value.Value;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;

import static org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX;

/**
 * The description of a (possibly annotated) class in an EE module.
 *
 * This class must be thread safe as it may be used by sub deployments at the same time
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EEModuleClassDescription {

    private static final DefaultConfigurator DEFAULT_CONFIGURATOR = new DefaultConfigurator();

    private final String className;
    private final Deque<ClassConfigurator> configurators = new LinkedBlockingDeque();
    private MethodIdentifier postConstructMethod;
    private MethodIdentifier preDestroyMethod;
    private MethodIdentifier aroundInvokeMethod;
    private boolean invalid;
    private StringBuilder invalidMessageBuilder;

    public EEModuleClassDescription(final String className) {
        this.className = className;
        configurators.addFirst(DEFAULT_CONFIGURATOR);
    }

    /**
     * Get the class name of this EE module class.
     *
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Get the method, if any, which has been marked as an around-invoke interceptor.
     *
     * @return the around-invoke method or {@code null} for none
     */
    public MethodIdentifier getAroundInvokeMethod() {
        return aroundInvokeMethod;
    }

    /**
     * Set the method which has been marked as an around-invoke interceptor.
     *
     * @param aroundInvokeMethod the around-invoke method or {@code null} for none
     */
    public void setAroundInvokeMethod(final MethodIdentifier aroundInvokeMethod) {
        this.aroundInvokeMethod = aroundInvokeMethod;
    }

    /**
     * Get the method, if any, which has been marked as a post-construct interceptor.
     *
     * @return the post-construct method or {@code null} for none
     */
    public MethodIdentifier getPostConstructMethod() {
        return postConstructMethod;
    }

    /**
     * Set the method which has been marked as a post-construct interceptor.
     *
     * @param postConstructMethod the post-construct method or {@code null} for none
     */
    public void setPostConstructMethod(final MethodIdentifier postConstructMethod) {
        this.postConstructMethod = postConstructMethod;
    }

    /**
     * Get the method, if any, which has been marked as a pre-destroy interceptor.
     *
     * @return the pre-destroy method or {@code null} for none
     */
    public MethodIdentifier getPreDestroyMethod() {
        return preDestroyMethod;
    }

    /**
     * Set the method which has been marked as a pre-destroy interceptor.
     *
     * @param preDestroyMethod the pre-destroy method or {@code null} for none
     */
    public void setPreDestroyMethod(final MethodIdentifier preDestroyMethod) {
        this.preDestroyMethod = preDestroyMethod;
    }

    /**
     * Get the configurators for this class.
     *
     * @return the configurators
     */
    public Deque<ClassConfigurator> getConfigurators() {
        return configurators;
    }

    private static class DefaultConfigurator implements ClassConfigurator {

        private static final Class<?>[] NO_CLASSES = new Class<?>[0];

        public void configure(final DeploymentPhaseContext context, final EEModuleClassDescription description, final EEModuleClassConfiguration configuration) throws DeploymentUnitProcessingException {
            DeploymentReflectionIndex index = context.getDeploymentUnit().getAttachment(REFLECTION_INDEX);
            Class<?> moduleClass = configuration.getModuleClass();
            ClassReflectionIndex<?> classIndex = index.getClassIndex(moduleClass);
            // Use the basic instantiator if none was set up
            if (configuration.getInstantiator() == null) {
                Constructor<?> constructor = classIndex.getConstructor(NO_CLASSES);
                if (constructor != null) {
                    configuration.setInstantiator(new ValueManagedReferenceFactory(createConstructedValue(constructor)));
                }
            }
        }

        private static <T> ConstructedValue<T> createConstructedValue(final Constructor<T> constructor) {
            return new ConstructedValue<T>(constructor, Collections.<Value<?>>emptyList());
        }
    }

    public synchronized void setInvalid(String message) {
        if(!invalid) {
            invalid = true;
            invalidMessageBuilder = new StringBuilder();
        } else {
            invalidMessageBuilder.append('\n');
        }
        invalidMessageBuilder.append(message);
    }

    public boolean isInvalid() {
        return invalid;
    }

    public String getInvalidMessage() {
        if(invalidMessageBuilder == null) {
            return "";
        }
        return invalidMessageBuilder.toString();
    }
}
