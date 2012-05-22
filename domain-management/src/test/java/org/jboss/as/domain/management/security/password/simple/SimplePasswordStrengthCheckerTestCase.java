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

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.domain.management.security.password.Dictionary;
import org.jboss.as.domain.management.security.password.Keyboard;
import org.jboss.as.domain.management.security.password.PasswordRestriction;
import org.jboss.as.domain.management.security.password.PasswordStrengthCheckResult;
import org.jboss.as.domain.management.security.password.ValueRestriction;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 * @author baranowb
 *
 */
public class SimplePasswordStrengthCheckerTestCase {

    private Keyboard keyboard = new SimpleKeyboard();
    private Dictionary dictionary = new SimpleDictionary();
    
    @Test
    public void testLengthRestriction(){
        List<PasswordRestriction> restrictions = new ArrayList<PasswordRestriction>();
        //one that will fail
        restrictions.add(SimplePasswordStrengthChecker.RESTRICTION_LENGTH);
        //one that will pass
        restrictions.add(SimplePasswordStrengthChecker.RESTRICTION_SYMBOLS);
        SimplePasswordStrengthChecker checker = new SimplePasswordStrengthChecker(restrictions,this.dictionary,this.keyboard);
        String pwd = "1W2sa#4";
        PasswordStrengthCheckResult result = checker.check(pwd, null);
        assertNotNull(result);
        assertNotNull(result.getPassedRestrictions());
        assertNotNull(result.getFailedRestrictions());
        
        assertEquals(1, result.getPassedRestrictions().size());
        assertEquals(1, result.getFailedRestrictions().size());
        
        assertNotNull(result.getStrength());
        
        assertEquals(SimplePasswordStrengthChecker.RESTRICTION_LENGTH, result.getFailedRestrictions().get(0));
        assertEquals(SimplePasswordStrengthChecker.RESTRICTION_SYMBOLS, result.getPassedRestrictions().get(0));
    }
    
    @Test
    public void testDigitsRestriction(){
        List<PasswordRestriction> restrictions = new ArrayList<PasswordRestriction>();
        //one that will fail
        restrictions.add(SimplePasswordStrengthChecker.RESTRICTION_DIGITS);
        //one that will pass
        restrictions.add(SimplePasswordStrengthChecker.RESTRICTION_ALPHA);
        SimplePasswordStrengthChecker checker = new SimplePasswordStrengthChecker(restrictions,this.dictionary,this.keyboard);
        String pwd = "DW$sa#x";
        PasswordStrengthCheckResult result = checker.check(pwd, null);
        assertNotNull(result);
        assertNotNull(result.getPassedRestrictions());
        assertNotNull(result.getFailedRestrictions());
        
        assertEquals(1, result.getPassedRestrictions().size());
        assertEquals(1, result.getFailedRestrictions().size());
        
        assertNotNull(result.getStrength());
        
        assertEquals(SimplePasswordStrengthChecker.RESTRICTION_DIGITS, result.getFailedRestrictions().get(0));
        assertEquals(SimplePasswordStrengthChecker.RESTRICTION_ALPHA, result.getPassedRestrictions().get(0));
    }
    
    @Test
    public void testSymbolsRestriction(){
        List<PasswordRestriction> restrictions = new ArrayList<PasswordRestriction>();
        //one that will fail
        restrictions.add(SimplePasswordStrengthChecker.RESTRICTION_SYMBOLS);
        //one that will pass
        restrictions.add(SimplePasswordStrengthChecker.RESTRICTION_ALPHA);
        SimplePasswordStrengthChecker checker = new SimplePasswordStrengthChecker(restrictions,this.dictionary,this.keyboard);
        String pwd = "DW5sa3x";
        PasswordStrengthCheckResult result = checker.check(pwd, null);
        assertNotNull(result);
        assertNotNull(result.getPassedRestrictions());
        assertNotNull(result.getFailedRestrictions());
        
        assertEquals(1, result.getPassedRestrictions().size());
        assertEquals(1, result.getFailedRestrictions().size());
        
        assertNotNull(result.getStrength());
        
        assertEquals(SimplePasswordStrengthChecker.RESTRICTION_SYMBOLS, result.getFailedRestrictions().get(0));
        assertEquals(SimplePasswordStrengthChecker.RESTRICTION_ALPHA, result.getPassedRestrictions().get(0));
    }
    
    @Test
    public void testAlphaRestriction(){
        List<PasswordRestriction> restrictions = new ArrayList<PasswordRestriction>();
        //one that will fail
        restrictions.add(SimplePasswordStrengthChecker.RESTRICTION_ALPHA);
        //one that will pass
        restrictions.add(SimplePasswordStrengthChecker.RESTRICTION_SYMBOLS);
        SimplePasswordStrengthChecker checker = new SimplePasswordStrengthChecker(restrictions,this.dictionary,this.keyboard);
        String pwd = "!#*_33";
        PasswordStrengthCheckResult result = checker.check(pwd, null);
        assertNotNull(result);
        assertNotNull(result.getPassedRestrictions());
        assertNotNull(result.getFailedRestrictions());
        
        assertEquals(1, result.getPassedRestrictions().size());
        assertEquals(1, result.getFailedRestrictions().size());
        
        assertNotNull(result.getStrength());
        
        assertEquals(SimplePasswordStrengthChecker.RESTRICTION_ALPHA, result.getFailedRestrictions().get(0));
        assertEquals(SimplePasswordStrengthChecker.RESTRICTION_SYMBOLS, result.getPassedRestrictions().get(0));
    }
    
    @Test
    public void testAdHocRestriction(){
        List<PasswordRestriction> restrictions = new ArrayList<PasswordRestriction>();
        restrictions.add(SimplePasswordStrengthChecker.RESTRICTION_ALPHA);
        restrictions.add(SimplePasswordStrengthChecker.RESTRICTION_SYMBOLS);
        SimplePasswordStrengthChecker checker = new SimplePasswordStrengthChecker(restrictions,this.dictionary,this.keyboard);
        String pwd = "!#*_3x";
        List<PasswordRestriction> adHocRestrictions = new ArrayList<PasswordRestriction>();
        ValueRestriction restriction = new ValueRestriction(pwd);
        adHocRestrictions.add(restriction);
        
        PasswordStrengthCheckResult result = checker.check(pwd, adHocRestrictions);
        assertNotNull(result);
        assertNotNull(result.getPassedRestrictions());
        assertNotNull(result.getFailedRestrictions());
        
        assertEquals(2, result.getPassedRestrictions().size());
        assertEquals(1, result.getFailedRestrictions().size());
        
        assertNotNull(result.getStrength());
        
        assertEquals(SimplePasswordStrengthChecker.RESTRICTION_ALPHA, result.getPassedRestrictions().get(0));
        assertEquals(SimplePasswordStrengthChecker.RESTRICTION_SYMBOLS, result.getPassedRestrictions().get(1));
        assertEquals(restriction, result.getFailedRestrictions().get(0));
    }
}
