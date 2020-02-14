package org.jboss.as.test.integration.ejb.security.securitydomain.ejb;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJBContext;
import javax.ejb.SessionContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@PermitAll
public abstract class BaseHello implements Hello {

    protected Logger log = Logger.getLogger(this.getClass().getSimpleName());

    private String expectedPrincipalClassName = "org.jboss.security.SimplePrincipal";

    @Resource
    private EJBContext ejbContext;

    @Resource
    private SessionContext stx;

    protected Hello otherEJB;

    protected HashMap<String, Object> data = null;

    protected abstract Hello getOtherEJB();

    protected BaseHello(String expectedPrincipalClassName) {
        if(expectedPrincipalClassName != null && !expectedPrincipalClassName.isEmpty())
            this.expectedPrincipalClassName = expectedPrincipalClassName;
    }

    @Override
    public Info sayHello(Info info) {

        log.info("**** ejb-context =" + ejbContext);
        log.info("**** session-context =" + stx);

        return info.update(this.getClass().getSimpleName(), ejbContext, expectedPrincipalClassName);
    }

    @Override
    public List<String>  sayHelloSeveralTimes(Info info) {

        data = (HashMap<String, Object>) ejbContext.getContextData();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            System.out.println(" key =" + entry.getKey() + "\t value =" + entry.getValue());
        }
        log.info("######## ejb context data =" + data.toString() + "\n\n");

        log.info("**** ejb-context =" + ejbContext);
        log.info("**** session-context =" + stx);

        // update info with this step
        info.update(this.getClass().getSimpleName(), ejbContext, expectedPrincipalClassName);
        // call the other ejb
        info = getOtherEJB().sayHello(info);

        // update info with this step
        info.update(this.getClass().getSimpleName(), ejbContext, expectedPrincipalClassName);

        return info.getPath();
    }
}
