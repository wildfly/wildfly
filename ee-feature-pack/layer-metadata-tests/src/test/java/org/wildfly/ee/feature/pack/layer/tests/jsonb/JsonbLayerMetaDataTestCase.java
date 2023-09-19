package org.wildfly.ee.feature.pack.layer.tests.jsonb;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;
import java.util.Set;

public class JsonbLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testAnnotationUsage() throws Exception {
        testSingleClassWar(JsonbAnnotationUsage.class);
    }

    @Test
    public void testClassFromRootPackageUsage() throws Exception {
        testSingleClassWar(JsonbClassFromRootPackageUsage.class);
    }

    @Test
    public void testClassFromAdapterPackageUsage() throws Exception {
        testSingleClassWar(JsonbClassFromAdapterPackageUsage.class);
    }

    @Test
    public void testClassFromConfigPackageUsage() throws Exception {
        testSingleClassWar(JsonbClassFromConfigPackageUsage.class);
    }

    @Test
    public void testClassFromSerializerPackageUsage() throws Exception {
        testSingleClassWar(JsonbClassFromSerializerPackageUsage.class);
    }

    private void testSingleClassWar(Class<?> clazz) throws Exception {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchiveDontContainJsonp(p);
    }

    private void checkLayersForArchiveDontContainJsonp(Path p) throws Exception {
        // Some extra checks here, since jsonp has the jakarta.json package
        // while jsonb uses jakarta.json.bind. We want to make sure the rule
        // for jsonp is tight enough to not inadvertently recommending jsonb as well
        Set<String> set = checkLayersForArchive(p, "jsonb");
        Assert.assertFalse(set.contains("jsonp"));
    }
}
