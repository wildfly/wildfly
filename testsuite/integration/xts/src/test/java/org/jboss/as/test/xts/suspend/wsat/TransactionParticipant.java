/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.suspend.wsat;

import com.arjuna.wst.Prepared;
import com.arjuna.wst.SystemException;
import com.arjuna.wst.Volatile2PCParticipant;
import com.arjuna.wst.Vote;
import com.arjuna.wst.WrongStateException;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class TransactionParticipant implements Volatile2PCParticipant {

    private static final Logger LOGGER = Logger.getLogger(TransactionParticipant.class);

    private static final List<String> INVOCATIONS = new ArrayList<>();

    private final String id;

    public TransactionParticipant(String id) {
        this.id = id;
    }

    public static void resetInvocations() {
        LOGGER.debugf("resetting invocations %s", INVOCATIONS);
        INVOCATIONS.clear();
    }

    public static List<String> getInvocations() {
        LOGGER.debugf("returning invocations %s", INVOCATIONS);
        return Collections.unmodifiableList(INVOCATIONS);
    }

    public String getId() {
        return id;
    }

    @Override
    public Vote prepare() throws WrongStateException, SystemException {
        INVOCATIONS.add("prepare");
        LOGGER.debugf("preparing call on %s", this);
        return new Prepared();
    }

    @Override
    public void commit() throws WrongStateException, SystemException {
        INVOCATIONS.add("commit");
        LOGGER.debugf("commit call on %s", this);
    }

    @Override
    public void rollback() throws WrongStateException, SystemException {
        INVOCATIONS.add("rollback");
        LOGGER.debugf("rollback call on %s", this);
    }

    @Override
    public void unknown() throws SystemException {
        INVOCATIONS.add("unknown");
        LOGGER.debugf("unknown call on %s", this);
    }

    @Override
    public void error() throws SystemException {
        INVOCATIONS.add("error");
        LOGGER.debugf("error call on %s", this);
    }

    @Override
    public String toString() {
        return String.format("%s{id='%s', INVOCATIONS=%s}", this.getClass().getSimpleName(), id, INVOCATIONS);
    }
}
