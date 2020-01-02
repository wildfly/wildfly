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
package org.jboss.as.test.manualmode.ejb.client.exception;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

@Stateless
@Remote(ClientInterface.class)
public class Client implements ClientInterface {

    public void callBean() throws Exception {
        Context ctx = getIntialContext();
        final String lookupString = "ejb:DeploymentEjb/ejbJar/SimpleRemoteBean!org.jboss.as.test.manualmode.ejb.client.exception.SimpleRemote";
        SimpleRemote bean = (SimpleRemote) ctx.lookup(lookupString);
        bean.throwBadException();
    }

    public static Context getIntialContext() throws NamingException {
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        props.put(Context.PROVIDER_URL, "http://localhost:8080/wildfly-services");
        props.put(Context.SECURITY_PRINCIPAL, System.getProperty("username", "user1"));
        props.put(Context.SECURITY_CREDENTIALS, System.getProperty("password", "password1"));
        return new InitialContext(props);
    }
}
