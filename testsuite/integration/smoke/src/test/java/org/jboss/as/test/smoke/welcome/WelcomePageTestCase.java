/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.welcome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.List;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Test for the WildFly welcome page.
 * <p>
 * Verifies the title of the page and links to the documentation, quickstarts and the admin console.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WelcomePageTestCase {

    public static final String TITLE = "Welcome to WildFly";
    public static final String HEADER_TEXT = "Welcome to WildFly";
    public static final String DOCUMENTATION_LINK_TEXT = "Documentation";
    public static final String QUICKSTARTS_LINK_TEXT = "Quickstarts";
    public static final String ADMINISTRATION_CONSOLE_LINK_TEXT = "Administration Console";

    @Drone
    WebDriver driver;

    @Test
    public void testWelcomePage() throws Exception {
        URL url = TestSuiteEnvironment.getHttpUrl();
        driver.get(url.toExternalForm());
        assertEquals(TITLE, driver.getTitle());

        WebElement header = driver.findElement(By.tagName("h1"));
        assertEquals(HEADER_TEXT, header.getText());

        List<WebElement> links = driver.findElements(By.tagName("a"));
        assertLink(links, DOCUMENTATION_LINK_TEXT);
        assertLink(links, QUICKSTARTS_LINK_TEXT);
        assertLink(links, ADMINISTRATION_CONSOLE_LINK_TEXT);
    }

    private void assertLink(List<WebElement> links, String text) {
        for (WebElement link : links) {
            if (text.equals(link.getText())) {
                return;
            }
        }
        fail("Link " + text + " not found!");
    }
}
