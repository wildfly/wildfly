/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.classchange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.Resource;
import org.wildfly.extension.classchange.logging.ClassChangeMessages;

/**
 * Class that handles compilation of source files
 *
 * @author Stuart Douglas
 */
public class ClassLoaderCompiler {

    private final ModuleClassLoader classLoader;
    private final Path base;
    private final List<String> classBaseNames;
    private final Map<String, ByteArrayOutputStream> output = new HashMap<>();

    public ClassLoaderCompiler(ModuleClassLoader classLoader, Path base, List<String> classBaseNames) {
        this.classLoader = classLoader;
        this.base = base;
        this.classBaseNames = classBaseNames;
    }

    public void compile() {
        // Compile source file.
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager standard = compiler.getStandardFileManager(null, null, null);
        DiagnosticCollector<JavaFileObject> diagnosticListener = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask task = compiler.getTask(null, new ClassLoaderJavaFileManager(standard), diagnosticListener, null, null, classBaseNames.stream().map(new Function<String, LocalFileObject>() {
            @Override
            public LocalFileObject apply(String name) {
                return new LocalFileObject(base.resolve(name.replace(".", "/") + ".java"), name, Kind.SOURCE);
            }
        }).collect(Collectors.toList()));
        if (!task.call()) {
            throw ClassChangeMessages.ROOT_LOGGER.compileFailed(diagnosticListener.getDiagnostics());
        }
    }

    public Map<String, ByteArrayOutputStream> getOutput() {
        return Collections.unmodifiableMap(output);
    }

    private class LocalFileObject implements JavaFileObject {

        private final Path path;
        private final String className;
        private final Kind kind;

        private LocalFileObject(Path path, String className, Kind kind) {
            this.className = className;
            this.kind = kind;
            this.path = path;
        }

        @Override
        public Kind getKind() {
            return kind;
        }

        @Override
        public boolean isNameCompatible(String simpleName, Kind kind) {
            if (kind == this.kind) {
                String fn = path.getFileName().toString();
                int idx = fn.lastIndexOf(".");
                if (idx > 0) {
                    fn = fn.substring(0, idx);
                }
                return fn.equals(simpleName);
            }
            return false;
        }

        @Override
        public NestingKind getNestingKind() {
            return null;
        }

        @Override
        public Modifier getAccessLevel() {
            return null;
        }

        @Override
        public URI toUri() {
            return path.toUri();
        }

        @Override
        public String getName() {
            return path.toString();
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return new FileInputStream(path.toFile());
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            output.put(className, out);
            return out;
        }

        @Override
        public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
            return new FileReader(path.toFile());
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return Files.readAllLines(path, StandardCharsets.UTF_8).stream()
                    .collect(StringBuilder::new, (sb, s) -> {
                        sb.append(s);
                        sb.append("\n");
                    }, StringBuilder::append);
        }

        @Override
        public Writer openWriter() throws IOException {
            return null;
        }

