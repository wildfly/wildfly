/*
 * Copyright 2016 Red Hat, Inc.
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

package org.jboss.as.test.multinode.security.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.arquillian.api.ServerSetupTask;

/**
 * @author bmaxwell
 *
 */
public class PlainSecurityPropagationTestCaseSetup extends AbstractSecurityPropagationTestCaseServerSetup implements ServerSetupTask, ServerConfiguration {

    public PlainSecurityPropagationTestCaseSetup() {
        super();
    }

    @Override
    public ConfigChange[] getClientConfigChanges() {
        List<ConfigChange> all = new ArrayList<>();
        all.addAll(Arrays.asList(PlainClientConfiguration.getClientConfigChanges()));
        all.addAll(Arrays.asList(PlainServerConfiguration.getServerConfigChanges()));
        return all.toArray(new ConfigChange[all.size()]);
//        return PlainClientConfiguration.getClientConfigChanges();
    }

    @Override
    public ConfigChange[] getServerConfigChanges() {
        List<ConfigChange> all = new ArrayList<>();
        all.addAll(Arrays.asList(PlainClientConfiguration.getClientConfigChanges()));
        all.addAll(Arrays.asList(PlainServerConfiguration.getServerConfigChanges()));
        return all.toArray(new ConfigChange[all.size()]);
//      return PlainServerConfiguration.getServerConfigChanges();
    }
}