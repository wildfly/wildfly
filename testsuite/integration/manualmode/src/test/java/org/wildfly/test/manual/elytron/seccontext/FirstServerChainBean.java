/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.test.manual.elytron.seccontext;

import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.switchIdentity;
import static org.wildfly.test.manual.elytron.seccontext.ServerChainSecurityContextPropagationTestCase.JAR_ENTRY_EJB_SERVER_CHAIN;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Callable;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.NamingException;

/**
 * Stateless EJB responsible for calling remote EntryBean.
 *
 * @author olukas
 */
@Stateless
@RolesAllowed({"entry", "admin", "no-server2-identity"})
@DeclareRoles({"entry", "whoami", "servlet", "admin", "no-server2-identity"})
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class FirstServerChainBean implements FirstServerChain {

    @Resource
    private SessionContext context;

    @Override
    public String whoAmI() {
        return context.getCallerPrincipal().getName();
    }

    @Override
    public String[] tripleWhoAmI(CallAnotherBeanInfo firstBeanInfo, CallAnotherBeanInfo secondBeanInfo) {
        String[] result = new String[3];
        result[0] = context.getCallerPrincipal().getName();

        final Callable<String[]> callable = () -> {
            return getEntryBean(firstBeanInfo.getLookupEjbAppName(), firstBeanInfo.getProviderUrl(),
                    firstBeanInfo.isStatefullWhoAmI()).doubleWhoAmI(secondBeanInfo);
        };
        try {
            String[] resultFromAnotherBeans = switchIdentity(firstBeanInfo.getUsername(), firstBeanInfo.getPassword(), callable,
                    firstBeanInfo.getType());
            result[1] = resultFromAnotherBeans[0];
            result[2] = resultFromAnotherBeans[1];
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            result[1] = sw.toString();
        } finally {
            String secondLocalWho = context.getCallerPrincipal().getName();
            if (!secondLocalWho.equals(result[0])) {
                throw new IllegalStateException(
                        "Local getCallerPrincipal changed from '" + result[0] + "' to '" + secondLocalWho);
            }
        }
        return result;
    }

    private Entry getEntryBean(String ejbAppName, String providerUrl, boolean statefullWhoAmI) throws NamingException {
        return SeccontextUtil.lookup(
                SeccontextUtil.getRemoteEjbName(ejbAppName == null ? JAR_ENTRY_EJB_SERVER_CHAIN : ejbAppName, "EntryBean",
                        Entry.class.getName(), statefullWhoAmI), providerUrl);
    }

}
