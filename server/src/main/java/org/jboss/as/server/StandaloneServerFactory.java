/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.server;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Use to construct a standalone server assuming modular classloading has already been set up
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public final class StandaloneServerFactory {

    // Hide ctor
    private StandaloneServerFactory() {
    }

    public static StandaloneServer create(final ServerEnvironment environment) {
        return new StandaloneServerImpl(environment);
    }

    protected static URL failsafeURL(File modulesJar) {
        try {
            return modulesJar.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(modulesJar.getAbsolutePath());
        }
    }

}