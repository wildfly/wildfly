/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.controller.operations.global;

import java.util.Locale;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
        invalidLanguageOrCountry("en_EN_too_long");
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
            GlobalOperationHandlers.resolveLocale(unparsed);
        } catch ( IllegalArgumentException e) {
            assertEquals(unparsed,e.getMessage());
            return; // pass...
        }
        fail("Format " + unparsed + " is invalid, test should have failed.");
    }
    
    @Test
    public void wfly3723() {
        assertEquals("",french, GlobalOperationHandlers.resolveLocale(french.toLanguageTag())); 
        assertEquals("",german, GlobalOperationHandlers.resolveLocale(german.toLanguageTag())); 
        assertEquals("",Locale.ROOT, GlobalOperationHandlers.resolveLocale(english.toLanguageTag()));         
    }
}
