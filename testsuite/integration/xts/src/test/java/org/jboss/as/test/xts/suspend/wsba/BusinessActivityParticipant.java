/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.suspend.wsba;

import com.arjuna.wst.BusinessAgreementWithParticipantCompletionParticipant;
import com.arjuna.wst.FaultedException;
import com.arjuna.wst.Status;
import com.arjuna.wst.SystemException;
import com.arjuna.wst.WrongStateException;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class BusinessActivityParticipant implements BusinessAgreementWithParticipantCompletionParticipant {

    private static final Logger LOGGER = Logger.getLogger(BusinessActivityParticipant.class);

    private static final List<String> INVOCATIONS = new ArrayList<>();

    private final String id;

    public BusinessActivityParticipant(String id) {
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
    public void close() throws WrongStateException, SystemException {
        INVOCATIONS.add("close");
        LOGGER.debugf("close call on %s", this);
    }

    @Override
    public void cancel() throws FaultedException, WrongStateException, SystemException {
        INVOCATIONS.add("cancel");
        LOGGER.debugf("cancel call on %s", this);
    }

    @Override
    public void compensate() throws FaultedException, WrongStateException, SystemException {
        INVOCATIONS.add("compensate");
        LOGGER.debugf("compensate call on %s", this);
    }

    @Override
    public String status() throws SystemException {
        INVOCATIONS.add("status");
        LOGGER.debugf("status call on %s", this);
        return Status.STATUS_ACTIVE;
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
