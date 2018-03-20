/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.deployment.jcedeployment;

import org.jboss.as.test.integration.deployment.jcedeployment.provider.DummyProvider;
import org.jboss.logging.Logger;

import javax.crypto.Cipher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.KeyStore;
import java.security.ProtectionDomain;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;

/**
 * This servlet requires Oracle JDK 7 as it uses javax.crypto.JarVerifier
 * and sun.security.validator.SimpleValidator in the init method.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@WebServlet(name = "ControllerServlet", urlPatterns = {"/controller"})
public class ControllerServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(ControllerServlet.class);

    public void init(ServletConfig config) throws ServletException {
        try {
            final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream in = Files.newInputStream(Paths.get("target/jcetest.keystore"))){
                keyStore.load(in, "password".toCharArray());
            }
            final X509Certificate testCertificate = (X509Certificate) keyStore.getCertificate("test");
            assert testCertificate != null;

            // the three musketeers who are guarding the crown are hardcoded in jse.jar (JarVerifier)
            final Object validator = get("javax.crypto.JarVerifier", "providerValidator", Object.class);    // sun.security.validator.SimpleValidator

            get(validator, "trustedX500Principals", Map.class).put(testCertificate.getIssuerX500Principal(), Arrays.asList(testCertificate));
        } catch (ClassNotFoundException e) {
            throw new ServletException("This requires being run on Oracle JDK 7.", e);
        } catch (Exception e) {
            throw new ServletException("Cannot install the certificate to the validator.", e);
        }

        java.security.Security.addProvider(new DummyProvider());
    }

    private static <T> T get(final Object obj, final String fieldName, Class<T> type) throws NoSuchFieldException, IllegalAccessException {
        final Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(obj));
    }

    private static <T> T get(final String className, final String fieldName, Class<T> type) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        final Class<?> cls = Class.forName(className);
        final Field field = cls.getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(null));
    }

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Provider[] providers = Security.getProviders();

            for (int i = 0; i < providers.length; i++) {

                final Provider provider = providers[i];

                log.debug("Provider name: " + provider.getName());
                log.debug("Provider information: " + provider.getInfo());
                log.debug("Provider version: " + provider.getVersion());

                URL url = null;
                ProtectionDomain pd = provider.getClass().getProtectionDomain();
                if (pd != null) {
                    CodeSource cs = pd.getCodeSource();
                    if (cs != null) {
                        url = cs.getLocation();
                    }
                }
                log.debug("Provider code base: " + url);
            }

            Cipher.getInstance("DummyAlg/DummyMode/DummyPadding", "DP");

            response.getWriter().write("ok");
            response.getWriter().close();

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

}
