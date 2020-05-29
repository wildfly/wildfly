/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.ejb.client.outbound.connection.transaction.preparehalt;

import org.jboss.as.test.integration.transactions.TestXAResource;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import java.util.Hashtable;

/**
 * A bean which uses remote outbound connection configured
 * for calling the second instance of the app server via EJB remoting.
 */
@Stateless
public class ClientBean implements ClientBeanRemote {
    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager tm;

    public void twoPhaseCommitCrashAtClient(String remoteDeploymentName) {
        TransactionalRemote bean = getRemote(remoteDeploymentName);
        bean.enlistOnePersistentXAResource();
        try {
            // Enlisting resource for having the 2PC started (the second resource is the EJB remote call)
            // the resource crashes the VM at start of the XAResource.prepare method
            // We depend on the transaction manager strict ordering - the order of resource enlistment
            // matches the order during 2PC processing
            tm.getTransaction().enlistResource(new TestXAResource(TestXAResource.TestAction.PREPARE_CRASH_VM));
        } catch (SystemException | RollbackException e) {
            throw new RuntimeException("Cannot enlist " + TestXAResource.class.getSimpleName() + " to the current transaction", e);
        }
    }

    public void twoPhaseIntermittentCommitFailureOnServer(String remoteDeploymentName) {
        TransactionalRemote bean = getRemote(remoteDeploymentName);
        bean.intermittentCommitFailure();
        try {
            // Enlisting second resource to force 2PC being processed
            tm.getTransaction().enlistResource(new TestXAResource());
        } catch (SystemException | RollbackException e) {
            throw new RuntimeException("Cannot enlist " + TestXAResource.class.getSimpleName() + " to the current transaction", e);
        }
    }


    public void onePhaseIntermittentCommitFailureOnServer(String remoteDeploymentName) {
        TransactionalRemote bean = getRemote(remoteDeploymentName);
        // Enlisting only remote EJB bean by the remote call and no other resource to process with 1PC
        // but the remote EJB works with two resources and on 1PC commit it triggers the 2PC
        bean.intermittentCommitFailureTwoPhase();
    }

    private TransactionalRemote getRemote(String remoteDeployment) {
        String lookupBean = "ejb:/" + remoteDeployment + "/"
                + "" + "/" + "TransactionalBean" + "!" + TransactionalRemote.class.getName();
        try {
            final Hashtable props = new Hashtable();
            props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
            final Context context = new javax.naming.InitialContext(props);
            final TransactionalRemote remote = (TransactionalRemote) context.lookup(lookupBean);
            return remote;
        } catch (NamingException ne) {
            throw new RuntimeException("Cannot get bean " + lookupBean, ne);
        }
    }
}
