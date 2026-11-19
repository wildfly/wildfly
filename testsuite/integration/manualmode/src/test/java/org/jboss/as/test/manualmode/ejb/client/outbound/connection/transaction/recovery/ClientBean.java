/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ejb.client.outbound.connection.transaction.recovery;

import java.util.Hashtable;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.as.test.integration.transactions.TestXAResource;

@Stateless
public class ClientBean implements ClientBeanRemote {

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager tm;

    @Override
    public void beginTransactionAndCrash(String remoteDeploymentName) {
        TransactionalBeanRemote remoteBean = getRemote(remoteDeploymentName);
        remoteBean.enlistPersistentXAResource();
        try {
            tm.getTransaction().enlistResource(new TestXAResource(TestXAResource.TestAction.PREPARE_CRASH_VM));
        } catch (SystemException | RollbackException e) {
            throw new RuntimeException("Cannot enlist crash resource to current transaction", e);
        }
    }

    private TransactionalBeanRemote getRemote(String remoteDeployment) {
        String lookup = "ejb:/" + remoteDeployment + "//" + "TransactionalBean" + "!" + TransactionalBeanRemote.class.getName();
        try {
            Hashtable<String, String> props = new Hashtable<>();
            props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
            Context context = new javax.naming.InitialContext(props);
            return (TransactionalBeanRemote) context.lookup(lookup);
        } catch (NamingException e) {
            throw new RuntimeException("Cannot look up remote bean at: " + lookup, e);
        }
    }
}
