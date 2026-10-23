/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.io.Serial;

import org.jboss.as.version.Stability;

/**
 * {@link EJB3AdditionalInitialization} for the current model, running at {@link Stability#COMMUNITY}, which is the
 * stability level the EJB access-log feature requires, i.e. it is not available at {@link Stability#DEFAULT}.
 * <p>
 * This is deliberately a separate class: {@link EJB3AdditionalInitialization} is loaded into legacy (EAP) controllers
 * whose class path may not contain {@link Stability}, so it must not reference it.
 */
public class EJB3CommunityAdditionalInitialization extends EJB3AdditionalInitialization {

    @Serial
    private static final long serialVersionUID = 5392467148290613774L;

    @Override
    public Stability getStability() {
        return Stability.COMMUNITY;
    }
}
