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

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.io.Serializable;
import java.net.SocketPermission;
import java.net.URL;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public class AbstactUnmarshallingFilterTestCase {

    static Archive<?> createDeployment(Class clazz) {
        String SERVER_HOST_PORT = TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort();

        return ShrinkWrap.create(JavaArchive.class, clazz.getSimpleName() + ".jar")
                .addPackage(AbstactUnmarshallingFilterTestCase.class.getPackage())
                .addAsManifestResource(new StringAsset("Dependencies: org.apache.xalan\n"), "MANIFEST.MF")
                .addAsManifestResource(createPermissionsXmlAsset(
                        new SocketPermission(SERVER_HOST_PORT, "connect,resolve")
                ), "permissions.xml");
    }


    @ArquillianResource
    private URL url;

    <T> T lookup(String beanName, Class<T> interfaceType, boolean http) throws NamingException {
        final Context jndiContext = getContext(http);

        return interfaceType.cast(jndiContext.lookup(String.format("ejb:/%s/%s!%s",
                getClass().getSimpleName(), beanName, interfaceType.getName())));
    }

    private Context getContext(boolean http) throws NamingException {
        final Hashtable<String, String> jndiProperties = new Hashtable<>();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        jndiProperties.put(Context.SECURITY_PRINCIPAL, "user1");
        jndiProperties.put(Context.SECURITY_CREDENTIALS, "password1");

        String addressPort = url.getHost() + ":" + url.getPort();
        if (http) {
            // use HTTP based invocation. Each invocation will be a HTTP request
            jndiProperties.put(Context.PROVIDER_URL, "http://" + addressPort + "/wildfly-services");
        } else {
            // use HTTP upgrade, an initial upgrade requests is sent to upgrade to the
            // remoting protocol
            jndiProperties.put(Context.PROVIDER_URL, "remote+http://" + addressPort);
        }
        return new InitialContext(jndiProperties);
    }

    static Serializable getTemplatesImpl()  {
        // Use reflection so we don't need this class on the compilation classpath
        // This call executes in the server in a deployment that has this class's module
        // configured as a dependency
        try {
            return (Serializable) Class.forName("org.apache.xalan.xsltc.trax.TemplatesImpl").newInstance();
        } catch (InstantiationException | IllegalAccessException  | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
