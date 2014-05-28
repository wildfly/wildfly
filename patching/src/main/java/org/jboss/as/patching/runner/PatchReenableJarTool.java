package org.jboss.as.patching.runner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchReenableJarTool {

    static FilenameFilter JAR_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".jar");
        }
    };

    public static void main(String... args) throws IOException {

        final String path = args[0];
        final File jbossHome = new File(path);
        if (!jbossHome.exists()) {
            throw notFound(jbossHome);
        }
        final File modules = new File(jbossHome, "modules");
        if (!modules.exists()) {
            throw notFound(modules);
        }
        final File base = new File(modules, "system/layers/base");
        if (!base.exists()) {
            throw notFound(base);
        }

        final List<File> jars = new ArrayList<>();
        findModuleRoots(base, jars);

        for (final File file : jars) {
            PatchModuleInvalidationUtils.processFile(null, file, PatchingTaskContext.Mode.ROLLBACK);
        }
    }

    static void findModuleRoots(final File file, final List<File> jars) {
        final File moduleXml = new File(file, "module.xml");
        if (moduleXml.exists()) {
            findJars(file, jars);
        } else {
            final File[] files = file.listFiles();
            if (files != null) {
                for (final File child : files) {
                    if (child.getName().equals(".overlays")) {
                        continue;
                    }
                    if (child.isDirectory()) {
                        findModuleRoots(child, jars);
                    }
                }
            }
        }
    }

    static void findJars(final File moduleRoot, final List<File> jars) {
        final File[] files = moduleRoot.listFiles(JAR_FILTER);
        if (files != null) {
            for (final File jar : files) {
                jars.add(jar);
            }
        }
    }

    static FileNotFoundException notFound(File file) {
        return new FileNotFoundException(file.getAbsolutePath() + " does not exist");
    }

}
