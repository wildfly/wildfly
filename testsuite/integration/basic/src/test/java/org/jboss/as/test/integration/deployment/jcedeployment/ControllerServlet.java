/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.jcedeployment;

import org.jboss.as.test.integration.deployment.jcedeployment.provider.DummyProvider;
import org.jboss.logging.Logger;

import javax.crypto.Cipher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
