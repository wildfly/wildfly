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

package org.wildfly.test.integration.changedetection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.wildfly.test.integration.util.AbstractWorkspaceReplacement;

/**
 * manager/builder for exploded war deployments
 */
public class ExplodedDeploymentManager implements AutoCloseable {

    private static final String DEPLOYMENTS_DIR = System.getenv("JBOSS_HOME") + "/standalone/deployments";

    private final File deploymentRoot;
    private final File markerFile;
    private final File doneFile;
    private final File failedFile;
    private final File undeployedFile;
    private final String name;
    private final Class<?> testClass;
    private final File classesRoot;
    private final List<String> webResources = new ArrayList<>();
    private final List<Class<?>> classes = new ArrayList<>();
    private final ExplodedReplacementStrategy explodedReplacementStrategy;

    private ExplodedDeploymentManager(Builder builder) {
        this.name = builder.name;
        this.testClass = builder.testClass;
        this.deploymentRoot = new File(DEPLOYMENTS_DIR + File.separatorChar + builder.name);
        this.markerFile = new File(DEPLOYMENTS_DIR + File.separatorChar + builder.name + ".dodeploy");
        this.doneFile = new File(DEPLOYMENTS_DIR + File.separatorChar + builder.name + ".deployed");
        this.failedFile = new File(DEPLOYMENTS_DIR + File.separatorChar + builder.name + ".failed");
        this.undeployedFile = new File(DEPLOYMENTS_DIR + File.separatorChar + builder.name + ".undeployed");
        classesRoot = new File(this.deploymentRoot, "WEB-INF/classes");
        this.webResources.addAll(builder.webResources);
        this.classes.addAll(builder.classes);
        this.explodedReplacementStrategy = builder.strategy;

    }

    protected void doSetup() {
        try {
            explodedReplacementStrategy.init(this);
            deploymentRoot.mkdir();
            for (String resource : webResources) {
                File target = new File(deploymentRoot, resource);
                try (InputStream res = testClass.getResourceAsStream(resource)) {
                    AbstractWorkspaceReplacement.copy(target, res);
                }
                target.setLastModified(target.lastModified() - 2000);
            }
            classesRoot.mkdirs();
            for (Class<?> resource : classes) {
                File target = new File(classesRoot, resource.getName().replace('.', '/') + ".class");
                target.getParentFile().mkdirs();
                try (InputStream res = resource.getResourceAsStream(resource.getSimpleName() + ".class")) {
                    AbstractWorkspaceReplacement.copy(target, res);
                }
                target.setLastModified(target.lastModified() - 2000);
            }

            try (FileOutputStream out = new FileOutputStream(markerFile)) {
                //create empty marker file
            }
            //now we wait for deployment
            long timeout = System.currentTimeMillis() + 30000; //wait at most 30s
            while (System.currentTimeMillis() < timeout) {
                if (doneFile.exists()) {
                    return;
                }
                if (failedFile.exists()) {
                    throw new RuntimeException("Failed to deploy " + deploymentRoot + " see server logs for more details");
                }
                Thread.sleep(100);
            }
            throw new RuntimeException("Timed out attempting to deploy " + deploymentRoot + " see server logs for more details");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getDeploymentUrl() {
        return "http://" + TestSuiteEnvironment.getServerAddress() + ":" + TestSuiteEnvironment.getHttpPort() + "/test/";
    }


    public void replaceWebResource(String original, String newResource) {
        explodedReplacementStrategy.replaceWebResource(original, newResource);
    }

    public void addWebResource(String s) {
        explodedReplacementStrategy.addWebResource(s);
    }

    public void replaceClass(Class<?> original, Class<?> replacement) {
        explodedReplacementStrategy.replaceClass(original, replacement);
    }

    public void addClass(Class<?> theClass) {
        explodedReplacementStrategy.addClass(theClass);
    }

    public File getDeploymentRoot() {
        return deploymentRoot;
    }

    public File getMarkerFile() {
        return markerFile;
    }

    public File getDoneFile() {
        return doneFile;
    }

    public File getFailedFile() {
        return failedFile;
    }

    public File getUndeployedFile() {
        return undeployedFile;
    }

    public String getName() {
        return name;
    }

    public Class<?> getTestClass() {
        return testClass;
    }

    public File getClassesRoot() {
        return classesRoot;
    }

    public List<String> getWebResources() {
        return webResources;
    }

    public List<Class<?>> getClasses() {
        return classes;
    }

    @Override
    public void close() {
        explodedReplacementStrategy.close();
        doneFile.delete();
        long timeout = System.currentTimeMillis() + 30000; //wait at most 30s
        while (System.currentTimeMillis() < timeout) {
            if (undeployedFile.exists() && !doneFile.exists()) {
                undeployedFile.delete();
                AbstractWorkspaceReplacement.deleteRecursively(deploymentRoot);
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Timed out attempting to undeploy " + deploymentRoot + " see server logs for more details");
    }

    public static class Builder {

        private final Class<?> testClass;
        private final String name;
        private ExplodedReplacementStrategy strategy;

        private final List<String> webResources = new ArrayList<>();
        private final List<Class<?>> classes = new ArrayList<>();

        public Builder(Class<?> testClass, String name) {
            this.testClass = testClass;
            this.name = name;
        }

        public Builder setStrategy(ExplodedReplacementStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder addWebResources(String... resources) {
            webResources.addAll(Arrays.asList(resources));
            return this;
        }

        public Builder addClasses(Class<?>... classes) {
            this.classes.addAll(Arrays.asList(classes));
            return this;
        }

        public ExplodedDeploymentManager buildAndDeploy() {
            ExplodedDeploymentManager explodedDeploymentManager = new ExplodedDeploymentManager(this);
            explodedDeploymentManager.doSetup();
            return explodedDeploymentManager;
        }

    }
}
