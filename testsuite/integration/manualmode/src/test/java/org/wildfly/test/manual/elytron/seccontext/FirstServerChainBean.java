/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.seccontext;

import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.switchIdentity;
import static org.wildfly.test.manual.elytron.seccontext.ServerChainSecurityContextPropagationTestCase.JAR_ENTRY_EJB_SERVER_CHAIN;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Callable;
import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
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
