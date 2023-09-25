/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jsf.phaselistener.injectiontarget;

import jakarta.ejb.EJB;
import jakarta.faces.event.PhaseEvent;
import jakarta.faces.event.PhaseId;
import jakarta.faces.event.PhaseListener;

public class TestPhaseListener implements PhaseListener{

    @EJB
    TestEJB testEJB;

    @Override
    public void afterPhase(PhaseEvent phaseEvent) {
    }

    @Override
    public void beforePhase(PhaseEvent phaseEvent) {
        if (phaseEvent.getPhaseId().equals(PhaseId.RENDER_RESPONSE)) {
            phaseEvent.getFacesContext().getExternalContext().addResponseHeader("X-WildFly", testEJB.ping());

        }
    }

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.ANY_PHASE;
    }
}
