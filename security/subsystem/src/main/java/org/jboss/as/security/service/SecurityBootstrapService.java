/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.security.service;

import java.lang.reflect.Constructor;
import java.security.Policy;
import java.util.Properties;
import java.util.Set;

import javax.security.jacc.PolicyContext;

import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.security.SecurityExtension;
import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.as.security.plugins.ModuleClassLoaderLocator;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Values;
import org.jboss.security.SecurityConstants;
import org.jboss.security.auth.callback.CallbackHandlerPolicyContextHandler;
import org.jboss.security.jacc.SubjectPolicyContextHandler;
import org.jboss.security.plugins.ClassLoaderLocatorFactory;
import org.jboss.security.plugins.JBossPolicyRegistration;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Bootstrap service for the security container
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author Anil Saldhana
 */
public class SecurityBootstrapService implements Service<Void> {

    static final String JACC_MODULE = "org.jboss.as.security.jacc-module";

    public static final ServiceName SERVICE_NAME = SecurityExtension.JBOSS_SECURITY.append("bootstrap");

    private static final SecurityLogger log = SecurityLogger.ROOT_LOGGER;

    private final InjectedValue<ServiceModuleLoader> moduleLoaderValue = new InjectedValue<ServiceModuleLoader>();


    protected volatile Properties securityProperty;

    private Policy oldPolicy;

    /**
     * This is static because we want to keep using the same one after reload
     *
     * see https://issues.jboss.org/browse/WFLY-10066
     */
    private static volatile Policy jaccPolicy;

    private final boolean initializeJacc;

    private static final String JACC_POLICY_PROVIDER = "javax.security.jacc.policy.provider";

    private static final String POLICY_REGISTRATION = "policyRegistration";

    public SecurityBootstrapService(boolean initializeJacc) {
        this.initializeJacc = initializeJacc;
    }

    /** {@inheritDoc} */
    @Override
    public void start(StartContext context) throws StartException {
        log.debugf("Starting SecurityBootstrapService");
        //Print out the current version of PicketBox
        SecurityLogger.ROOT_LOGGER.currentVersion(org.picketbox.Version.VERSION);
        initializeJacc();
        setupPolicyRegistration(context);
    }

    private void initializeJacc() throws StartException {
        if (!initializeJacc) {
            SecurityLogger.ROOT_LOGGER.debugf("Legacy subsystem configured to not initialize JACC. If you want JACC support, make sure you have it properly configured in Elytron subsystem.");
            return;
        }
        SecurityLogger.ROOT_LOGGER.debugf("Initializing JACC from legacy subsystem.");
        try {
            // Get the current Policy impl
            oldPolicy = Policy.getPolicy();
            if(jaccPolicy == null) {
                String module = WildFlySecurityManager.getPropertyPrivileged(JACC_MODULE, null);
                String provider = WildFlySecurityManager.getPropertyPrivileged(JACC_POLICY_PROVIDER, "org.jboss.security.jacc.DelegatingPolicy");
                Class<?> providerClass = loadClass(module, provider);
                try {
                    // Look for a ctor(Policy) signature
                    Class<?>[] ctorSig = {Policy.class};
                    Constructor<?> ctor = providerClass.getConstructor(ctorSig);
                    Object[] ctorArgs = {oldPolicy};
                    jaccPolicy = (Policy) ctor.newInstance(ctorArgs);
                } catch (NoSuchMethodException e) {
                    log.debugf("Provider does not support ctor(Policy)");
                    try {
                        jaccPolicy = (Policy) providerClass.newInstance();
                    } catch (Exception e1) {
                        throw SecurityLogger.ROOT_LOGGER.unableToStartException("SecurityBootstrapService", e1);
                    }
                } catch (Exception e) {
                    throw SecurityLogger.ROOT_LOGGER.unableToStartException("SecurityBootstrapService", e);
                }
            }

            // Install the JACC policy provider
            Policy.setPolicy(jaccPolicy);

            // Have the policy load/update itself
            jaccPolicy.refresh();

            // Register the default active Subject PolicyContextHandler
            SubjectPolicyContextHandler handler = new SubjectPolicyContextHandler();
            PolicyContext.registerHandler(SecurityConstants.SUBJECT_CONTEXT_KEY, handler, true);

            // Register the JAAS CallbackHandler JACC PolicyContextHandlers
            CallbackHandlerPolicyContextHandler chandler = new CallbackHandlerPolicyContextHandler();
            PolicyContext.registerHandler(SecurityConstants.CALLBACK_HANDLER_KEY, chandler, true);

            //Register a module classloader locator
            ClassLoaderLocatorFactory.set(new ModuleClassLoaderLocator(moduleLoaderValue.getValue()));
        } catch (Exception e) {
            throw SecurityLogger.ROOT_LOGGER.unableToStartException("SecurityBootstrapService", e);
        }
    }

    private void setupPolicyRegistration(final StartContext context) {
        ServiceTarget target = context.getChildTarget();
        final BinderService binderService = new BinderService(POLICY_REGISTRATION);
        target.addService(ContextNames.buildServiceName(ContextNames.JAVA_CONTEXT_SERVICE_NAME, POLICY_REGISTRATION), binderService)
                .addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .addInjection(binderService.getManagedObjectInjector(), new ValueManagedReferenceFactory(
                        Values.immediateValue(new JBossPolicyRegistration())))
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();

    }

    private Class<?> loadClass(final String module, final String className) throws ClassNotFoundException, ModuleLoadException {
        if (module != null) {
            return SecurityActions.getModuleClassLoader(module).loadClass(className);
        }

        return SecurityActions.loadClass(className);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    @Override
    public void stop(StopContext context) {
        // remove handlers
        Set handlerKeys = PolicyContext.getHandlerKeys();
        handlerKeys.remove(SecurityConstants.CALLBACK_HANDLER_KEY);
        handlerKeys.remove(SecurityConstants.SUBJECT_CONTEXT_KEY);

        // Install the policy provider that existed on startup
        if (jaccPolicy != null)
            Policy.setPolicy(oldPolicy);
    }

    /** {@inheritDoc} */
    @Override
    public Void getValue() throws IllegalStateException {
        return null;
    }

    public Injector<ServiceModuleLoader> getServiceModuleLoaderInjectedValue() {
        return moduleLoaderValue;
    }
}