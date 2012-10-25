/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container.embedded;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;
import org.jboss.arquillian.container.spi.client.deployment.Validate;
import org.jboss.as.arquillian.container.CommonContainerConfiguration;

/**
 * {@link ContainerConfiguration} implementation for JBoss AS Embedded
 *
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 * @author <a href="mailto:mmatloka@gmail.com">Michal Matloka</a>
 */
public class EmbeddedContainerConfiguration extends CommonContainerConfiguration {

    private String jbossHome = System.getenv("JBOSS_HOME");

    private String modulePath = System.getProperty("module.path");

    private String bundlePath = System.getProperty("bundle.path");

    public EmbeddedContainerConfiguration() {

        // if no jbossHome is set use jboss.home of already running jvm
        if (jbossHome == null || jbossHome.isEmpty()) {
            jbossHome = System.getProperty("jboss.home");
        }

        if (modulePath == null || modulePath.isEmpty()) {
            modulePath = jbossHome + "/modules";
        }

        if (bundlePath == null || bundlePath.isEmpty()) {
            bundlePath = jbossHome + "/bundles";
        }
    }

    /**
     * @return the jbossHome
     */
    public String getJbossHome() {
        return jbossHome;
    }

    /**
     * @param jbossHome the jbossHome to set
     */
    public void setJbossHome(String jbossHome) {
        this.jbossHome = jbossHome;
    }

    public String getModulePath() {
        return modulePath;
    }

    public void setModulePath(final String modulePath) {
        this.modulePath = modulePath;
    }

    public String getBundlePath() {
        return bundlePath;
    }

    public void setBundlePath(String bundlePath) {
        this.bundlePath = bundlePath;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.as.arquillian.container.CommonContainerConfiguration#validate()
     */
    @Override
    public void validate() throws ConfigurationException {
        super.validate();
        Validate.configurationDirectoryExists(jbossHome, "jbossHome '" + jbossHome + "' must exist");
        Validate.configurationDirectoryExists(jbossHome, "modulePath '" + modulePath + "' must exist");
        Validate.configurationDirectoryExists(jbossHome, "bundlePath '" + bundlePath + "' must exist");
    }
}
