/*
* JBoss, Home of Professional Open Source.
* Copyright 2013, Red Hat Middleware LLC, and individual contributors
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
