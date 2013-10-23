/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.junit.Test;

/**
 *
 * @author baranowb
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PasswordRestrictionsTestCase {

    @Test(expected = PasswordValidationException.class)
    public void testLengthRestrictionFail() throws PasswordValidationException {
        LengthRestriction lr = new LengthRestriction(2);
        lr.validate("", "1");
    }

    @Test
    public void testLengthRestrictionPass() throws PasswordValidationException {
        LengthRestriction lr = new LengthRestriction(2);
        lr.validate("", "12");
    }

    @Test(expected = PasswordValidationException.class)
    public void testValueRestrictionFail() throws PasswordValidationException {
        ValueRestriction lr = new ValueRestriction(new String[] { "restricted" });
        lr.validate("", "restricted");
    }

    @Test
    public void testValueRestrictionPass() throws PasswordValidationException {
        ValueRestriction lr = new ValueRestriction(new String[] { "restricted" });
        lr.validate("", "12");
    }

    @Test(expected = PasswordValidationException.class)
    public void testRegexRestrictionFail() throws PasswordValidationException {
        RegexRestriction lr = new RegexRestriction("\\d*", "", "");
        lr.validate("", "xxxAAA");
    }

    @Test
    public void testRegexRestrictionPass() throws PasswordValidationException {
        RegexRestriction lr = new RegexRestriction("x*ax+", "", "");
        lr.validate("", "xxax");
    }

    @Test(expected = PasswordValidationException.class)
    public void testUsernameMatchFail() throws PasswordValidationException {
        UsernamePasswordMatch upm = new UsernamePasswordMatch();
        upm.validate("darranl", "darranl");
    }

    @Test
    public void testUsernameMatchPass() throws PasswordValidationException {
        UsernamePasswordMatch upm = new UsernamePasswordMatch();
        upm.validate("darranl", "password");
    }

}
