/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.micrometer.model;

import java.util.Locale;

public enum MicrometerSchema {
    VERSION_1_0(1, 0), // WildFly Preview 27
    ;

    public static final MicrometerSchema CURRENT = VERSION_1_0;


    private final int major;
    private final int minor;

    MicrometerSchema(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public int major() {
        return this.major;
    }

    public int minor() {
        return this.minor;
    }

    public String getNamespaceUri() {
        return String.format(Locale.ROOT, "urn:wildfly:micrometer:%d.%d", this.major, this.minor);
    }
}
