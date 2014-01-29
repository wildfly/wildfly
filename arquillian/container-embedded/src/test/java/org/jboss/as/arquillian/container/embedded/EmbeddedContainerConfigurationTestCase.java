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

import static org.junit.Assert.*;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.junit.Test;

import java.io.File;

/**
 * @author <a href="mailto:tommy.tynja@diabol.se">Tommy Tynj&auml;</a>
 */
public class EmbeddedContainerConfigurationTestCase {

    @Test
    public void shouldValidateDefaultConfiguration() {
        final EmbeddedContainerConfiguration conf = new EmbeddedContainerConfiguration();
        conf.validate();
    }

    @Test(expected = ConfigurationException.class)
    public void shouldValidateThatModulePathIsNonExisting() {
        final EmbeddedContainerConfiguration conf = new EmbeddedContainerConfiguration();
        conf.setModulePath("");
        validate(conf);
    }

    @Test(expected = ConfigurationException.class)
    public void shouldValidateThatBundlePathIsNonExisting() {
        final EmbeddedContainerConfiguration conf = new EmbeddedContainerConfiguration();
        conf.setBundlePath("");
        validate(conf);
    }

    @Test
    public void shouldValidateThatModulePathAndBundlePathExists() {
        final EmbeddedContainerConfiguration conf = new EmbeddedContainerConfiguration();
        createDir(conf.getModulePath());
        createDir(conf.getBundlePath());
        validate(conf);
    }

    private void validate(final EmbeddedContainerConfiguration conf) {
        assertNotNull(conf.getJbossHome());
        assertNotNull(conf.getModulePath());
        assertNotNull(conf.getBundlePath());
        conf.validate();
    }

    private static void createDir(final String path) {
        File dir = new File(path);
        if (!dir.exists())
            assertTrue("Failed to create directory", dir.mkdirs());
    }
}
