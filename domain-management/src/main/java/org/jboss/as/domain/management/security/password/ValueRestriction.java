/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link PasswordValidation} to verify that a password is not in a list of banned passwords.
 *
 * @author baranowb
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ValueRestriction implements PasswordRestriction {

    private final Set<String> forbiddenValues;

    private final String requirementsMessage;

    public ValueRestriction(final String[] forbiddenValues, final boolean must) {
        this.forbiddenValues = new HashSet<String>(Arrays.asList(forbiddenValues));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < forbiddenValues.length; i++) {
            sb.append(forbiddenValues[i]);
            if (i + 1 < forbiddenValues.length) {
                sb.append(", ");
            }
        }
        requirementsMessage = must ? MESSAGES.passwordMustNotEqualInfo(sb.toString()) : MESSAGES.passwordShouldNotEqualInfo(sb.toString());
    }

    @Override
    public String getRequirementMessage() {
        return requirementsMessage;
    }

    @Override
    public void validate(String userName, String password) throws PasswordValidationException {
        if (forbiddenValues.contains(password)) {
            throw MESSAGES.passwordMustNotBeEqual(password);
        }
    }

}
