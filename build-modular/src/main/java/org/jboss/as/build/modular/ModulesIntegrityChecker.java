/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.build.modular;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import org.jboss.as.config.assembly.ModuleParser;
import org.jboss.as.config.assembly.ModuleParser.ModuleDependency;
import org.jboss.logging.Logger;
import org.jboss.modules.LocalModuleLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleNotFoundException;
import org.jboss.modules.Resource;

/**
 * Check integrity of the modules hirarchy
 *
 * @author Thomas.Diesler@jboss.com
 * @since 15-Jul-2013
 */
public class ModulesIntegrityChecker {

    static final Logger log = Logger.getLogger(ModulesIntegrityChecker.class);

    private final File modulesDir;
    private final ModuleLoader moduleLoader;
    private final ModulesIntegrityResult result = new ModulesIntegrityResult();
    private final Map<ModuleIdentifier, Set<String>> modulePackageIgnore = new HashMap<ModuleIdentifier, Set<String>>();
    private final Set<String> globalPackageIgnore = new HashSet<String>();
    private final boolean failOnError;

    private Set<ModuleIdentifier> processed = new HashSet<ModuleIdentifier>();

    /**
     * arg[0] - The modules directory (e.g. wildfly/build/target/wildfly-8.0.0/modules/system/layers/base)
     * arg[1] - Optional comma seperated list of global packages to ignore (e.g. org.jboss.logging.annotations)
     * arg[2] - Optional file containing the set of module specific packages to ignore.
     * arg[3] - Optional failOnError flag (default is 'false')
     * arg[4] - Optional module identifier (e.g. org.jboss.vfs:main)
     *
     * Package names are indented. Hash comments and empty lines are supported.
     *
     * org.jboss.common-core:main
     *    org.apache.commons.httpclient
     *    org.apache.webdav.lib
     */
    public static void main(String[] args) throws Exception {
        if (args == null)
            throw new IllegalArgumentException("Null args");
        if (args.length < 1)
            throw new IllegalArgumentException("Invalid args: " + Arrays.asList(args));

        String[] globalIgnore = args.length > 1 ? args[1].split(",") : null;
        File moduleIgnore = args.length > 2 ? new File(args[2]) : null;
        Boolean failOnError = args.length > 3 ? Boolean.parseBoolean(args[2]) : null;

        ModulesIntegrityChecker checker = new ModulesIntegrityChecker(new File(args[0]), globalIgnore, moduleIgnore, failOnError);
        checker.checkIntegrity(args.length > 4 ? ModuleIdentifier.fromString(args[4]) : null);
    }

    public ModulesIntegrityChecker(File modulesDir) throws IOException {
        this(modulesDir, null, null, null);
    }

    public ModulesIntegrityChecker(File modulesDir, String[] globalIgnore, File moduleIgnore, Boolean failOnErrror) throws IOException {
        if (modulesDir == null || !modulesDir.isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + modulesDir);
        }
        this.modulesDir = modulesDir;
        this.moduleLoader = new LocalModuleLoader(new File[] { modulesDir });
        this.failOnError = Boolean.TRUE.equals(failOnErrror);

