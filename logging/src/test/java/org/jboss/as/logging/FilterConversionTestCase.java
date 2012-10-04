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

package org.jboss.as.logging;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.Level;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class FilterConversionTestCase {
    static final Map<String, ModelNode> MAP = new HashMap<String, ModelNode>();

    static {
        final ModelNode anyFilter = new ModelNode().setEmptyObject();
        anyFilter.get(CommonAttributes.ANY.getName()).get(CommonAttributes.MATCH.getName()).set(".*");
        anyFilter.protect();
        MAP.put("any(match(\".*\"))", anyFilter);

        final ModelNode changeLevelFilter = new ModelNode().setEmptyObject();
        changeLevelFilter.get(CommonAttributes.CHANGE_LEVEL.getName()).set(Level.ALL.getName());
        changeLevelFilter.protect();
        MAP.put("levelChange(ALL)", changeLevelFilter);

        final ModelNode allNotFilter = new ModelNode().setEmptyObject();
        allNotFilter.get(CommonAttributes.ALL.getName()).get(CommonAttributes.NOT.getName()).get(CommonAttributes.MATCH.getName()).set("JBAS11\\d*");
        allNotFilter.protect();
        MAP.put("all(not(match(\"JBAS11\\\\d*\")))", allNotFilter);

        final ModelNode replaceFilter = new ModelNode().setEmptyObject();
        replaceFilter.get(CommonAttributes.REPLACE.getName(), CommonAttributes.REPLACE_ALL.getName()).set(true);
        replaceFilter.get(CommonAttributes.REPLACE.getName(), CommonAttributes.PATTERN.getName()).set("JBAS");
        replaceFilter.get(CommonAttributes.REPLACE.getName(), CommonAttributes.REPLACEMENT.getName()).set("EAP");
        replaceFilter.protect();
        MAP.put("substituteAll(\"JBAS\",\"EAP\")", replaceFilter);

        final ModelNode levelRangeFilter = new ModelNode().setEmptyObject();
        levelRangeFilter.get(CommonAttributes.LEVEL_RANGE_LEGACY.getName(), CommonAttributes.MAX_INCLUSIVE.getName()).set(true);
        levelRangeFilter.get(CommonAttributes.LEVEL_RANGE_LEGACY.getName(), CommonAttributes.MIN_LEVEL.getName()).set(Level.INFO.getName());
        levelRangeFilter.get(CommonAttributes.LEVEL_RANGE_LEGACY.getName(), CommonAttributes.MAX_LEVEL.getName()).set(Level.FATAL.getName());
        levelRangeFilter.protect();
        MAP.put("levelRange(INFO,FATAL]", levelRangeFilter);
    }

    @Test
    public void testFilterToFilterSpec() throws Exception {
        for (Map.Entry<String, ModelNode> entry : MAP.entrySet()) {
            final String expectedValue = entry.getKey();
            final ModelNode filter = entry.getValue();
            final String filterSpec = Filters.filterToFilterSpec(filter);
            assertEquals(String.format("Filter conversion invalid: %nFilter=%s%nValue=%s", filter.asString(), filterSpec), expectedValue, filterSpec);
        }
    }

    @Test
    public void testFilterSpecToFilter() throws Exception {
        for (Map.Entry<String, ModelNode> entry : MAP.entrySet()) {
            final ModelNode expectedValue = entry.getValue();
            final String filterSpec = entry.getKey();
            final ModelNode filter = Filters.filterSpecToFilter(filterSpec);

            ModelTestUtils.compare(expectedValue, filter);
        }
    }
}
