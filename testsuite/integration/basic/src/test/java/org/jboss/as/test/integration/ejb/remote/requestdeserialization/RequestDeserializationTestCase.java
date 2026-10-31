/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.requestdeserialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
public class RequestDeserializationTestCase {

    @Deployment
    public static Archive<?> createDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, RequestDeserializationTestCase.class.getSimpleName() + ".jar");
        jar.addPackage(RequestDeserializationTestCase.class.getPackage());
        return jar;
    }

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        final Hashtable<String, String> props = new Hashtable<>();
//        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        props.put("java.naming.factory.initial","org.wildfly.naming.client.WildFlyInitialContextFactory");
        final Context jndiContext = new InitialContext(props);

        return interfaceType.cast(jndiContext.lookup(String.format("ejb:/%s/%s!%s",
                getClass().getSimpleName(), beanName, interfaceType.getName())));
    }

    @Test
    @RunAsClient
    public void test() throws NamingException {
        HelloRemote bean = lookup(HelloBean.class.getSimpleName(), HelloRemote.class);
        Response response = bean.sayHello(new Request("Cheers"));

        // check the TCCL used during unmarshalling of parameters on the server side, it should be the deployment
        // classloader, not the "org.wildfly.extension.io" module classloader
        assertThat(response.getTccl()).doesNotContain("org.wildfly.extension.io");
        assertThat(response.getTccl()).contains(getClass().getSimpleName());
    }
}
