/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.authorization;

import org.jboss.ejb3.annotation.SecurityDomain;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import java.io.Serializable;
import java.util.Date;

/**
 * @author Jaikiran Pai
 */
@Stateless
@Remote(AttendanceRegistry.class)
@SecurityDomain("ejb3-tests")
public class AttendanceRegistrySLSB implements AttendanceRegistry<AttendanceRegistrySLSB.DefaultTimeProvider> {

    @PermitAll
    @Override
    public String recordEntry(final String user, final DefaultTimeProvider defaultTimeProvider) {
        return "(PermitAll) - User " + user + " logged in at " + defaultTimeProvider.getTime();
    }

    @RolesAllowed("Role2")
    @Override
    public String recordEntry(final String user, final long time) {
        return "User " + user + " logged in at " + time;
    }


    public static final class DefaultTimeProvider implements TimeProvider, Serializable {
        private final Date date;

        public DefaultTimeProvider(final Date date) {
            this.date = date;
        }

        @Override
        public long getTime() {
            return this.date.getTime();
        }
    }


}
