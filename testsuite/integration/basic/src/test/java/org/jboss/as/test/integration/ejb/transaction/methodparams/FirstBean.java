package org.jboss.as.test.integration.ejb.transaction.methodparams;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class FirstBean implements SessionBean {

	
	public void ejbCreate() {
		
	}
	
	public void ejbPassivate() {}
	
	public void ejbActivate() {}
	
	public void ejbRemove() {}
	
	public boolean test(String[] s) {
		try {
			LocalHome localHome = (LocalHome) new InitialContext().lookup("java:comp/env/ejb/Second");
			return localHome.create().test(s);
		} catch (NamingException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean test(String s) {
		try {
			LocalHome localHome = (LocalHome) new InitialContext().lookup("java:comp/env/ejb/Second");
			return localHome.create().test(s);
		} catch (NamingException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean test(int x) {
		try {
			LocalHome localHome = (LocalHome) new InitialContext().lookup("java:comp/env/ejb/Second");
			return localHome.create().test(x);
		} catch (NamingException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void setSessionContext(SessionContext arg0) throws EJBException {
		
	}
	
}
