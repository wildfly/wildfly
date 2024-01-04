/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jsf.phaselistener.injectiontarget.bean;

import jakarta.faces.event.PhaseEvent;
import jakarta.faces.event.PhaseId;
import jakarta.faces.event.PhaseListener;
import jakarta.inject.Inject;

public class TestPhaseListener implements PhaseListener{

    @Inject
    SimpleBean simpleBean;

    @Override
    public void afterPhase(PhaseEvent phaseEvent) {
    }

    @Override
    public void beforePhase(PhaseEvent phaseEvent) {
        if (phaseEvent.getPhaseId().equals(PhaseId.RENDER_RESPONSE)) {
            phaseEvent.getFacesContext().getExternalContext().addResponseHeader("X-WildFly", simpleBean.ping());
        }
    }

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.ANY_PHASE;
    }
}
