package org.jboss.as.test.integration.naming.remote.multiple;

import javax.ejb.Remote;

@Remote
public interface MyEjb {
	public abstract String doIt();
}
