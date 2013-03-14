package org.jboss.as.test.integration.ejb.transaction.methodparams;

import javax.ejb.EJBLocalObject;

public interface Local extends EJBLocalObject {

	public boolean test(String[] s);

	public boolean test(String s);
	
	public boolean test(int x);

}
