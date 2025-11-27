/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.lib;

import static org.jboss.as.test.smoke.jakarta.data.lib.Constants.ANDREA;
import static org.jboss.as.test.smoke.jakarta.data.lib.Constants.ANDREA_BDAY;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/** Used to validate use of a Repository during initialization of an ApplicationScoped bean packaged in an ear lib. */
@ApplicationScoped
@Transactional
public class RecruiterService {

    private Recruiter recruiter;

    // Allow creation of transactional proxies
    RecruiterService() {}

    @Inject
    public RecruiterService(Recruiter recruiter) {
        this.recruiter = recruiter;
    }

    public void init(@Observes Startup startup) {
        recruiter.recruit(ANDREA, ANDREA_BDAY);
    }

    public Person recruit(String name, LocalDate birthdate) {
        return recruiter.recruit(name, birthdate);
    }

    public Optional<Person> find(String name) {
        return recruiter.find(name);
    }
}
