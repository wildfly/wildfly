/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.extension.rts.common;

import java.util.ArrayList;
import java.util.List;

import org.jboss.narayana.rest.integration.api.Participant;
import org.jboss.narayana.rest.integration.api.Vote;

/**
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
public final class LoggingParticipant implements Participant {

    private static final long serialVersionUID = 7584938841973602732L;

    private final List<String> invocations;

    private final Vote outcome;

    public LoggingParticipant(final Vote outcome) {
        this.outcome = outcome;
        this.invocations = new ArrayList<String>();
    }

    @Override
    public Vote prepare() {
        invocations.add("prepare");
        return outcome;
    }

    @Override
    public void commit() {
        invocations.add("commit");
    }

    @Override
    public void commitOnePhase() {
        invocations.add("commitOnePhase");
    }

    @Override
    public void rollback() {
        invocations.add("rollback");
    }

    public List<String> getInvocations() {
        return invocations;
    }

}
