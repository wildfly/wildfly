package org.jboss.as.test.integration.ejb.transaction.methodparams;

import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class SecondBean implements SessionBean {

	
	public void ejbCreate() {
		
	}
	
	public void ejbPassivate() {}
	
	public void ejbActivate() {}
	
	public void ejbRemove() {}
	
	public boolean test(String[] s) {
		System.out.println(">> test(String[])");
		return true;
	}

	public boolean test(String s) {
		System.out.println(">> test(String)");
		return true;
	}

	public boolean test(int x) {
		System.out.println(">> test(int)");
		return true;
	}

	public void setSessionContext(SessionContext arg0) throws EJBException {
		
	}
	
}
