/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.security;

import static org.wildfly.common.Assert.checkNotNullParam;

import java.security.GeneralSecurityException;
import java.security.Permission;
import java.util.Map.Entry;

import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyContextException;

import org.jboss.as.ee.security.JaccService;
import org.jboss.as.server.deployment.AttachmentList;
import org.wildfly.security.jakarta.authz.PolicyRegistration;

/**
 * A service that creates JACC permissions for an ejb deployment
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author Scott.Stark@jboss.org
 * @author Anil.Saldhana@jboss.org
 * @author Stuart Douglas
 */
public class EjbJaccService extends JaccService<AttachmentList<EjbJaccConfig>> {

    private final ClassLoader deploymentClassLoader;

    public EjbJaccService(String contextId, AttachmentList<EjbJaccConfig> metaData, Boolean standalone, ClassLoader deploymClassLoader) {
        super(contextId, metaData, standalone);
        this.deploymentClassLoader = checkNotNullParam("deploymentClassLoader", deploymClassLoader);
    }

    @Override
    public void createPermissions(final AttachmentList<EjbJaccConfig> metaData, final PolicyConfiguration policyConfiguration) throws PolicyContextException {
        for (EjbJaccConfig permission : metaData) {
            for (Permission deny : permission.getDeny()) {
                policyConfiguration.addToExcludedPolicy(deny);
            }
            for (Permission permit : permission.getPermit()) {
                policyConfiguration.addToUncheckedPolicy(permit);
            }
            for (Entry<String, Permission> role : permission.getRoles()) {
                policyConfiguration.addToRole(role.getKey(), role.getValue());
            }
        }
    }

    @Override
    public void beginContextPolicy() throws GeneralSecurityException {
        PolicyRegistration.beginContextPolicy(contextId, deploymentClassLoader);
    }

    @Override
    public void endContextPolicy() throws GeneralSecurityException {
        PolicyRegistration.endContextPolicy(contextId);
    }


}
