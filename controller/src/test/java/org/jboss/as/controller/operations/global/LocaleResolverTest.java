/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.operations.global;

import java.util.Locale;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author rpelisse
 */
public class LocaleResolverTest {

    Locale french = Locale.FRENCH;
    Locale english = Locale.ENGLISH;
    Locale german = Locale.GERMAN;

    @Test
    public void invalidLanguageNotUsingAlphanumericCharacters() {
        invalidLanguageOrCountry("1#");
    }

    @Test
    public void invalidCountryNotUsingAlphanumericCharacters() {
        invalidLanguageOrCountry("en_1€");
    }

    @Test
    public void invalidLanguageTag() {
        validLanguageOrCountry("en_EN_long_but_OK");
        invalidLanguageOrCountry("e"); // too short
        invalidLanguageOrCountry("e_U");
        invalidLanguageOrCountry("en_U");
        invalidLanguageOrCountry("en_US_"); // incomplete
    }

    @Test
    public void nonExistingLanguageOrCountry() {
        invalidLanguageOrCountry("aW");
        invalidLanguageOrCountry("aW_AW");
    }

    @Test
    public void invalidLocaleSeparator() {
        invalidLanguageOrCountry("*");
        invalidLanguageOrCountry("de_DE8");
        invalidLanguageOrCountry("en_#");
    }

    public void invalidLanguageOrCountry(String unparsed) {
        try {
            LocaleResolver.resolveLocale(unparsed);
        } catch ( IllegalArgumentException e) {
            assertEquals(unparsed,e.getMessage());
            return; // pass...
        }
        fail("Format " + unparsed + " is invalid, test should have failed.");
    }

    public void validLanguageOrCountry(String unparsed) {
        try {
            LocaleResolver.resolveLocale(unparsed);
        } catch ( IllegalArgumentException e) {
            assertEquals(unparsed,e.getMessage());
            fail("Format " + unparsed + " is invalid, test should have failed.");
        }
    }

    @Test
    public void wfly3723() {
        assertEquals("",french, LocaleResolver.resolveLocale(french.toLanguageTag()));
        assertEquals("",german, LocaleResolver.resolveLocale(german.toLanguageTag()));
        assertEquals("",Locale.ROOT, LocaleResolver.resolveLocale(english.toLanguageTag()));
    }
}