        @Override
        public long getLastModified() {
            try {
                return Files.getLastModifiedTime(path).toMillis();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean delete() {
            return path.toFile().delete();
        }

        public String binaryName() {
            return className;
        }
    }

    private class ClassLoaderJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

        private final JavaFileManager standardJavaFileManager;

        private ClassLoaderJavaFileManager(StandardJavaFileManager standardJavaFileManager) {
            super(standardJavaFileManager);
            this.standardJavaFileManager = new ForwardingJavaFileManager<JavaFileManager>(standardJavaFileManager) {
                @Override
                public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
                    return null;
                }

                @Override
                public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
                    return null;
                }
            };
        }

        @Override
        public ClassLoader getClassLoader(Location location) {
            if (location == StandardLocation.PLATFORM_CLASS_PATH) {
                return standardJavaFileManager.getClassLoader(location);
            }
            return classLoader; //TODO: figure this out for ear deployments
        }

        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
            if (location == StandardLocation.PLATFORM_CLASS_PATH) {
                return standardJavaFileManager.list(location, packageName, kinds, recurse);
            }
            Set<JavaFileObject> ret = new HashSet<>();
            if (kinds.contains(Kind.CLASS) && location.equals(StandardLocation.CLASS_PATH)) {
                final String packageWithSlashes = packageName.replace(".", "/");
                try {
                    Iterator<Resource> resources = classLoader.getModule().iterateResources(path -> {
                        if (recurse) {
                            return path.startsWith(packageWithSlashes);
                        } else {
                            return path.equals(packageWithSlashes);
                        }
                    });
                    while (resources.hasNext()) {
                        Resource res = resources.next();
                        if (!res.getName().endsWith(".class")) {
                            continue;
                        }
                        String binaryName = res.getName().replace("/", ".").substring(0, res.getName().length() - 6);
                        try {
                            ret.add(new ZipJavaFileObject(org.fakereplace.util.FileReader.readFileBytes(res.openStream()), binaryName, res.getURL().toURI()));
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (ModuleLoadException e) {
                    ClassChangeMessages.ROOT_LOGGER.failedToCompileWhileGettingLocation(location, e);
                }
            } else {
                return standardJavaFileManager.list(location, packageName, kinds, recurse);
            }

            return ret;
        }

        private void listDir(String packageName, Path dirPath, boolean recurse, List<JavaFileObject> objects) throws IOException {
            for (Path file : Files.newDirectoryStream(dirPath)) {
                if (Files.isDirectory(file)) {
                    if (recurse) {
                        listDir(packageName + "." + file.getFileName(), file, recurse, objects);
                    }
                } else {
                    Kind kind;
                    if (file.toString().endsWith("module-info.java")) {
                        continue;
                    } else if (file.toString().endsWith(".java")) {
                        kind = Kind.SOURCE;
                    } else if (file.toString().endsWith(".class")) {
                        kind = Kind.CLASS;
                    } else {
                        continue;
                    }
                    String className = packageName + "." + file.getFileName();
                    className = className.substring(0, className.lastIndexOf("."));
                    objects.add(new LocalFileObject(file, className, kind));
                }
            }
        }

        @Override
        public String inferBinaryName(Location location, JavaFileObject file) {
            if (file instanceof LocalFileObject) {
                return ((LocalFileObject) file).binaryName();
            }
            if (file instanceof ZipJavaFileObject) {
                return ((ZipJavaFileObject) file).binaryName();
            }
            String s = standardJavaFileManager.inferBinaryName(location, file);
            if (s.equals("module-info")) {
                //TODO: I have no idea how this should work, but without it everything falls apart
                //once JDK9 is final there should hopefully me more info on this
                return location.getName() + ".module-info";
            }
            return s;
        }

        @Override
        public boolean isSameFile(FileObject a, FileObject b) {
            return false;
        }

        @Override
        public boolean handleOption(String current, Iterator<String> remaining) {
            return false;
        }

        @Override
        public boolean hasLocation(Location location) {
            if (location == StandardLocation.SOURCE_PATH) {
                return true;
            } else if (location == StandardLocation.CLASS_PATH) {
                return true;
            } else if (location == StandardLocation.PLATFORM_CLASS_PATH) {
                return true;
            }
            return false;
        }

        @Override
        public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind) throws IOException {
            return null;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
            if (sibling instanceof LocalFileObject) {
                return (JavaFileObject) sibling;
            }
            return null;
        }

        @Override
        public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
            return null;
        }

        @Override
        public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
            return null;
        }

        @Override
        public void flush() throws IOException {

        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public int isSupportedOption(String option) {
            return 0;
        }
    }

    private static final class ZipJavaFileObject implements JavaFileObject {

        private final byte[] content;

        private final String binaryName;
        private final String simpleName;
        private final URI uri;

        private ZipJavaFileObject(byte[] content, String binaryName, URI uri) {
            this.content = content;
            this.binaryName = binaryName;
            this.simpleName = binaryName.substring(binaryName.lastIndexOf("."));
            this.uri = uri;
        }

        @Override
        public Kind getKind() {
            return Kind.CLASS;
        }

        @Override
        public boolean isNameCompatible(String simpleName, Kind kind) {
            return kind == Kind.CLASS && simpleName.equals(this.simpleName);
        }

        @Override
        public NestingKind getNestingKind() {
            return null;
        }

        @Override
        public Modifier getAccessLevel() {
            return null;
        }

        @Override
        public URI toUri() {
            return null;
        }

        @Override
        public String getName() {
            return uri.toString();
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
            return new InputStreamReader(openInputStream());
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            StringBuilder sb = new StringBuilder();
            try (Reader reader = openReader(ignoreEncodingErrors)) {
                char[] buf = new char[1000];
                int res;
                while ((res = reader.read(buf)) > 0) {
                    sb.append(buf, 0, res);
                }
            }
            return sb.toString();
        }

        @Override
        public Writer openWriter() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getLastModified() {
            return 0;
        }

        @Override
        public boolean delete() {
            return false;
        }

        public String binaryName() {
            return binaryName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ZipJavaFileObject that = (ZipJavaFileObject) o;
            return Objects.equals(binaryName, that.binaryName) &&
                    Objects.equals(simpleName, that.simpleName) &&
                    Objects.equals(uri, that.uri);
        }

        @Override
        public int hashCode() {

            return Objects.hash(binaryName, simpleName, uri);
        }
    }

}
