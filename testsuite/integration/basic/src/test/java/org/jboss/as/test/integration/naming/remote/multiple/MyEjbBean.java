package org.jboss.as.test.integration.naming.remote.multiple;

import java.util.Properties;

import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Stateless
public class MyEjbBean implements MyEjb {
	protected MyObject lookup() {
		try {
			Properties env = new Properties();
			env.put(Context.PROVIDER_URL, "remote://localhost:4447");
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
