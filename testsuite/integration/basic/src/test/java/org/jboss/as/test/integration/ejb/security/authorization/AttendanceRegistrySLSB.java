/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.security.authorization;

import org.jboss.ejb3.annotation.SecurityDomain;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Remote;
import javax.ejb.Stateless;
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
