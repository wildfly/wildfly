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

import org.jboss.as.test.integration.transactions.PersistentTestXAResource;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.TransactionManager;

/**
 * Bean expected to be called by {@link ClientBean}.
 * Working with {@link javax.transaction.xa.XAResource}s to force the transaction manager to process
 * two-phase commit after the end of the business method.
 */
@Stateless
public class TransactionalBean implements TransactionalRemote {
    private static final Logger log = Logger.getLogger(TransactionalBean.class);

    @EJB
    private TransactionCheckerSingleton transactionCheckerSingleton;

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager tm;

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void enlistOnePersistentXAResource() {
        try {
            log.debugf("Invocation to #enlistOnePersistentXAResource with transaction", tm.getTransaction());
            tm.getTransaction().enlistResource(new PersistentTestXAResource(transactionCheckerSingleton));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot enlist single PersistentTestXAResource to transaction", e);
        }
    }

}
