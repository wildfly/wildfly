/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.inject;

import java.util.ArrayList;
import jakarta.interceptor.InvocationContext;
import javax.sql.DataSource;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class XMLInterceptor {
    MySession2 session2;
    DataSource ds;

    MySession2Local session2Method;
    DataSource dsMethod;

    public void setSession2Method(MySession2Local session2Method) {
        this.session2Method = session2Method;
    }

    public void setDsMethod(DataSource dsMethod) {
        this.dsMethod = dsMethod;
    }

    public Object intercept(InvocationContext ctx) throws Exception {
        session2.doit();
        if (ds == null) { throw new RuntimeException("ds was null"); }

        session2Method.doit();
        if (dsMethod == null) { throw new RuntimeException("ds was null"); }
        ArrayList list = new ArrayList();
        list.add("MyInterceptor");
        return list;
    }

}
