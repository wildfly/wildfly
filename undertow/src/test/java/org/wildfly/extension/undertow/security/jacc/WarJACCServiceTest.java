/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.security.jacc;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WarJACCServiceTest {

    @Test
    public void testPatternExtensionMatching() {
        WarJACCService.PatternInfo htm  = new WarJACCService.PatternInfo("*.htm", 2);
        WarJACCService.PatternInfo html = new WarJACCService.PatternInfo("*.html", 2);

        WarJACCService.PatternInfo indexHtm  = new WarJACCService.PatternInfo("/index.htm", 4);
        WarJACCService.PatternInfo indexHtml = new WarJACCService.PatternInfo("/index.html", 4);

        WarJACCService.PatternInfo indexHtmHtml  = new WarJACCService.PatternInfo("/index.htm.html", 4);
        WarJACCService.PatternInfo indexHtmlHtm  = new WarJACCService.PatternInfo("/index.html.htm", 4);

        assertTrue(htm.isExtensionFor(indexHtm));
        assertTrue(html.isExtensionFor(indexHtml));

        // extension has to match completely, not partially
        assertFalse(html.isExtensionFor(indexHtm));
        assertFalse(htm.isExtensionFor(indexHtml));

        assertTrue(htm.isExtensionFor(indexHtmlHtm));
        assertTrue(html.isExtensionFor(indexHtmHtml));

        // extension has to match the last segment
        assertFalse(html.isExtensionFor(indexHtmlHtm));
        assertFalse(htm.isExtensionFor(indexHtmHtml));
    }
}
