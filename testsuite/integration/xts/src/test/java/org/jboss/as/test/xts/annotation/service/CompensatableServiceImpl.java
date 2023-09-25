/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.xts.annotation.service;

import com.arjuna.mw.wst.TxContext;
import com.arjuna.mw.wst11.BusinessActivityManager;
import org.jboss.logging.Logger;
import org.jboss.narayana.compensations.api.Compensatable;
import org.jboss.narayana.compensations.api.CompensationTransactionType;

import jakarta.ejb.Stateless;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@Stateless
@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
@Compensatable(CompensationTransactionType.SUPPORTS)
public class CompensatableServiceImpl implements CompensatableService {

    private static Logger LOG = Logger.getLogger(CompensatableServiceImpl.class);

    @Override
    public boolean isTransactionActive() {
        LOG.debug("CompensatableServiceImpl.isTransactionActive()");

        TxContext txContext = null;

        try {
            txContext = BusinessActivityManager.getBusinessActivityManager().currentTransaction();
        } catch (com.arjuna.wst.SystemException e) {
        }

        return txContext != null;
    }
}
