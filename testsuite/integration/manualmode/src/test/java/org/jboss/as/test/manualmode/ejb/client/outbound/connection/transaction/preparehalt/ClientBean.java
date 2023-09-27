/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ejb.client.outbound.connection.transaction.preparehalt;

import org.jboss.as.test.integration.transactions.TestXAResource;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import javax.naming.Context;
import javax.naming.NamingException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import java.util.Hashtable;

/**
 * A bean which uses remote outbound connection configured
 * for calling the second instance of the app server via Jakarta Enterprise Beans remoting.
 */
@Stateless
public class ClientBean implements ClientBeanRemote {
    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager tm;

    public void twoPhaseCommitCrashAtClient(String remoteDeploymentName) {
        TransactionalRemote bean = getRemote(remoteDeploymentName);
        bean.enlistOnePersistentXAResource();
        try {
            // Enlisting resource for having the 2PC started (the second resource is the Jakarta Enterprise Beans remote call)
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
        // Enlisting only remote Jakarta Enterprise Beans bean by the remote call and no other resource to process with 1PC
        // but the remote Jakarta Enterprise Beans works with two resources and on 1PC commit it triggers the 2PC
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
