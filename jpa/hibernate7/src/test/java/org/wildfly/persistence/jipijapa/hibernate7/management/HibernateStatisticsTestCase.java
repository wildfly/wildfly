/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.persistence.jipijapa.hibernate7.management;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Test class for verifying the operations of the Statistics classes.
 *
 * @author Ilias Bourdakos
 */
public class HibernateStatisticsTestCase {

    /** The available statistics classes */
    // in the future maybe each statistics class will require its own test class
    HibernateAbstractStatistics[] testStats = new HibernateAbstractStatistics[] {
        new HibernateStatistics(),
        new HibernateEntityStatistics(),
        new HibernateCollectionStatistics(),
        new HibernateEntityCacheStatistics(),
        new HibernateQueryCacheStatistics()
    };

    /**
     * Verifies that {@code hasChildName} correctly identifies known children for each
     * statistics class, rejects non-existing and null names, and produces results consistent
     * with {@code getChildrenNames().contains()}.
     */
    @Test
    public void testHibernateStatisticsHasChildName() {
        for (HibernateAbstractStatistics stats : testStats) {
            String statsClass = stats.getClass().getName();

            if (stats instanceof HibernateStatistics) {
                assertTrue(stats.hasChildName("entity"), "Should find 'entity' child (statistics class=" + statsClass + ")");
                assertTrue(stats.hasChildName("collection"), "Should find 'collection' child (statistics class=" + statsClass + ")");
                assertTrue(stats.hasChildName("entity-cache"), "Should find 'entity-cache' child (statistics class=" + statsClass + ")");
                assertTrue(stats.hasChildName("query-cache"), "Should find 'query-cache' child (statistics class=" + statsClass + ")");
            }

            assertFalse(stats.hasChildName("non-existing-child"), "Should not find non-existing child (statistics class=" + statsClass + ")");
            assertFalse(stats.hasChildName(null), "Should not find null child (statistics class=" + statsClass + ")");

            for (String childName : stats.getChildrenNames()) {
                assertEquals(stats.hasChildName(childName), stats.getChildrenNames().contains(childName),
                    "hasChildName should give same result as getChildrenNames().contains() for existing child (statistics class=" + statsClass + ")");
            }
            assertEquals(stats.hasChildName("non-existing"), stats.getChildrenNames().contains("non-existing"),
                "hasChildName should give same result as getChildrenNames().contains() for non-existing child (statistics class=" + statsClass + ")");
        }
    }

    /**
     * Verifies that {@code hasDynamicChildName} returns false for all statistics classes
     * when both the {@code EntityManagerFactoryAccess} and {@code PathAddress} are null.
     */
    @Test
    public void testHibernateStatisticsHasDynamicChildNameNullFactoryPath() {
        for (HibernateAbstractStatistics stats : testStats) {
            String statsClass = stats.getClass().getName();

            assertFalse(stats.hasDynamicChildName(null, null, "any-name"),
            "HibernateStatistics should not have dynamic children (statistics class=" + statsClass + ")");
        }
    }
}