        if (globalIgnore != null) {
            addGlobalPackageIgnore(globalIgnore);
        }
        if (moduleIgnore != null) {
            ModuleIdentifier modid = null;
            BufferedReader br = new BufferedReader(new FileReader(moduleIgnore));
            try {
                String line = br.readLine();
                while (line != null) {
                    if (!line.startsWith("#") && line.trim().length() > 0) {
                        if (line.startsWith(" ") || line.startsWith("\t")) {
                            addModulePackageIgnore(modid, line.trim());
                        } else {
                            modid = ModuleIdentifier.fromString(line.trim());
                        }
                    }
                    line = br.readLine();
                }
            } finally {
                br.close();
            }
        }
    }

    public ModulesIntegrityResult checkIntegrity() throws Exception {
        return checkIntegrity(null);
    }

    public ModulesIntegrityResult checkIntegrity(ModuleIdentifier identifier) throws Exception {
        if (identifier != null)
            processModuleDirectory(toModuleDir(identifier));
        else
            processModuleDirectory(modulesDir);

        int errorCount = result.getErrorCount();
        int totalCount = result.getModuleCount();

        // Print the result
        if (errorCount > 0) {
            Map<ModuleIdentifier, Set<String>> noncomplete = result.getNonComplete();
            for (Entry<ModuleIdentifier, Set<String>> entry : noncomplete.entrySet()) {
                ModuleIdentifier modid = entry.getKey();
                System.err.println(modid);
                for (String pname : entry.getValue()) {
                    System.err.println("   " + pname);
                }
            }

            System.err.println();
            System.err.println(errorCount + " of " + totalCount + " modules inconsistent");

            if (failOnError)
                throw new IllegalStateException("Found " + errorCount + "  inconsistencies");
        }

        return result;
    }

    public ModulesIntegrityChecker addGlobalPackageIgnore(String... packages) {
        for (String pname : packages) {
            globalPackageIgnore.add(pname);
        }
        return this;
    }

    public ModulesIntegrityChecker addModulePackageIgnore(ModuleIdentifier identifier, String... packages) {
        Set<String> pset = modulePackageIgnore.get(identifier);
        if (pset == null) {
            pset = new HashSet<String>();
            modulePackageIgnore.put(identifier, pset);
        }
        for (String pname : packages) {
            pset.add(pname);
        }
        return this;
    }

    private void processModuleDirectory(File directory) throws Exception {
        for (File file : directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().equals("module.xml");
            }
        })) {
            if (file.isDirectory()) {
                processModuleDirectory(file);
            } else {
                processModulesDefinition(file);
            }
        }
    }

    private void processModulesDefinition(File file) throws Exception {
        ModuleParser parser = new ModuleParser(file).parse();
        ModuleIdentifier identifier = parser.getIdentifier();
        if (identifier != null) {
            processModule(identifier, parser.getDependencies());
        }
    }

    private void processModule(ModuleIdentifier identifier, List<ModuleDependency> dependencies) throws Exception {
        if (processed.contains(identifier))
            return;

        // Mark as already processed
        processed.add(identifier);
        result.moduleCount.incrementAndGet();

        // Depth-First dependency processing
        Set<ModuleIdentifier> ignored = new HashSet<ModuleIdentifier>();
        for (ModuleDependency dep : dependencies) {
            ModuleIdentifier depid = dep.getModuleIdentifier();
            Module depmod = null;
            try {
                depmod = moduleLoader.loadModule(depid);
            } catch (ModuleNotFoundException e) {
                ignored.add(depid);
                if (!dep.isOptional()) {
                    result.nonexisting.add(ModuleIdentifier.fromString(e.getMessage()));
                }
            }
            if (depmod != null) {
                File modxml = toModuleXML(depid);
                processModulesDefinition(modxml);
            }
        }

        log.debugf("Checking module: %s", identifier);
        ClassPool classPool = ClassPool.getDefault();

        // Add this module to the class pool
        ModuleClassLoader classLoader = moduleLoader.loadModule(identifier).getClassLoader();
        classPool.appendClassPath(new LoaderClassPath(classLoader));

        // Add the dependencies to the class pool
        for (ModuleDependency dep : dependencies) {
            ModuleIdentifier depid = dep.getModuleIdentifier();
            if (!ignored.contains(depid)) {
                ModuleClassLoader depcl = moduleLoader.loadModule(depid).getClassLoader();
                classPool.appendClassPath(new LoaderClassPath(depcl));
            }
        }

        Set<String> noncomplete = new HashSet<String>();

        Iterator<Resource> itres = classLoader.iterateResources("/", true);
        while (itres.hasNext()) {
            Resource res = itres.next();
            String resname = res.getName();
            if (!resname.endsWith(".class") || resname.endsWith("package-info.class") || resname.lastIndexOf('$') > 0)
                continue;

            String classname = resname.substring(0, resname.length() - 6).replace('/', '.');
            checkClassResource(classPool, identifier, classname, noncomplete);
        }
        if (!noncomplete.isEmpty()) {
            log.debugf("Not complete module: %s", identifier);
            List<String> sorted = new ArrayList<String>(noncomplete);
            Collections.sort(sorted);
            for (String pname : sorted) {
                log.debugf(" %s", pname);
            }
            result.noncomplete.put(identifier, new LinkedHashSet<String>(sorted));
        }
    }

    private void checkClassResource(ClassPool classPool, ModuleIdentifier identifier, String classname, Set<String> notfound) throws Exception {
        CtClass cc = classPool.get(classname);
        Collection<?> refs;
        try {
            refs = cc.getRefClasses();
        } catch (RuntimeException rte) {
            log.debugf("Cannot get references for: %s", classname);
            return;
        }

        for (Object obj : refs) {
            String ref = (String) obj;
            if (ref.startsWith("java.") || ref.lastIndexOf('$') > 0)
                continue;

            try {
                classPool.get(ref);
            } catch (NotFoundException ex) {
                String pname = ref.substring(0, ref.lastIndexOf('.'));
                Set<String> moduleIgnore = modulePackageIgnore.get(identifier);
                boolean ignoreNotFound = globalPackageIgnore.contains(pname);
                ignoreNotFound |= (moduleIgnore != null && moduleIgnore.contains(pname));
                if (ignoreNotFound == false) {
                    notfound.add(pname);
                }
            }
        }
    }

    private File toModuleDir(ModuleIdentifier modid) {
        String pathname = modid.getName().replace('.', File.separatorChar) + File.separator + modid.getSlot();
        return new File(modulesDir.getAbsolutePath() + File.separator + pathname);
    }

    private File toModuleXML(ModuleIdentifier depid) {
        return new File(toModuleDir(depid).getAbsolutePath() + File.separator + "module.xml");
    }

    public static class ModulesIntegrityResult {

        AtomicInteger moduleCount = new AtomicInteger();
        Set<ModuleIdentifier> nonexisting = new HashSet<ModuleIdentifier>();
        Map<ModuleIdentifier, Set<String>> noncomplete = new HashMap<ModuleIdentifier, Set<String>>();

        public int getModuleCount() {
            return moduleCount.get();
        }

        public Set<ModuleIdentifier> getNonExisting() {
            return Collections.unmodifiableSet(nonexisting);
        }

        public Map<ModuleIdentifier, Set<String>> getNonComplete() {
            return Collections.unmodifiableMap(noncomplete);
        }

        public int getErrorCount() {
            return nonexisting.size() + noncomplete.size();
        }
    }

}
