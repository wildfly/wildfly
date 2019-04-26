/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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
package org.jboss.as.weld;

/**
 * Capability names exposed by Weld subsystem.
 * <p>
 * This class is designed to be used outside of the weld subsystem. Other subsystems that require the name of a Weld
 * capability can safely import this class without adding a jboss module dependency. To archive it, this class must
 * only declare string constants, which are resolved at compile time.
 *
 * @author Yeray Borges
 */
public final class Capabilities {
    public static final String WELD_CAPABILITY_NAME = "org.wildfly.weld";
}
