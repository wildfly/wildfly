/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.undertow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.model.test.ModelTestControllerVersion;

/**
 * @author wangc
 *
 */
public class UndertowDependencies {

    private static final Map<ModelTestControllerVersion, String[]> UNDERTOW_DEPENDENCIES;
    static {
        Map<ModelTestControllerVersion, String[]> map = new HashMap<ModelTestControllerVersion, String[]>();

        map.put(ModelTestControllerVersion.EAP_7_1_0, new String[]{
                String.format("org.jboss.eap:wildfly-clustering-common:%s", ModelTestControllerVersion.EAP_7_1_0.getMavenGavVersion()),
                String.format("org.jboss.eap:wildfly-web-common:%s", ModelTestControllerVersion.EAP_7_1_0.getMavenGavVersion()),
        });

        UNDERTOW_DEPENDENCIES = Collections.unmodifiableMap(map);
    }

    static String[] getUndertowDependencies(ModelTestControllerVersion controllerVersion) {
        return UNDERTOW_DEPENDENCIES.get(controllerVersion);
    }
}
