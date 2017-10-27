/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.test.manual.elytron.seccontext;

import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_WHOAMI;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.switchIdentity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
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
 * Stateless EJB responsible for calling remote EJB or Servlet.
 *
 * @author Josef Cacek
 */
@Stateless
@RolesAllowed({ "entry", "admin", "no-server2-identity", "authz" })
@DeclareRoles({ "entry", "whoami", "servlet", "admin", "no-server2-identity", "authz" })
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class EntryBean implements Entry {

    @Resource
    private SessionContext context;

    @Override
    public String whoAmI() {
        return context.getCallerPrincipal().getName();
    }

    @Override
    public String[] doubleWhoAmI(CallAnotherBeanInfo info) {
        final Callable<String> callable = () -> {
            return getWhoAmIBean(info.getLookupEjbAppName(), info.getProviderUrl(),
                    info.isStatefullWhoAmI()).getCallerPrincipal().getName();
        };

        return whoAmIAndCall(info, callable);
    }

    public String[] whoAmIAndIllegalStateException(CallAnotherBeanInfo info) {
        final Callable<String> callable = () -> {
            return getWhoAmIBean(info.getLookupEjbAppName(), info.getProviderUrl(),
                    info.isStatefullWhoAmI()).throwIllegalStateException();
        };

        return whoAmIAndCall(info, callable);
    }

    public String[] whoAmIAndServer2Exception(CallAnotherBeanInfo info) {
        final Callable<String> callable = () -> {
            return getWhoAmIBean(info.getLookupEjbAppName(), info.getProviderUrl(),
                    info.isStatefullWhoAmI()).throwServer2Exception();
        };

        return whoAmIAndCall(info, callable);
    }

    @Override
    public String readUrl(String username, String password, ReAuthnType type, final URL url) {
        final Callable<String> callable = () -> {
            URLConnection conn = url.openConnection();
            conn.connect();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                return br.readLine();
            }
        };
        String result = null;
        String firstWho = context.getCallerPrincipal().getName();
        try {
            result = switchIdentity(username, password, callable, type);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            result = sw.toString();
        } finally {
            String secondLocalWho = context.getCallerPrincipal().getName();
            if (!secondLocalWho.equals(firstWho)) {
                throw new IllegalStateException(
                        "Local getCallerPrincipal changed from '" + firstWho + "' to '" + secondLocalWho);
            }
        }
        return result;
    }

    private String[] whoAmIAndCall(CallAnotherBeanInfo info, Callable<String> callable) {
        String[] result = new String[2];
        result[0] = context.getCallerPrincipal().getName();

        try {
            result[1] = switchIdentity(info.getUsername(), info.getPassword(), info.getAuthzName(), callable, info.getType());
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

    private WhoAmI getWhoAmIBean(String ejbAppName, String providerUrl, boolean statefullWhoAmI) throws NamingException {
        return SeccontextUtil.lookup(
                SeccontextUtil.getRemoteEjbName(ejbAppName == null ? WAR_WHOAMI : ejbAppName, "WhoAmIBean",
                        WhoAmI.class.getName(), statefullWhoAmI), providerUrl);
    }
}
