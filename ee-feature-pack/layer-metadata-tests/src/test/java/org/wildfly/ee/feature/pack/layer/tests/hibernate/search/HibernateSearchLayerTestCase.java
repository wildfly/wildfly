package org.wildfly.ee.feature.pack.layer.tests.hibernate.search;

import org.junit.Test;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

import static org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase.ArchiveType.JAR;

@Indexed
public class HibernateSearchLayerTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testHibernateSearchDetected() throws Exception {
        Path p = createArchiveBuilder(JAR)
                .addClasses(this.getClass())
                .build();
        checkLayersForArchive(p, "hibernate-search");
    }
}
