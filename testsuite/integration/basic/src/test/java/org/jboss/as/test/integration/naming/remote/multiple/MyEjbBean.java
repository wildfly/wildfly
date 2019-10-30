package org.jboss.as.test.integration.naming.remote.multiple;

import java.util.Properties;

import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.network.NetworkUtils;

@Stateless
public class MyEjbBean implements MyEjb {
    protected MyObject lookup() {
        try {
            Properties env = new Properties();
            String address = System.getProperty("node0", "localhost");
            // format possible IPv6 address
            address = NetworkUtils.formatPossibleIpv6Address(address);
            env.put(Context.PROVIDER_URL, "http-remoting://" + address + ":8080");
            env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
            Context ctx = new InitialContext(env);
            try {
                return (MyObject) ctx.lookup("loc/stub");
            } finally {
                ctx.close();
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public String doIt() {
        MyObject obj = lookup();
        return obj.doIt("Test");
    }
}
