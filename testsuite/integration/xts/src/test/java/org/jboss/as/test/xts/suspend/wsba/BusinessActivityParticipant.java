/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
        LOGGER.infof("resetting invocations %s", INVOCATIONS);
        INVOCATIONS.clear();
    }

    public static List<String> getInvocations() {
        LOGGER.infof("returning invocations %s", INVOCATIONS);
        return Collections.unmodifiableList(INVOCATIONS);
    }

    public String getId() {
        return id;
    }

    @Override
    public void close() throws WrongStateException, SystemException {
        INVOCATIONS.add("close");
        LOGGER.infof("close call on %s", this);
    }

    @Override
    public void cancel() throws FaultedException, WrongStateException, SystemException {
        INVOCATIONS.add("cancel");
        LOGGER.infof("cancel call on %s", this);
    }

    @Override
    public void compensate() throws FaultedException, WrongStateException, SystemException {
        INVOCATIONS.add("compensate");
        LOGGER.infof("compensate call on %s", this);
    }

    @Override
    public String status() throws SystemException {
        INVOCATIONS.add("status");
        LOGGER.infof("status call on %s", this);
        return Status.STATUS_ACTIVE;
    }

    @Override
    public void unknown() throws SystemException {
        INVOCATIONS.add("unknown");
        LOGGER.infof("unknown call on %s", this);
    }

    @Override
    public void error() throws SystemException {
        INVOCATIONS.add("error");
        LOGGER.infof("error call on %s", this);
    }

    @Override
    public String toString() {
        return String.format("%s{id='%s', INVOCATIONS=%s}", this.getClass().getSimpleName(), id, INVOCATIONS);
    }

}
