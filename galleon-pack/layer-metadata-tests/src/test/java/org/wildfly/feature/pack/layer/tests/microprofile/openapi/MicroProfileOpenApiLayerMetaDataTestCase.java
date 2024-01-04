/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.feature.pack.layer.tests.microprofile.openapi;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MicroProfileOpenApiLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    //////////////////////////////////////////////////////////////////////
    // Annotation tests
    // Note that the annotations in org.eclipse.microprofile.openapi.annotations.info
    // and org.eclipse.microprofile.openapi.annotations.links are not directly usable
    // (they are used as values for other annotations)

    @Test
    public void testOpenApiAnnotationFromRootPackage() throws Exception {
        testSingleClassInWar(OpenApiAnnotationFromRootPackageUsage.class);
    }

    @Test
    public void testOpenApiAnnotationFromCallbacksPackage() throws Exception {
        testSingleClassInWar(OpenApiAnnotationFromCallbacksPackageUsage.class);
    }

    @Test
    public void testOpenApiAnnotationFromExtensionsPackage() throws Exception {
        testSingleClassInWar(OpenApiAnnotationFromExtensionsPackageUsage.class);
    }

    @Test
    public void testOpenApiAnnotationFromMediaPackage() throws Exception {
        testSingleClassInWar(OpenApiAnnotationFromMediaPackageUsage.class);
    }

    @Test
    public void testOpenApiAnnotationFromParametersPackage() throws Exception {
        testSingleClassInWar(OpenApiAnnotationFromParametersPackageUsage.class);
    }

    @Test
    public void testOpenApiAnnotationFromResponsesPackage() throws Exception {
        testSingleClassInWar(OpenApiAnnotationFromResponsesPackageUsage.class);
    }

    @Test
    public void testOpenApiAnnotationFromSecurityPackage() throws Exception {
        testSingleClassInWar(OpenApiAnnotationFromSecurityPackageUsage.class);
    }


    private void testSingleClassInWar(Class<?> clazz) throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkOpenApi(p);
    }

    @Test
    public void testConfiguredOasModelReaderMetaInf() throws Exception {
        Path p = createArchive(ArchiveType.JAR, "microprofile-config.properties", "mp.openapi.model.reader=x\n");
        checkOpenApiAndConfig(p);
    }

    @Test
    public void testConfiguredOasModelReaderWebInfClassesMetaInf() throws Exception {
        Path p = createArchive(ArchiveType.WAR, "microprofile-config.properties", "mp.openapi.model.reader=x\n");
        checkOpenApiAndConfig(p);
    }

    @Test
    public void testConfiguredOasFilterMetaInf() throws Exception {
        Path p = createArchive(ArchiveType.JAR, "microprofile-config.properties", "mp.openapi.filter=x\n");
        checkOpenApiAndConfig(p);
    }

    @Test
    public void testConfiguredOasFilterWebInfClassesMetaInf() throws Exception {
        Path p = createArchive(ArchiveType.WAR, "microprofile-config.properties", "mp.openapi.filter=x\n");
        checkOpenApiAndConfig(p);
    }

    @Test
    public void testStaticContentYmlMetaInf() throws Exception {
        testArchiveWithFile(ArchiveType.JAR, "openapi.yml", null);
    }

    @Test
    public void testStaticContentYamlMetaInf() throws Exception {
        testArchiveWithFile(ArchiveType.JAR, "openapi.yaml", null);
    }

    @Test
    public void testStaticContentJsonMetaInf() throws Exception {
        testArchiveWithFile(ArchiveType.JAR, "openapi.json", null);
    }

    @Test
    public void testStaticContentYmlWebInfClassesMetaInf() throws Exception {
        testArchiveWithFile(ArchiveType.WAR, "openapi.yml", null);
    }

    @Test
    public void testStaticContentYamlWebInfClassesMetaInf() throws Exception {
        testArchiveWithFile(ArchiveType.WAR, "openapi.yaml", null);
    }

    @Test
    public void testStaticContentJsonWebInfClassesMetaInf() throws Exception {
        testArchiveWithFile(ArchiveType.WAR, "openapi.json", null);
    }

    private Path createArchive(ArchiveType archiveType, String filename, String contents) throws Exception {
        ArchiveBuilder builder = createArchiveBuilder(archiveType);
        if (contents == null) {
            contents = "";
        }
        if (archiveType == ArchiveType.WAR) {
            // Will go into WEB-INF/classes/META-INF
            builder.addFileToInf(filename, contents, true);
        } else if (archiveType == ArchiveType.JAR) {
            // Will go into /META-INF
            builder.addFileToInf(filename, contents);
        } else {
            throw new IllegalStateException("Unhandled archiveType " + archiveType);
        }
        Path p = builder.build();
        return p;
    }

    private void testArchiveWithFile(ArchiveType archiveType, String filename, String contents) throws Exception {
        Path p = createArchive(archiveType, filename, contents);
        checkOpenApi(p);
    }

    private void checkOpenApi(Path p) {
        checkLayersForArchive(p, new ExpectedLayers("microprofile-openapi", "microprofile-openapi"));
    }

    private void checkOpenApiAndConfig(Path p) {
        checkLayersForArchive(p,
                new ExpectedLayers("microprofile-openapi", "microprofile-openapi")
                        // microprofile-config doesn't show up as a decorator since it is a dependency of microprofile-openapi
                        .addLayer("microprofile-config"));
    }
}
