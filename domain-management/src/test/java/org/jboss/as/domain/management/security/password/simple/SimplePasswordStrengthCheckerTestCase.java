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

package org.jboss.as.domain.management.security.password.simple;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.domain.management.security.password.Dictionary;
import org.jboss.as.domain.management.security.password.Keyboard;
import org.jboss.as.domain.management.security.password.LengthRestriction;
import org.jboss.as.domain.management.security.password.PasswordCheckUtil;
import org.jboss.as.domain.management.security.password.PasswordRestriction;
import org.jboss.as.domain.management.security.password.PasswordStrengthCheckResult;
import org.jboss.as.domain.management.security.password.ValueRestriction;

/**
 * @author baranowb
 */
public class SimplePasswordStrengthCheckerTestCase {

    private Keyboard keyboard = new SimpleKeyboard();
    private Dictionary dictionary = new SimpleDictionary();

    public static final PasswordCheckUtil PCU = PasswordCheckUtil.create(null);
    public static final PasswordRestriction ALPHA_RESTRICTION = PCU.createAlphaRestriction(1);
    public static final PasswordRestriction SYMBOL_RESTRICTION = PCU.createSymbolRestriction(1);
    public static final PasswordRestriction DIGIT_RESTRICTION = PCU.createDigitRestriction(1);
    public static final LengthRestriction LENGTH_RESTRICTION = new LengthRestriction(8);

    @Test
    public void testLengthRestriction() {
        List<PasswordRestriction> restrictions = new ArrayList<PasswordRestriction>();
        //one that will fail
        restrictions.add(LENGTH_RESTRICTION);
        //one that will pass
        restrictions.add(SYMBOL_RESTRICTION);
        SimplePasswordStrengthChecker checker = new SimplePasswordStrengthChecker(restrictions, this.dictionary, this.keyboard);
        String pwd = "1W2sa#4";
        PasswordStrengthCheckResult result = checker.check("", pwd, null);
        assertNotNull(result);
        assertNotNull(result.getPassedRestrictions());
        assertNotNull(result.getRestrictionFailures());

        assertEquals(1, result.getPassedRestrictions().size());
        assertEquals(1, result.getRestrictionFailures().size());

        assertNotNull(result.getStrength());

        assertEquals(MESSAGES.passwordNotLongEnough(8).getMessage(), result.getRestrictionFailures().get(0).getMessage());
        assertEquals(SYMBOL_RESTRICTION, result.getPassedRestrictions().get(0));
    }

    @Test
    public void testDigitsRestriction() {
        List<PasswordRestriction> restrictions = new ArrayList<PasswordRestriction>();
        //one that will fail
        restrictions.add(DIGIT_RESTRICTION);
        //one that will pass
        restrictions.add(ALPHA_RESTRICTION);
        SimplePasswordStrengthChecker checker = new SimplePasswordStrengthChecker(restrictions, this.dictionary, this.keyboard);
        String pwd = "DW$sa#x";
        PasswordStrengthCheckResult result = checker.check("", pwd, null);
        assertNotNull(result);
        assertNotNull(result.getPassedRestrictions());
        assertNotNull(result.getRestrictionFailures());

        assertEquals(1, result.getPassedRestrictions().size());
        assertEquals(1, result.getRestrictionFailures().size());

        assertNotNull(result.getStrength());

        assertEquals(MESSAGES.passwordMustHaveDigit(1), result.getRestrictionFailures().get(0).getMessage());
        assertEquals(ALPHA_RESTRICTION, result.getPassedRestrictions().get(0));
    }

    @Test
    public void testSymbolsRestriction() {
        List<PasswordRestriction> restrictions = new ArrayList<PasswordRestriction>();
        //one that will fail
        restrictions.add(SYMBOL_RESTRICTION);
        //one that will pass
        restrictions.add(ALPHA_RESTRICTION);
        SimplePasswordStrengthChecker checker = new SimplePasswordStrengthChecker(restrictions, this.dictionary, this.keyboard);
        String pwd = "DW5sa3x";
        PasswordStrengthCheckResult result = checker.check("", pwd, null);
        assertNotNull(result);
        assertNotNull(result.getPassedRestrictions());
        assertNotNull(result.getRestrictionFailures());

        assertEquals(1, result.getPassedRestrictions().size());
        assertEquals(1, result.getRestrictionFailures().size());

        assertNotNull(result.getStrength());

        assertEquals(MESSAGES.passwordMustHaveSymbol(1), result.getRestrictionFailures().get(0).getMessage());
        assertEquals(ALPHA_RESTRICTION, result.getPassedRestrictions().get(0));
    }

    @Test
    public void testAlphaRestriction() {
        List<PasswordRestriction> restrictions = new ArrayList<PasswordRestriction>();
        //one that will fail
        restrictions.add(ALPHA_RESTRICTION);
        //one that will pass
        restrictions.add(SYMBOL_RESTRICTION);
        SimplePasswordStrengthChecker checker = new SimplePasswordStrengthChecker(restrictions, this.dictionary, this.keyboard);
        String pwd = "!#*_33";
        PasswordStrengthCheckResult result = checker.check("", pwd, null);
        assertNotNull(result);
        assertNotNull(result.getPassedRestrictions());
        assertNotNull(result.getRestrictionFailures());

        assertEquals(1, result.getPassedRestrictions().size());
        assertEquals(1, result.getRestrictionFailures().size());

        assertNotNull(result.getStrength());

        assertEquals(MESSAGES.passwordMustHaveAlpha(1), result.getRestrictionFailures().get(0).getMessage());
        assertEquals(SYMBOL_RESTRICTION, result.getPassedRestrictions().get(0));
    }

    @Test
    public void testAdHocRestriction() {
        List<PasswordRestriction> restrictions = new ArrayList<PasswordRestriction>();
        restrictions.add(ALPHA_RESTRICTION);
        restrictions.add(SYMBOL_RESTRICTION);
        SimplePasswordStrengthChecker checker = new SimplePasswordStrengthChecker(restrictions, this.dictionary, this.keyboard);
        String pwd = "!#*_3x";
        List<PasswordRestriction> adHocRestrictions = new ArrayList<PasswordRestriction>();
        ValueRestriction restriction = new ValueRestriction(new String[] { pwd }, true);
        adHocRestrictions.add(restriction);

        PasswordStrengthCheckResult result = checker.check("", pwd, adHocRestrictions);
        assertNotNull(result);
        assertNotNull(result.getPassedRestrictions());
        assertNotNull(result.getRestrictionFailures());

        assertEquals(2, result.getPassedRestrictions().size());
        assertEquals(1, result.getRestrictionFailures().size());

        assertNotNull(result.getStrength());

        assertEquals(ALPHA_RESTRICTION, result.getPassedRestrictions().get(0));
        assertEquals(SYMBOL_RESTRICTION, result.getPassedRestrictions().get(1));
        assertEquals(MESSAGES.passwordMustNotBeEqual(pwd).getMessage(), result.getRestrictionFailures().get(0).getMessage());
    }
}
