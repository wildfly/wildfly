/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
