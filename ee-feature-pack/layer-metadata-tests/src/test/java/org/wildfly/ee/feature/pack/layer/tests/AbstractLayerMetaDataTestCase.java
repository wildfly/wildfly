/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

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
import org.wildfly.glow.ScanArguments;
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
import java.util.function.Consumer;
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
            deleteArchivesDirectory();
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


    protected Set<String> checkLayersForArchive(Path archivePath, String...expectedLayers) {
        return checkLayersForArchive(archivePath, null, expectedLayers);
    }

    protected Set<String> checkLayersForArchive(Path archivePath, ExpectedLayers expectedLayers) {
        return checkLayersForArchive(archivePath, null, expectedLayers);
    }

    protected Set<String> checkLayersForArchive(Path archivePath, Consumer<ScanArguments.Builder> argumentsAugmenter, String...expectedLayers) {
        return checkLayersForArchive(archivePath, argumentsAugmenter, new ExpectedLayers(expectedLayers));
    }

    protected Set<String> checkLayersForArchive(Path archivePath, Consumer<ScanArguments.Builder> argumentsAugmenter, ExpectedLayers expectedLayers) {
        checkMethodCalled = true;
        try {
            checkMethodCalled = true;
            ScanArguments.Builder argumentsBuilder = Arguments.scanBuilder().setBinaries(Collections.singletonList(archivePath));
            if (argumentsAugmenter != null) {
                argumentsAugmenter.accept(argumentsBuilder);
            }
            Arguments arguments = argumentsBuilder.build();
            ScanResults scanResults = GlowSession.scan(MavenResolver.newMavenResolver(), arguments, GlowMessageWriter.DEFAULT);

            Set<String> foundLayers = scanResults.getDiscoveredLayers().stream().map(l -> l.getName()).collect(Collectors.toSet());
            Set<String> foundDecorators = scanResults.getDecorators().stream().map(l -> l.getName()).collect(Collectors.toSet());

            checkLayers(expectedLayers.getExpectedFoundLayers(), foundLayers);
            checkLayers(expectedLayers.getExpectedDecorators(), foundDecorators);

            return foundLayers;
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException)e;
            throw new RuntimeException(e);
        }
    }


    private void checkLayers(Set<String> expectedLayers, Set<String> foundLayers) {
        //foundLayers.removeAll(standardExpectedLayers);

        Assert.assertEquals("\nExpected:\n" + expectedLayers + "\nActual:\n" + foundLayers,
                expectedLayers.size(), foundLayers.size());

        for (String expectedLayer : expectedLayers) {
            Assert.assertTrue(expectedLayer + ": " + foundLayers, foundLayers.contains(expectedLayer));
        }
    }

    protected ArchiveBuilder createArchiveBuilder(ArchiveType type) {
        return new ArchiveBuilder("test-" + System.currentTimeMillis() + "." + type.suffix, type);
    }

    protected ArchiveBuilder createArchiveBuilder(String name, ArchiveType type) {
        return new ArchiveBuilder(name, type);
    }

    private static void deleteArchivesDirectory() throws IOException {
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


    public class ExpectedLayers {
        private Set<String> layers = new HashSet<>();
        private Set<String> decoratorLayers = new HashSet<>();
        private Set<String> excludedLayers = new HashSet<>();

        public ExpectedLayers() {
        }

        public ExpectedLayers(String layer) {
            addLayer(layer);
        }

        public ExpectedLayers(String... layers) {
            this.layers.addAll(Arrays.asList(layers));
        }

        public ExpectedLayers(String layer, String decorator) {
            addLayerAndDecorator(layer, decorator);
        }

        public ExpectedLayers addLayer(String layer) {
            this.layers.add(layer);
            return this;
        }

        public ExpectedLayers addLayerAndDecorator(String layer, String decorator) {
            layers.add(layer);
            decoratorLayers.add(decorator);
            return this;
        }

        public ExpectedLayers addDecorator(String decorator) {
            decoratorLayers.add(decorator);
            return this;
        }

        public ExpectedLayers excludedLayers(String...excludedDecorators) {
            excludedLayers.addAll(Arrays.asList(excludedDecorators));
            return this;
        }

        private Set<String> getExpectedFoundLayers() {
            return layers;
        }

        private Set<String> getExpectedDecorators() {
            Set<String> decorators = new HashSet<>();
            for (String decorator : decoratorLayers) {
                decorators.add(decorator);
            }
            return decorators;
        }
    }
}
