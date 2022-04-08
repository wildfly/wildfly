/*
 * Copyright (c) 2020. Red Hat, Inc.
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

package org.jboss.as.test.integration.ejb.remote.requestdeserialization;

import java.io.Serializable;
import java.util.HashSet;
import javax.ejb.EJBException;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup(UnmarshallingFilterTestCase.ServerSetup.class)
public class UnmarshallingFilterTestCase extends AbstactUnmarshallingFilterTestCase {

    public static class ServerSetup implements ServerSetupTask {
        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            // This test results in construction of a org.jboss.ejb.protocol.remote.EJBClientChannel
            // in the server which affects the behavior of later tests. So reload to clear it out.
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }
    }

    @Deployment
    public static Archive<?> createDeployment() {
        return createDeployment(UnmarshallingFilterTestCase.class);
    }

    @Test
    public void testRemotingBlacklist() throws NamingException {
        testBlacklist(false);
    }

    @Test
    public void testHttpBlacklist() throws NamingException {
        testBlacklist(true);
    }

    private void testBlacklist(boolean http) throws NamingException {
        HelloRemote bean = lookup(HelloBean.class.getSimpleName(), HelloRemote.class, http);
        Serializable blacklisted = getTemplatesImpl();
        try {
            Response response = bean.sayHello(new Request(blacklisted));
            Assert.fail(response.getGreeting());
        } catch (EJBException good) {
            //
        }
        try {
            HashSet<Serializable> set = new HashSet<>();
            set.add(blacklisted);
            Response response = bean.sayHello(new Request(set));
            Assert.fail(response.getGreeting());
        } catch (EJBException good) {
            //
        }
    }
}
