package org.jboss.as.test.integration.ejb.transaction.methodparams;

import javax.ejb.EJBLocalHome;

public interface LocalHome extends EJBLocalHome {

	Local create();
	
}
