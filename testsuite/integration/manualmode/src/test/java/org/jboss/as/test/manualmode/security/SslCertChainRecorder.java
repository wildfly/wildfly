package org.jboss.as.test.manualmode.security;

import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.security.cert.X509Certificate;

/**
 * Passed into a {@link SSLEngine} to record the peer certificates upon handshake completion. We used a custom
 * {@link TrustManager} called {@code TrustAndStoreTrustManager} for this purpose but it turned out that it delivered
 * inconsistent results on IBM JKD vs. Oracle/OpenJDK.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class SslCertChainRecorder {
    /**
     * The recorded chains
     */
    private final List<X509Certificate[]> chains = new ArrayList<>();

    /**
     * Forget all chains recorded so far.
     */
    public void clear() {
        synchronized (chains) {
            chains.clear();
        }
    }

    /**
     * @param subjDn
     *            the subject DN to search for
     * @return the number of certificates that match the given {@code subjDn}
     */
    public int countCerts(String subjDn) {
        int result = 0;
        synchronized (chains) {
            for (X509Certificate[] chain : chains) {
                for (X509Certificate cert : chain) {
                    if (cert.getSubjectDN().getName().equals(subjDn)) {
                        result++;
                    }
                }
            }
        }
        return result;
    }

    /**
     * @return the number of recorded chains. This typically equals the number of successful SSL handshakes since the
     *         last invocation of {@link #clear()}.
     */
    public int getChainCount() {
        synchronized (chains) {
            return chains.size();
        }
    }

    /**
     * Records the given {@code chain}.
     * @param chain
     */
    public void record(X509Certificate[] chain) {
        synchronized (chains) {
            chains.add(chain);
        }
    }

    @Override
    public String toString() {
        return chains.toString();
    }
}
