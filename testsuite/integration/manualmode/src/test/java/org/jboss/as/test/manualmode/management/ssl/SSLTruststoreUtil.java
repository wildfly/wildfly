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
package org.jboss.as.test.manualmode.management.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

public class SSLTruststoreUtil {

	public static DefaultHttpClient getHttpClientWithTrustStore(File trustStoreFile, String password) {

		try {
			KeyStore trustStore = KeyStore.getInstance("jks");
			trustStore.load(new FileInputStream(trustStoreFile), password.toCharArray());

			SSLSocketFactory ssf = new AdditionalKeyStoresSSLSocketFactory(trustStore);

			HttpParams params = new BasicHttpParams();

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", SSLManagementInterfaceTestCase.MGMT_PORT, PlainSocketFactory.getSocketFactory()));
			registry.register(new Scheme("https", SSLManagementInterfaceTestCase.MGMT_SECURED_PORT, ssf));

			ClientConnectionManager ccm = new PoolingClientConnectionManager(registry);
			return new DefaultHttpClient(ccm, params);
		} catch (Exception e) {
			return new DefaultHttpClient();
		}
	}

	/**
	 * Allows you to trust certificates from additional KeyStores in addition to
	 * the default KeyStore and all hostnames.
	 */
	static class AdditionalKeyStoresSSLSocketFactory extends SSLSocketFactory {
		protected SSLContext sslContext = SSLContext.getInstance("TLS");

		public AdditionalKeyStoresSSLSocketFactory(KeyStore trustStore) throws NoSuchAlgorithmException,
				KeyManagementException, KeyStoreException, UnrecoverableKeyException {
			
			super(null, null, null, trustStore, null, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			sslContext.init(null, new TrustManager[] { new AdditionalKeyStoresTrustManager(trustStore) }, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
			return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return sslContext.getSocketFactory().createSocket();
		}

		/**
		 * Based on
		 * http://download.oracle.com/javase/1.5.0/docs/guide/security/jsse
		 * /JSSERefGuide.html#X509TrustManager
		 */
		static class AdditionalKeyStoresTrustManager implements X509TrustManager {

			protected ArrayList<X509TrustManager> x509TrustManagers = new ArrayList<X509TrustManager>();

			protected AdditionalKeyStoresTrustManager(KeyStore... additionalkeyStores) {
				final ArrayList<TrustManagerFactory> factories = new ArrayList<TrustManagerFactory>();

				try {
					// The default Trustmanager with default keystore
					final TrustManagerFactory original = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
					original.init((KeyStore) null);
					factories.add(original);

					for (KeyStore keyStore : additionalkeyStores) {
						final TrustManagerFactory additionalCerts = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
						additionalCerts.init(keyStore);
						factories.add(additionalCerts);
					}

				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				/*
				 * Iterate over the returned trustmanagers, and hold on to any
				 * that are X509TrustManagers
				 */
				for (TrustManagerFactory tmf : factories)
					for (TrustManager tm : tmf.getTrustManagers())
						if (tm instanceof X509TrustManager)
							x509TrustManagers.add((X509TrustManager) tm);

				if (x509TrustManagers.size() == 0)
					throw new RuntimeException("Couldn't find any X509TrustManagers");

			}

			/*
			 * Delegate to the default trust manager.
			 */
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				final X509TrustManager defaultX509TrustManager = x509TrustManagers.get(0);
				defaultX509TrustManager.checkClientTrusted(chain, authType);
			}

			/*
			 * Loop over the trustmanagers until we find one that accepts our
			 * server
			 */
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				for (X509TrustManager tm : x509TrustManagers) {
					try {
						tm.checkServerTrusted(chain, authType);
						return;
					} catch (CertificateException e) {
						// ignore
					}
				}
				throw new CertificateException();
			}

			public X509Certificate[] getAcceptedIssuers() {
				final ArrayList<X509Certificate> list = new ArrayList<X509Certificate>();
				for (X509TrustManager tm : x509TrustManagers)
					list.addAll(Arrays.asList(tm.getAcceptedIssuers()));
				return list.toArray(new X509Certificate[list.size()]);
			}
		}

	}
}
