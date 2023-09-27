/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.security;

import java.security.Permission;
import java.util.Map.Entry;

import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyContextException;

import org.jboss.as.ee.security.JaccService;
import org.jboss.as.server.deployment.AttachmentList;

/**
 * A service that creates JACC permissions for an ejb deployment
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author Scott.Stark@jboss.org
 * @author Anil.Saldhana@jboss.org
 * @author Stuart Douglas
 */
public class EjbJaccService extends JaccService<AttachmentList<EjbJaccConfig>> {

    public EjbJaccService(String contextId, AttachmentList<EjbJaccConfig> metaData, Boolean standalone) {
        super(contextId, metaData, standalone);
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
}
