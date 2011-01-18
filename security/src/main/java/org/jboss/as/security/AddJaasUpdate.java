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

package org.jboss.as.security;

import java.util.List;

import javax.security.auth.login.Configuration;

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.security.service.JaasConfigurationService;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.security.auth.login.XMLLoginConfigImpl;
import org.jboss.security.config.ApplicationPolicy;
import org.jboss.security.config.ApplicationPolicyRegistration;

/**
 * Update to add JAAS configuration
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public final class AddJaasUpdate extends AbstractSecuritySubsystemUpdate<Void> {

    private static final long serialVersionUID = -1169438216018092782L;

    private List<ApplicationPolicy> applicationPolicies;

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final ServiceTarget target = updateContext.getServiceTarget();

        // add parsed security domains to the Configuration
        final Configuration loginConfig = XMLLoginConfigImpl.getInstance();
        addParsedApplicationPolicies(loginConfig, applicationPolicies);

        // add jaas configuration service
        final JaasConfigurationService jaasConfigurationService = new JaasConfigurationService(loginConfig);
        target.addService(JaasConfigurationService.SERVICE_NAME, jaasConfigurationService).setInitialMode(
                ServiceController.Mode.ACTIVE).install();
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<SecuritySubsystemElement, ?> getCompensatingUpdate(SecuritySubsystemElement original) {
        return new RemoveJaasUpdate();
    }

    /** {@inheritDoc} */
    protected void applyUpdate(SecuritySubsystemElement element) throws UpdateFailedException {
        JaasElement jaas = new JaasElement();
        jaas.setApplicationPolicies(applicationPolicies);
        element.setJaas(jaas);
    }

    /**
     * Sets application policies list
     *
     * @param securityDomains list
     */
    public void setApplicationPolicies(List<ApplicationPolicy> applicationPolicies) {
        this.applicationPolicies = applicationPolicies;
    }

    /**
     * Adds parsed application policies to the installed Configuration
     *
     * @param configuration installed Configuration
     * @param applicationPolicies list of parsed application policy elements
     */
    public static void addParsedApplicationPolicies(Configuration configuration, List<ApplicationPolicy> applicationPolicies) {
        ApplicationPolicyRegistration policyRegistration = (ApplicationPolicyRegistration) configuration;
        if (applicationPolicies != null) {
            for (ApplicationPolicy applicationPolicy : applicationPolicies) {
                policyRegistration.addApplicationPolicy(applicationPolicy.getName(), applicationPolicy);
            }
        }
    }

}
