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

package org.jboss.as.ejb3.security;

import java.security.Permission;
import java.util.Map.Entry;

import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyContextException;

import org.jboss.as.security.service.JaccService;
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
