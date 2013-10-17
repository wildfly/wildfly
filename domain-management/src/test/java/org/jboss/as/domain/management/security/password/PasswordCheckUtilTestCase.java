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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.junit.Test;

/**
 * @author <a href="mailto:g.grossetie@gmail.com">Guillaume Grossetie</a>
 */
public class PasswordCheckUtilTestCase {

    @Test
    public void testInitRestriction() throws URISyntaxException, IOException {
        final URL resource = PasswordCheckUtilTestCase.class.getResource("add-user.properties");

        File addUser = new File(resource.getFile());
        final List<PasswordRestriction> passwordRestrictions = PasswordCheckUtil.create(addUser).getPasswordRestrictions("ggrossetie");
        assertPasswordRejected(passwordRestrictions, "ggrossetie", "Password must not match username");
        assertPasswordRejected(passwordRestrictions, "abc12", "Password must have at least 8 characters");
        assertPasswordRejected(passwordRestrictions, "abcdefgh", "Password must have at least 2 digits");
        assertPasswordRejected(passwordRestrictions, "abcdefgh1", "Password must have at least 2 digits");
        assertPasswordRejected(passwordRestrictions, "root", "Password must not be 'root'");
        assertPasswordRejected(passwordRestrictions, "admin", "Password must not be 'admin'");
        assertPasswordRejected(passwordRestrictions, "administrator", "Password must not be 'administrator'");
        assertPasswordAccepted(passwordRestrictions, "abcdefgh12", "Password is valid");
    }

    private void assertPasswordRejected(List<PasswordRestriction> passwordRestrictions, String password, String expectedMessage) {
        boolean accepted = true;
        for(PasswordRestriction passwordRestriction : passwordRestrictions) {
            accepted &= passwordRestriction.pass(password);
        }
        assertFalse(expectedMessage, accepted);
    }

    private void assertPasswordAccepted(List<PasswordRestriction> passwordRestrictions, String password, String expectedMessage) {
        boolean accepted = true;
        for(PasswordRestriction passwordRestriction : passwordRestrictions) {
            accepted &= passwordRestriction.pass(password);
        }
        assertTrue(expectedMessage, accepted);
    }
}
