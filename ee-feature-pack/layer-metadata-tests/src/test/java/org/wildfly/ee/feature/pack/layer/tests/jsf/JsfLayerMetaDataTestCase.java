package org.wildfly.ee.feature.pack.layer.tests.jsf;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class JsfLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    /*
<prop name="org.wildfly.rule.xml-path" value="/WEB-INF/web.xml,/web-app/servlet/servlet-class,jakarta.faces.webapp.FacesServlet"/>
<prop name="org.wildfly.rule.expected-file" value="/WEB-INF/faces-config.xml"/>
<prop name="org.wildfly.rule.annotations" value="jakarta.faces.annotation"/>
<prop name="org.wildfly.rule.class" value="jakarta.faces.*"/>
     */

    @Test
    public void testFacesServletInWebXml() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml(
                        "web.xml",
                        createXmlElementWithContent("jakarta.faces.webapp.FacesServlet", "web-app", "servlet", "servlet-class"))
                .build();
        checkLayersForArchive(p, "jsf", "servlet");
    }

    @Test
    public void testFacesConfigXml() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("faces-config.xml", "")
                .build();
        checkLayersForArchive(p, "jsf");
    }

    @Test
    public void testAnnotation() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JsfAnnotationUsage.class)
                .build();
        checkLayersForArchive(p, "jsf");
    }

    @Test
    public void testClassFromRootPackage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JsfClassFromRootPackageUsage.class)
                .build();
        checkLayersForArchive(p, "jsf");
    }

    @Test
    public void testClassFromNestedPackage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(JsfClassFromNestedPackageUsage.class)
                .build();
        checkLayersForArchive(p, "jsf");

    }
}
