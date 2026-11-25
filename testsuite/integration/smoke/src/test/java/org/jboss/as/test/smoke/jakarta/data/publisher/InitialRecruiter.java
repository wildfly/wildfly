/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.publisher;

import static org.jboss.as.test.smoke.jakarta.data.lib.Constants.BARRY;
import static org.jboss.as.test.smoke.jakarta.data.lib.Constants.BARRY_BDAY;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.as.test.smoke.jakarta.data.lib.Recruiter;

/** Used to validate use of a Repository during initialization of an ApplicationScoped bean packaged in a war. */
@ApplicationScoped
@Transactional
public class InitialRecruiter {

    @Inject
    private Recruiter recruiter;

    public void init(@Observes Startup event) {
        recruiter.recruit(BARRY, BARRY_BDAY);
    }
}
