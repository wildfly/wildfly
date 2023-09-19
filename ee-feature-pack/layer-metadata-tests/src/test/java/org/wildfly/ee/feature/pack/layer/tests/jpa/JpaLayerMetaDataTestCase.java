package org.wildfly.ee.feature.pack.layer.tests.jpa;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class JpaLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    /*
<prop name="org.wildfly.rule.expected-file" value="[/META-INF/persistence.xml,/WEB-INF/classes/META-INF/persistence.xml]"/>
<prop name="org.wildfly.rule.annotations" value="jakarta.persistence"/>
<prop name="org.wildfly.rule.class" value="jakarta.persistence"/>

     */
    @Test
    public void testMetaInfPersistenceXml() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addXml("persistence.xml", "")
                .build();
        checkLayersForArchive(p, "jpa");
    }

    @Test
    public void testWebInfClassesMetaInfPersistenceXml() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("persistence.xml", "", true)
                .build();
        checkLayersForArchive(p, "jpa");
    }


    @Test
    public void testAnnotationFromRootPackage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JpaAnnotationFromRootPackageUsage.class)
                .build();
        checkLayersForArchive(p, "jpa");
    }


    @Test
    public void testClassFromRootPackage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JpaClassFromRootPackageUsage.class)
                .build();
        checkLayersForArchive(p, "jpa");
    }


    @Test
    public void testClassFromCriteriaPackage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JpaClassFromCriteriaPackageUsage.class)
                .build();
        checkLayersForArchive(p, "jpa");
    }
}
