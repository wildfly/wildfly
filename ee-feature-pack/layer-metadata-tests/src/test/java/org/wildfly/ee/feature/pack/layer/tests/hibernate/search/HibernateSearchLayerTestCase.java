/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.hibernate.search;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

import static org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase.ArchiveType.JAR;

@Indexed
public class HibernateSearchLayerTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testHibernateSearchDetected() {
        Path p = createArchiveBuilder(JAR)
                .addClasses(this.getClass())
                .build();
        checkLayersForArchive(p, new ExpectedLayers("hibernate-search", "hibernate-search"));
    }
}
