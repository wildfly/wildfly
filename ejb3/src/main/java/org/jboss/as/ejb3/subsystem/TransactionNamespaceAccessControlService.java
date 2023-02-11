/*
 * JBoss, Home of Professional Open Source
 * Copyright 2023, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.subsystem;

import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;;

import org.jboss.as.naming.InitialContext;
import org.jboss.as.naming.LookupInterceptor;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class TransactionNamespaceAccessControlService implements Service {
    private static final String REMOTE_USER_TRANSACTION = "RemoteUserTransaction";
    private static final String LOCAL_USER_TRANSACTION = "LocalUserTransaction";

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "TransactionNamespaceAccessControlService");

    private LookupInterceptor userTransactionLookupInterceptor = new LookupInterceptor() {

        @Override
        public void aroundLookup(Name name) throws NamingException {
            if (requiresCheckAllowed(name)) {
                try {
                    AllowedMethodsInformation.checkAllowed(MethodType.GET_USER_TRANSACTION);
                } catch (IllegalStateException e) {
                    throw new NamingException(e.getMessage());
                }
            }
        }

        private boolean requiresCheckAllowed(Name origName) throws InvalidNameException {
            if (origName.isEmpty() || !origName.get(0).startsWith("txn:")) {
                return false;
            }
            final Name reparsedName = (Name) origName.clone();
            final String first = reparsedName.get(0);
            final int idx = first.indexOf(':');
            final String segment = first.substring(idx + 1);
            reparsedName.remove(0);
            if (segment.length() > 0 || (origName.size() > 1 && origName.get(1).length() > 0)) {
                reparsedName.add(0, segment);
            }
            if (reparsedName.toString().equals(REMOTE_USER_TRANSACTION) || reparsedName.toString().equals(LOCAL_USER_TRANSACTION)) {
                return true;
            } else {
                return false;
            }
        }
    };

    @Override
    public void start(StartContext context) {
        InitialContext.getInitialContextFactory().addInterceptor(userTransactionLookupInterceptor);
    }

    @Override
    public void stop(StopContext context) {
        InitialContext.getInitialContextFactory().removeInterceptor(userTransactionLookupInterceptor);
    }
}
