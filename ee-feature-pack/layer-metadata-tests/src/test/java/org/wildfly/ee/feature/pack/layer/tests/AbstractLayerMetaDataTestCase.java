package org.wildfly.ee.feature.pack.layer.tests;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.wildfly.glow.Arguments;
import org.wildfly.glow.GlowMessageWriter;
import org.wildfly.glow.GlowSession;
import org.wildfly.glow.ScanResults;
import org.wildfly.glow.maven.MavenResolver;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class AbstractLayerMetaDataTestCase {

    private static String URL_PROPERTY = "wildfly-glow-galleon-feature-packs-url";
    protected static Path ARCHIVES_PATH = Paths.get("target/glow-archives");

    private boolean checkMethodCalled;

    @BeforeClass
    public static void prepareArchivesDirectory() throws Exception {
        Path glowXmlPath = Path.of("target/test-classes/glow");
        System.setProperty(URL_PROPERTY, glowXmlPath.toUri().toString());
        if (Files.exists(ARCHIVES_PATH)) {
            Files.walkFileTree(ARCHIVES_PATH, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        Files.createDirectories(ARCHIVES_PATH);
    }

    @Before
    public void before() {
        checkMethodCalled = false;
    }

    @After
    public void after() {
        Assert.assertTrue(checkMethodCalled);
    }

    protected static String createXmlElementWithContent(String content, String... path) {
        StringBuilder sb = new StringBuilder();
        Stack<String> stack = new Stack<>();
        for (String element : path) {
            sb.append("<" + element + ">");
            stack.push(element);
        }
        if (content != null) {
            sb.append(content);
        }
        while (!stack.empty()) {
            sb.append("</" + stack.pop() + ">");
        }
        return sb.toString();
    }

    protected Set<String> checkLayersForArchive(Path archivePath, String...expectedLayers) throws Exception {
        checkMethodCalled = true;
        Arguments arguments = Arguments.scanBuilder().setBinaries(Collections.singletonList(archivePath)).build();
        ScanResults scanResults = GlowSession.scan(MavenResolver.newMavenResolver(), arguments, GlowMessageWriter.DEFAULT);
        Set<String> foundLayers = scanResults.getDiscoveredLayers().stream().map(l -> l.getName()).collect(Collectors.toSet());

        Assert.assertEquals(expectedLayers.length, foundLayers.size());

        for (String expectedLayer : expectedLayers) {
            Assert.assertTrue(expectedLayer, foundLayers.contains(expectedLayer));
        }

        return foundLayers;
    }

    protected ArchiveBuilder createArchiveBuilder(ArchiveType type) {
        return new ArchiveBuilder("test-" + System.currentTimeMillis() + "." + type.suffix, type);
    }

    protected ArchiveBuilder createArchiveBuilder(String name, ArchiveType type) {
        return new ArchiveBuilder(name, type);
    }

    public class ArchiveBuilder {
        private final String name;
        private final ArchiveType type;
        private Map<String, String> xmlContents = new HashMap<>();
        private Set<String> webInfClassesMetaInfXmls = new HashSet<>();
        private List<Class<?>> classes = new ArrayList<>();

        private ArchiveBuilder(String name, ArchiveType type) {
            this.name = name;
            this.type = type;
        }

        public ArchiveBuilder addXml(String name, String contents) {
            return addXml(name, contents, false);
        }

        public ArchiveBuilder addXml(String name, String contents, boolean webInfClassesMetaInf) {
            if (webInfClassesMetaInf) {
                if (type != ArchiveType.WAR) {
                    throw new IllegalStateException("Can only specify webInfClassesMetaInf for wars");
                }
                webInfClassesMetaInfXmls.add(name);
            }
            xmlContents.put(name, contents);
            return this;
        }

        public ArchiveBuilder addClasses(Class<?>... classes) {
            this.classes.addAll(Arrays.asList(classes));
            return this;
        }

        public Path build() {
            if (type == ArchiveType.WAR) {
                WebArchive war = ShrinkWrap.create(WebArchive.class, name);
                addXmlContents((name, xml) -> {
                    if (webInfClassesMetaInfXmls.contains(name)) {
                        name = "classes/META-INF/" + name;
                    }
                    war.addAsWebInfResource(xml, name);
                });
                addClasses(war);
                return export(war);
            } else if (type == ArchiveType.JAR || type == ArchiveType.RAR || type == ArchiveType.SAR) {
                JavaArchive jar = ShrinkWrap.create(JavaArchive.class, name);
                addXmlContents((name, xml) -> jar.addAsManifestResource(xml, name));
                addClasses(jar);
                return export(jar);
            }

            // TODO handle other archive types as needed

            return null;
        }

        private void addXmlContents(BiConsumer<String, StringAsset> consumer) {
            for (Map.Entry<String, String> xml : xmlContents.entrySet()) {
                consumer.accept(xml.getKey(), new StringAsset(xml.getValue()));
            }
        }

        private void addClasses(ClassContainer<?> classContainer) {
            for (Class<?> clazz : classes) {
                classContainer.addClass(clazz);
            }
        }

        private Path export(Archive<?> archive) {
            //System.out.println(archive.toString(true));
            ZipExporter zipExporter = archive.as(ZipExporter.class);
            Path path = ARCHIVES_PATH.resolve(name);
            zipExporter.exportTo(path.toFile());
            return path;
        }
    }

    public enum ArchiveType {
        WAR("war"),
        JAR("jar"),
        RAR("rar"),
        SAR("sar");

        private final String suffix;

        ArchiveType(String suffix) {
            this.suffix = suffix;
        }
    }
}
