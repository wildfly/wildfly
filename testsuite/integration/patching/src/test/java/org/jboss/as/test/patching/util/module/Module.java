package org.jboss.as.test.patching.util.module;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.metadata.ModuleItem;
import org.jboss.as.test.patching.PatchingTestUtil;
import org.jboss.as.test.patching.ResourceItem;

/**
 * @author Martin Simka
 */
public class Module {
    private String namespace;
    private String name;
    private String slot;
    private Properties properties;
    private List<String> dependencies;
    private List<ResourceItem> resourceRoots;
    private List<ResourceItem> miscFiles;

    private Module(Builder builder) {
        this.namespace = builder.namespace;
        this.name = builder.name;
        this.slot = builder.slot;
        this.properties = builder.properties;
        this.dependencies = builder.dependencies;
        this.resourceRoots = builder.resourceRoots;
        this.miscFiles = builder.miscFiles;
    }

    public String getName() {
        return name;
    }

    public String getSlot() {
        return slot;
    }

    public String generateXml() {
        StringBuilder stringBuilder = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        String rootElementTemplate = "<module xmlns=\"%s\" name=\"%s\" slot=\"%s\">\n";
        stringBuilder.append(String.format(rootElementTemplate, namespace, name, slot));
        if (!properties.isEmpty()) {
            stringBuilder.append("     <properties>\n");
            String propertyTemplate = "          <property name=\"%s\" value=\"%s\"/>\n";
            for (String key : properties.stringPropertyNames()) {
                stringBuilder.append(String.format(propertyTemplate, key, properties.getProperty(key)));
            }
            stringBuilder.append("     </properties>\n");
        }

        stringBuilder.append("     <resources>\n");
        String resourceRootTemplate = "          <resource-root path=\"%s\"/>\n";
        for (ResourceItem resourceRoot : resourceRoots) {
            stringBuilder.append(String.format(resourceRootTemplate, resourceRoot.getItemName()));
        }
        stringBuilder.append(String.format(resourceRootTemplate, "."));
        stringBuilder.append("     </resources>\n");

        if (!resourceRoots.isEmpty()) {
            stringBuilder.append("     <dependencies>\n");
            String dependencyTemplate = "          <module name=\"%s\"/>\n";
            for (String module : dependencies) {
                stringBuilder.append(String.format(dependencyTemplate, module));
            }
            stringBuilder.append("     </dependencies>\n");
        }
        stringBuilder.append("</module>\n");
        return stringBuilder.toString();
    }

    /**
     * writes module to disk
     *
     * @param baseDir usually modules dir, written path starts with first part of module name
     * @return main dir
     * @throws IOException
     */
    public File writeToDisk(File baseDir) throws IOException {
        File mainDir = IoUtils.mkdir(baseDir, (name + "." + slot).split("\\."));
        File moduleXml = PatchingTestUtil.touch(mainDir, "module.xml");
        PatchingTestUtil.dump(moduleXml, generateXml().getBytes());
        for (ResourceItem resourceRoot : resourceRoots) {
            File f = PatchingTestUtil.touch(mainDir, resourceRoot.getItemName());
            PatchingTestUtil.dump(f, resourceRoot.getContent());
        }
        for (ResourceItem miscFile : miscFiles) {
            File f = PatchingTestUtil.touch(mainDir, miscFile.getItemName());
            PatchingTestUtil.dump(f, miscFile.getContent());
        }
        return mainDir;
    }


    public static class Builder {
        private String namespace;
        private String name;
        private String slot;
        private Properties properties;
        private List<String> dependencies;
        private List<ResourceItem> resourceRoots;
        private List<ResourceItem> miscFiles;

        public Builder(String name, String namespace) {
            if (name == null) {
                throw new NullPointerException("name");
            }
            if (namespace == null) {
                throw new NullPointerException("namespace");
            }
            this.name = name;
            this.namespace = namespace;
            properties = new Properties();
            dependencies = new ArrayList<String>();
            resourceRoots = new ArrayList<ResourceItem>();
            miscFiles = new ArrayList<ResourceItem>();
        }

        public Builder(String name) {
            if (name == null) {
                throw new NullPointerException("name");
            }
            this.name = name;
            this.namespace = "urn:jboss:module:1.2";
            properties = new Properties();
            dependencies = new ArrayList<String>();
            resourceRoots = new ArrayList<ResourceItem>();
            miscFiles = new ArrayList<ResourceItem>();
        }

        public Builder slot(String slot) {
            if (slot == null) {
                throw new NullPointerException("slot");
            }
            this.slot = slot;
            return this;
        }

        public Builder property(String name, String value) {
            if (name == null) {
                throw new NullPointerException("name");
            }
            if (value == null) {
                throw new NullPointerException("value");
            }
            properties.setProperty(name, value);
            return this;
        }

        public Builder dependency(String moduleName) {
            if (moduleName == null) {
                throw new NullPointerException("moduleName");
            }
            dependencies.add(moduleName);
            return this;
        }

        public Builder resourceRoot(ResourceItem resourceRoot) {
            if (resourceRoot == null) {
                throw new NullPointerException("resourceRoot");
            }
            resourceRoots.add(resourceRoot);
            return this;
        }

        public Builder miscFile(ResourceItem miscFile) {
            if (miscFile == null) {
                throw new NullPointerException("miscFile");
            }
            miscFiles.add(miscFile);
            return this;
        }

        public Module build() {
            assert notNull(name);
            assert notNull(namespace);
            if (slot == null) {
                slot = ModuleItem.MAIN_SLOT;
            }
            return new Module(this);
        }

        static boolean notNull(Object o) {
            return o != null;
        }
    }
}
