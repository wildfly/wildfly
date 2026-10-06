/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.timerservice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.ejb.EJBException;
import jakarta.ejb.Timer;

import org.jboss.ejb3.timerservice.ExtendedTimerService;
import org.wildfly.transaction.client.ContextTransactionManager;

import jakarta.transaction.Transaction;

/**
 * Extension of {@link TimerServiceImpl} that additionally implements {@link ExtendedTimerService}.
 * Instantiated only when the server runs at {@link org.jboss.as.version.Stability#COMMUNITY} or higher,
 * so that the injected {@link jakarta.ejb.TimerService} is castable to {@code ExtendedTimerService} only
 * at that stability level.
 */
public class ExtendedTimerServiceImpl extends TimerServiceImpl implements ExtendedTimerService {

    public ExtendedTimerServiceImpl(TimerServiceConfiguration configuration) {
        super(configuration);
    }

    @Override
    public Collection<Timer> getTimersByExternalId(String externalId) {
        this.validateInvocationContext();
        final Map<String, Timer> timersById = new HashMap<>();

        // 1. Fetch from in-memory timers
        for (final TimerImpl timer : this.timers.values()) {
            if ((timer.isActive() || timer.getState() == TimerState.ACTIVE) && Objects.equals(externalId, timer.getExternalId())) {
                timersById.put(timer.getId(), timer);
            }
        }

        // 2. Fetch from uncommitted transaction timers
        for (final TimerImpl timer : getWaitingOnTxCompletionTimers().values()) {
            if (timer.isActive() && Objects.equals(externalId, timer.getExternalId())) {
                timersById.put(timer.getId(), timer);
            }
        }

        // 3. Fetch from the database store
        if (this.persistence != null) {
            final ContextTransactionManager transactionManager = ContextTransactionManager.getInstance();
            try {
                Transaction clientTX = transactionManager.getTransaction();
                if (clientTX == null) {
                    transactionManager.begin();
                }
                List<TimerImpl> persistedTimers = this.persistence.loadActiveTimersByExternalId(externalId, this);
                if (clientTX == null) {
                    transactionManager.commit();
                }

                for (TimerImpl timer : persistedTimers) {
                    if ((timer.isActive() || timer.getState() == TimerState.ACTIVE) && !timersById.containsKey(timer.getId())) {
                        timersById.put(timer.getId(), timer);
                    }
                }
            } catch (Exception e) {
                try {
                    transactionManager.rollback();
                } catch (Exception ee) {
                    // omit
                }
                throw new EJBException("Failed to fetch timers by external ID from database", e);
            }
        }

        return new ArrayList<>(timersById.values());
    }
}
