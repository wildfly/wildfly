/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.management.security.password;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.domain.management.logging.DomainManagementLogger;

/**
 * A {@link PasswordValidation} which wraps multiple other restrictions.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class CompoundRestriction implements PasswordRestriction {

    private final List<PasswordRestriction> wrapped = new ArrayList<PasswordRestriction>();
    private final boolean must;

    public CompoundRestriction(final boolean must) {
        this.must = must;
    }

    synchronized void add(final PasswordRestriction restriction) {
        wrapped.add(restriction);
    }

    public List<PasswordRestriction> getRestrictions() {
        return Collections.unmodifiableList(wrapped);
    }

    @Override
    public synchronized String getRequirementMessage() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wrapped.size(); i++) {
            sb.append(wrapped.get(i).getRequirementMessage());
            if (i + 1 < wrapped.size()) {
                sb.append(", ");
            }
        }

        return must ? DomainManagementLogger.ROOT_LOGGER.passwordMustContainInfo(sb.toString()) : DomainManagementLogger.ROOT_LOGGER.passwordShouldContainInfo(sb.toString());
    }

    @Override
    public void validate(String userName, String password) throws PasswordValidationException {
        for (PasswordRestriction current : wrapped) {
            current.validate(userName, password);
        }

    }

}
