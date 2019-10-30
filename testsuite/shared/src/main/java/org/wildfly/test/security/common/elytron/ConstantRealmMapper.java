/*
 * Copyright 2019 Red Hat, Inc.
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

package org.wildfly.test.security.common.elytron;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;

/**
 * A {@link ConfigurableElement} to define a constant realm mapper resource.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ConstantRealmMapper implements ConfigurableElement {

    private final PathAddress address;
    private final String name;
    private final String realm;

    ConstantRealmMapper(String name, String realm) {
        this.name = name;
        this.address = PathAddress.pathAddress(PathElement.pathElement("subsystem", "elytron"), PathElement.pathElement("constant-realm-mapper", name));
        this.realm = realm;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        ModelNode addOperation = Util.createAddOperation(address);
        addOperation.get("realm-name").set(realm);
        Utils.applyUpdate(addOperation, client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        ModelNode removeOperation = Util.createRemoveOperation(address);
        Utils.applyUpdate(removeOperation, client);
    }

    public static ConfigurableElement newInstance(final String name, final String realm) {
        return new ConstantRealmMapper(name, realm);
    }


}
