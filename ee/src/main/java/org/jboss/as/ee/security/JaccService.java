/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.security;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;
import static org.wildfly.common.Assert.checkNotNullParam;
import static org.wildfly.security.authz.jacc.PolicyUtil.getPolicyUtil;

import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyContextException;

import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A service for Jakarta Authorization policies
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public abstract class JaccService<T> implements Service<PolicyConfiguration> {

    private static final String JACC_MODULE = "org.jboss.as.security.jacc-module";

    public static final ServiceName SERVICE_NAME = ServiceName.of("jacc");

    protected final String contextId;

    private final T metaData;

    private final Boolean standalone;

    private volatile PolicyConfiguration policyConfiguration;

    private final InjectedValue<PolicyConfiguration> parentPolicy = new InjectedValue<PolicyConfiguration>();

    public JaccService(final String contextId, T metaData, Boolean standalone) {
        checkNotNullParam(contextId, contextId);
        this.contextId = contextId;
        this.metaData = metaData;
        this.standalone = standalone;
    }

    /** {@inheritDoc} */
    @Override
    public PolicyConfiguration getValue() throws IllegalStateException, IllegalArgumentException {
        return policyConfiguration;
    }

    /** {@inheritDoc} */
    @Override
    public void start(StartContext context) throws StartException {
        try {
            PolicyConfigurationFactory pcf = getPolicyConfigurationFactory();
            synchronized (pcf) { // synchronize on the factory
                policyConfiguration = pcf.getPolicyConfiguration(contextId, false);
                if (metaData != null) {
                    createPermissions(metaData, policyConfiguration);
                } else {
                    ROOT_LOGGER.debugf("Cannot create permissions with 'null' metaData for id=%s", contextId);
                }
                if (!standalone) {
                    PolicyConfiguration parent = parentPolicy.getValue();
                    if (parent != null) {
                        parent = pcf.getPolicyConfiguration(parent.getContextID(), false);
                        parent.linkConfiguration(policyConfiguration);
                        policyConfiguration.commit();
                        parent.commit();
                    } else {
                        ROOT_LOGGER.debugf("Could not retrieve parent policy for policy %s", contextId);
                    }
                } else {
                    policyConfiguration.commit();
                }
                // Allow the policy to incorporate the policy configs
                getPolicyUtil().refresh();
            }
            beginContextPolicy();
        } catch (Exception e) {
            throw ROOT_LOGGER.unableToStartException("JaccService", e);
        }
    }

    private PolicyConfigurationFactory getPolicyConfigurationFactory() throws ModuleLoadException, ClassNotFoundException, PolicyContextException {
        String module = WildFlySecurityManager.getPropertyPrivileged(JACC_MODULE, null);
        final ClassLoader originalClassLoader;
        final ClassLoader jaccClassLoader;
        if (module != null) {
            jaccClassLoader = SecurityActions.getModuleClassLoader(module);
            originalClassLoader = SecurityActions.setThreadContextClassLoader(jaccClassLoader);
        } else {
            jaccClassLoader = null;
            originalClassLoader = null;
        }

        try {
            return PolicyConfigurationFactory.getPolicyConfigurationFactory();
        } finally {
            if (originalClassLoader != null) {
                SecurityActions.setThreadContextClassLoader(originalClassLoader);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stop(StopContext context) {
        try {
            PolicyConfigurationFactory pcf = PolicyConfigurationFactory.getPolicyConfigurationFactory();
            synchronized (pcf) { // synchronize on the factory
                policyConfiguration = pcf.getPolicyConfiguration(contextId, false);
                policyConfiguration.delete();
            }
            endContextPolicy();
        } catch (Exception e) {
            ROOT_LOGGER.errorDeletingJACCPolicy(e);
        }
        policyConfiguration = null;
    }

    /**
     * Target {@code Injector}
     *
     * @return target
     */
    public Injector<PolicyConfiguration> getParentPolicyInjector() {
        return parentPolicy;
    }

    /**
     * Create JACC permissions for the deployment
     *
     * @param metaData
     * @param policyConfiguration
     * @throws PolicyContextException
     */
    public abstract void createPermissions(T metaData, PolicyConfiguration policyConfiguration) throws PolicyContextException;

    /**
     * Begin any Policy set up for this Context.
     */
    public void beginContextPolicy() throws SecurityException {};

    /**
     * End any Policy set up for this Context.
     */
    public void endContextPolicy() throws SecurityException {};

}
