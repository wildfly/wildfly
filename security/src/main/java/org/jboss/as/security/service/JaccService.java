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

package org.jboss.as.security.service;

import java.security.Policy;

import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContextException;

import org.jboss.as.security.SecurityExtension;
import org.jboss.as.security.SecurityLogger;
import org.jboss.as.security.SecurityMessages;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A service for JACC policies
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public abstract class JaccService<T> implements Service<PolicyConfiguration> {

    protected static final SecurityLogger log = SecurityLogger.ROOT_LOGGER;

    public static final ServiceName SERVICE_NAME = SecurityExtension.JBOSS_SECURITY.append("jacc");

    private final String contextId;

    private final T metaData;

    private final Boolean standalone;

    private volatile PolicyConfiguration policyConfiguration;

    private final InjectedValue<PolicyConfiguration> parentPolicy = new InjectedValue<PolicyConfiguration>();

    public JaccService(final String contextId, T metaData, Boolean standalone) {
        if (contextId == null)
            throw SecurityMessages.MESSAGES.nullArgument("JACC Context Id");
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
            PolicyConfigurationFactory pcf = PolicyConfigurationFactory.getPolicyConfigurationFactory();
            synchronized (pcf) { // synchronize on the factory
                policyConfiguration = pcf.getPolicyConfiguration(contextId, false);
                if (standalone) {
                    if (metaData != null) {
                        createPermissions(metaData, policyConfiguration);
                    } else {
                        log.debugf("Cannot create permissions with 'null' metaData for id=" + contextId);
                    }
                }
                if (!standalone) {
                    PolicyConfiguration parent = parentPolicy.getValue();
                    if (parent != null) {
                        parent = pcf.getPolicyConfiguration(parent.getContextID(), false);
                        parent.linkConfiguration(policyConfiguration);
                        policyConfiguration.commit();
                        parent.commit();
                    } else {
                        log.debugf("Could not retrieve parent policy for policy " + contextId);
                    }
                } else {
                    policyConfiguration.commit();
                }
                // Allow the policy to incorporate the policy configs
                Policy.getPolicy().refresh();
            }
        } catch (Exception e) {
            throw SecurityMessages.MESSAGES.unableToStartException("JaccService", e);
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
        } catch (Exception e) {
            SecurityLogger.ROOT_LOGGER.errorDeletingJACCPolicy(e);
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

}
