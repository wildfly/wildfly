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

package org.jboss.as.test.integration.ejb.remote.client.api.tx;

import org.jboss.logging.Logger;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author Jaikiran Pai
 */
@Stateless
@Remote(RemoteBatch.class)
@TransactionAttribute (TransactionAttributeType.MANDATORY)
public class BatchCreationBean implements RemoteBatch {

    private static final Logger logger = Logger.getLogger(BatchCreationBean.class);

    @PersistenceContext(unitName = "ejb-client-tx-pu")
    private EntityManager entityManager;

    public void createBatch(final String batchName) {
        final Batch batch = new Batch();
        batch.setBatchName(batchName);
        logger.trace("Persisting new batch " + batchName);
        this.entityManager.persist(batch);
    }

    public void step1(final String batchName, final String stepName) {
        this.addStepToBatch(batchName, stepName);
    }

    public void successfulStep2(final String batchName, final String stepName) {
        this.addStepToBatch(batchName, stepName);
    }

    public void appExceptionFailingStep2(final String batchName, final String stepName) throws SimpleAppException {
        this.addStepToBatch(batchName, stepName);
        throw new SimpleAppException();
    }

    public void systemExceptionFailingStep2(final String batchName, final String stepName) {
        this.addStepToBatch(batchName, stepName);
        throw new RuntimeException("Intentional exception from " + this.getClass().getSimpleName());
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void independentStep3(final String batchName, final String stepName) {
        this.addStepToBatch(batchName, stepName);
    }

    public void step4(final String batchName, final String stepName) {
        this.addStepToBatch(batchName, stepName);
    }

    private Batch requireBatch(final String batchName) {
        final Batch batch = this.entityManager.find(Batch.class, batchName);
        if (batch == null) {
            throw new IllegalArgumentException("No such batch named " + batchName);
        }
        return batch;
    }

    private void addStepToBatch(final String batchName, final String stepName) {
        final Batch batch = this.requireBatch(batchName);
        batch.addStep(stepName);
        this.entityManager.persist(batch);
    }
}
