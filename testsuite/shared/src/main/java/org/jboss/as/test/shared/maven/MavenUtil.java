/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
 *
 */
package org.jboss.as.test.shared.maven;

import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.repository.internal.SnapshotMetadataGeneratorFactory;
import org.apache.maven.repository.internal.VersionsMetadataGeneratorFactory;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.MetadataGeneratorFactory;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.filter.ExclusionsDependencyFilter;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.VersionScheme;
import org.jboss.logging.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Tomaz Cerar
 */
public class MavenUtil {

    private static final String SYS_PROP_MAVEN_REPO_URLS = "org.wildfly.test.maven.repository.urls";
    private final RepositorySystem REPOSITORY_SYSTEM;
    private final List<RemoteRepository> remoteRepositories;

    private static final String PROXY_HTTP_PREFIX = "http.";
    private static final String PROXY_HTTPS_PREFIX = "https.";
    private static final String PROXY_HOST = "proxyHost";
    private static final String PROXY_PORT = "proxyPort";

    private static MavenSettings mavenSettings = MavenSettings.getSettings();
    private DefaultRepositorySystemSession session;
    private static final Logger log = Logger.getLogger("maven.downloader");

    private MavenUtil(RepositorySystem repositorySystem, List<RemoteRepository> remoteRepositories) {
        this.REPOSITORY_SYSTEM = repositorySystem;
        this.remoteRepositories = remoteRepositories;
    }

    public static MavenUtil create(boolean useEapRepository) {

        return new MavenUtil(newRepositorySystem(), createRemoteRepositories(useEapRepository));
    }

    public URL createMavenGavURL(String artifactGav) throws MalformedURLException {
        Artifact artifact = new DefaultArtifact(artifactGav);
        if (artifact.getVersion() == null) {
            throw new IllegalArgumentException("Null version");
        }

        VersionScheme versionScheme = new GenericVersionScheme();
        try {
            versionScheme.parseVersion(artifact.getVersion());
        } catch (InvalidVersionSpecificationException e) {
            throw new IllegalArgumentException(e);
        }

        try {
            versionScheme.parseVersionRange(artifact.getVersion());
            throw new IllegalArgumentException(artifact.getVersion() + " is a version range. A specific version is needed");
        } catch (InvalidVersionSpecificationException expected) {

        }

        RepositorySystemSession session = newRepositorySystemSession();

        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        for (RemoteRepository remoteRepo : remoteRepositories) {
            artifactRequest.addRepository(remoteRepo);
        }

        ArtifactResult artifactResult;
        try {
            artifactResult = REPOSITORY_SYSTEM.resolveArtifact(session, artifactRequest);
        } catch (ArtifactResolutionException e) {
            throw new RuntimeException(e);
        }


        File file = artifactResult.getArtifact().getFile().getAbsoluteFile();
        return file.toURI().toURL();
    }

    public List<URL> createMavenGavRecursiveURLs(String artifactGav, String... excludes) throws MalformedURLException, DependencyCollectionException, DependencyResolutionException {
        Artifact artifact = new DefaultArtifact(artifactGav);
        if (artifact.getVersion() == null) {
            throw new IllegalArgumentException("Null version");
        }

        VersionScheme versionScheme = new GenericVersionScheme();
        try {
            versionScheme.parseVersion(artifact.getVersion());
        } catch (InvalidVersionSpecificationException e) {
            throw new IllegalArgumentException(e);
        }

        try {
            versionScheme.parseVersionRange(artifact.getVersion());
            throw new IllegalArgumentException(artifact.getVersion() + " is a version range. A specific version is needed");
        } catch (InvalidVersionSpecificationException expected) {

        }

        RepositorySystemSession session = newRepositorySystemSession();

        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        for (RemoteRepository remoteRepo : remoteRepositories) {
            artifactRequest.addRepository(remoteRepo);
        }

        ArtifactResult artifactResult;
        try {
            artifactResult = REPOSITORY_SYSTEM.resolveArtifact(session, artifactRequest);
        } catch (ArtifactResolutionException e) {
            throw new RuntimeException(e);
        }

        List<URL> urls = new ArrayList<>();
        urls.add(artifactToUrl(artifactResult.getArtifact()));

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, "compile"));
        for (RemoteRepository remoteRepo : remoteRepositories) {
            collectRequest.addRepository(remoteRepo);
        }

        DependencyNode node = REPOSITORY_SYSTEM.collectDependencies(session, collectRequest).getRoot();
        DependencyFilter filter = new ExclusionsDependencyFilter(Arrays.asList(excludes));
        DependencyRequest dependencyRequest = new DependencyRequest(node, filter);

        REPOSITORY_SYSTEM.resolveDependencies(session, dependencyRequest);

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        node.accept(nlg);
        for (Artifact cur : nlg.getArtifacts(false)) {
            urls.add(artifactToUrl(cur));
        }

        log.debug("--------------------");
        log.debug(nlg.getClassPath());
        log.debug("--------------------");

        return urls;
    }

    private static Integer getProxyPort(String systemProperty) {
        String port = System.getProperty(systemProperty);
        if (port != null && !port.isEmpty()) {
            try {
                Integer intPort = Integer.parseInt(port);
                return intPort;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static List<RemoteRepository> createRemoteRepositories(boolean useEapRepository) {
        // prepare proxy
        String httpProxyHost = System.getProperty(String.format("%s%s", PROXY_HTTP_PREFIX, PROXY_HOST));
        String httpsProxyHost = System.getProperty(String.format("%s%s", PROXY_HTTPS_PREFIX, PROXY_HOST));
        Integer httpProxyPort = getProxyPort(String.format("%s%s", PROXY_HTTP_PREFIX, PROXY_PORT));
        Integer httpsProxyPort = getProxyPort(String.format("%s%s", PROXY_HTTPS_PREFIX, PROXY_PORT));
        Proxy httpProxy = null;
        Proxy httpsProxy = null;
        if (httpProxyHost != null && httpProxyPort != null && !httpProxyHost.isEmpty()) {
            httpProxy = new Proxy(Proxy.TYPE_HTTP, httpProxyHost, httpProxyPort);
        }
        if (httpsProxyHost != null && httpsProxyPort != null && !httpsProxyHost.isEmpty()) {
            httpsProxy = new Proxy(Proxy.TYPE_HTTPS, httpsProxyHost, httpsProxyPort);
        }

        String remoteReposFromSysProp = System.getProperty(SYS_PROP_MAVEN_REPO_URLS);
        List<RemoteRepository> remoteRepositories = new ArrayList<>();
        if (remoteReposFromSysProp == null || remoteReposFromSysProp.trim().length() == 0 || remoteReposFromSysProp.startsWith("${")) {
            if (useEapRepository) {
                RemoteRepository.Builder repository = new RemoteRepository.Builder("product-repository", "default", "https://maven.repository.redhat.com/ga/");
                if (httpsProxy != null) {
                    repository.setProxy(httpsProxy);
                }
                remoteRepositories.add(repository.build());
            }
            //always add jboss developer repository
            RemoteRepository.Builder repository = new RemoteRepository.Builder("jboss-developer", "default", "http://repository.jboss.org/nexus/content/groups/developer/");
            if (httpProxy != null) {
                repository.setProxy(httpProxy);
            }
            remoteRepositories.add(repository.build());
            //add repos from users settings.xml
            List<String> remoteRepositories1 = mavenSettings.getRemoteRepositories();
            for (int i = 0; i < remoteRepositories1.size(); i++) {
                String repo = remoteRepositories1.get(i);
                RemoteRepository.Builder myRepo = new RemoteRepository.Builder("repo-" +i, "default", repo);
                if (httpProxy != null && repo.startsWith("http")) {
                    myRepo.setProxy(httpProxy);
                }
                if (httpsProxy != null && repo.startsWith("https")) {
                    myRepo.setProxy(httpsProxy);
                }
                remoteRepositories.add(myRepo.build());
            }

        } else {
            int i = 0;
            for (String repoUrl : remoteReposFromSysProp.split(",")) {
                //remoteRepositories.add(new RemoteRepository("repo" + i, "default", repoUrl.trim()));
                repoUrl = repoUrl.trim();
                RemoteRepository.Builder repository = new RemoteRepository.Builder("repo" + i, "default", repoUrl);
                if (repoUrl.startsWith("https:") && httpsProxy != null) {
                    repository.setProxy(httpsProxy);
                }
                if (repoUrl.startsWith("http:") && httpProxy != null) {
                    repository.setProxy(httpProxy);
                }
                remoteRepositories.add(repository.build());
                i++;
            }
        }
        return remoteRepositories;
    }

    private RepositorySystemSession newRepositorySystemSession() {
        if (this.session != null){
            return this.session;
        }
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();


        LocalRepository localRepo = new LocalRepository(mavenSettings.getLocalRepository().toString());
        session.setLocalRepositoryManager(REPOSITORY_SYSTEM.newLocalRepositoryManager(session, localRepo));

        //Copy these from the aether demo if they are nice to have
        session.setTransferListener(new ConsoleTransferListener());
        session.setRepositoryListener(new ConsoleRepositoryListener());
        session.setTransferListener(new AbstractTransferListener() {
            @Override
            public void transferFailed(TransferEvent event) {
                super.transferFailed(event);
            }
        });
        this.session = session;
        return session;
    }

    private static URL artifactToUrl(Artifact artifact) throws MalformedURLException {
        return artifact.getFile().toURI().toURL();
    }

    static RepositorySystem newRepositorySystem() {
            /*
             * Aether's components implement
             * org.sonatype.aether.spi.locator.Service to ease manual wiring and
             * using the prepopulated DefaultServiceLocator, we only need to
             * register the repository connector factories.
             */

        DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.addService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);
        locator.addService(VersionResolver.class, DefaultVersionResolver.class);
        locator.addService(VersionRangeResolver.class, DefaultVersionRangeResolver.class);
        locator.addService(MetadataGeneratorFactory.class, SnapshotMetadataGeneratorFactory.class);
        locator.addService(MetadataGeneratorFactory.class, VersionsMetadataGeneratorFactory.class);
        locator.setErrorHandler(new MyErrorHandler());

        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        //locator.addService(TransporterFactory.class, WagonTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);


        return locator.getService(RepositorySystem.class);
    }

    static class MyErrorHandler extends DefaultServiceLocator.ErrorHandler {
        @Override
        public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
            log.error("Could not create type: " + type + " impl: " + impl, exception);
        }
    }

    private static class ConsoleRepositoryListener extends AbstractRepositoryListener {

        ConsoleRepositoryListener() {
        }


        public void artifactDeployed(RepositoryEvent event) {
            log.debug("Deployed " + event.getArtifact() + " to " + event.getRepository());
        }

        public void artifactDeploying(RepositoryEvent event) {
            log.debug("Deploying " + event.getArtifact() + " to " + event.getRepository());
        }

        public void artifactDescriptorInvalid(RepositoryEvent event) {
            log.debug("Invalid artifact descriptor for " + event.getArtifact() + ": " ,event.getException());
        }

        public void artifactDescriptorMissing(RepositoryEvent event) {
            log.debug("Missing artifact descriptor for " + event.getArtifact());
        }

        public void artifactInstalled(RepositoryEvent event) {
            log.debug("Installed " + event.getArtifact() + " to " + event.getFile());
        }

        public void artifactInstalling(RepositoryEvent event) {
            log.debug("Installing " + event.getArtifact() + " to " + event.getFile());
        }

        public void artifactResolved(RepositoryEvent event) {
            log.debug("Resolved artifact " + event.getArtifact() + " from " + event.getRepository());
        }

        public void artifactDownloading(RepositoryEvent event) {
            log.debug("Downloading artifact " + event.getArtifact() + " from " + event.getRepository());
        }

        public void artifactDownloaded(RepositoryEvent event) {
            log.debug("Downloaded artifact " + event.getArtifact() + " from " + event.getRepository());
        }

        public void artifactResolving(RepositoryEvent event) {
            log.debug("Resolving artifact " + event.getArtifact());
        }

        public void metadataDeployed(RepositoryEvent event) {
            log.debug("Deployed " + event.getMetadata() + " to " + event.getRepository());
        }

        public void metadataDeploying(RepositoryEvent event) {
            log.debug("Deploying " + event.getMetadata() + " to " + event.getRepository());
        }

        public void metadataInstalled(RepositoryEvent event) {
            log.debug("Installed " + event.getMetadata() + " to " + event.getFile());
        }

        public void metadataInstalling(RepositoryEvent event) {
            log.debug("Installing " + event.getMetadata() + " to " + event.getFile());
        }

        public void metadataInvalid(RepositoryEvent event) {
            log.debug("Invalid metadata " + event.getMetadata());
        }

        public void metadataResolved(RepositoryEvent event) {
            log.debug("Resolved metadata " + event.getMetadata() + " from " + event.getRepository());
        }

        public void metadataResolving(RepositoryEvent event) {
            log.debug("Resolving metadata " + event.getMetadata() + " from " + event.getRepository());
        }

    }

    private static class ConsoleTransferListener extends AbstractTransferListener {

        private Map<TransferResource, Long> downloads = new ConcurrentHashMap<>();
        private int lastLength;

        ConsoleTransferListener() {

        }


        @Override
        public void transferInitiated(TransferEvent event) {
            String message = event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading";

            log.debug(message + ": " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
        }

        @Override
        public void transferProgressed(TransferEvent event) {
            TransferResource resource = event.getResource();
            downloads.put(resource, Long.valueOf(event.getTransferredBytes()));

            StringBuilder buffer = new StringBuilder(64);

            for (Map.Entry<TransferResource, Long> entry : downloads.entrySet()) {
                long total = entry.getKey().getContentLength();
                long complete = entry.getValue().longValue();

                buffer.append(getStatus(complete, total)).append("  ");
            }

            int pad = lastLength - buffer.length();
            lastLength = buffer.length();
            pad(buffer, pad);
            buffer.append('\r');
            log.trace(buffer);
        }

        private String getStatus(long complete, long total) {
            if (total >= 1024) {
                return toKB(complete) + "/" + toKB(total) + " KB ";
            } else if (total >= 0) {
                return complete + "/" + total + " B ";
            } else if (complete >= 1024) {
                return toKB(complete) + " KB ";
            } else {
                return complete + " B ";
            }
        }

        private void pad(StringBuilder buffer, int spaces) {
            String block = "                                        ";
            while (spaces > 0) {
                int n = Math.min(spaces, block.length());
                buffer.append(block, 0, n);
                spaces -= n;
            }
        }

        @Override
        public void transferSucceeded(TransferEvent event) {
            transferCompleted(event);

            TransferResource resource = event.getResource();
            long contentLength = event.getTransferredBytes();
            if (contentLength >= 0) {
                String type = (event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploaded" : "Downloaded");
                String len = contentLength >= 1024 ? toKB(contentLength) + " KB" : contentLength + " B";

                String throughput = "";
                long duration = System.currentTimeMillis() - resource.getTransferStartTime();
                if (duration > 0) {
                    DecimalFormat format = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH));
                    double kbPerSec = (contentLength / 1024.0) / (duration / 1000.0);
                    throughput = " at " + format.format(kbPerSec) + " KB/sec";
                }

                log.debug(type + ": " + resource.getRepositoryUrl() + resource.getResourceName() + " (" + len + throughput
                        + ")");
            }
        }

        @Override
        public void transferFailed(TransferEvent event) {
            transferCompleted(event);
            log.warn(event.getException());

        }

        private void transferCompleted(TransferEvent event) {
            downloads.remove(event.getResource());

            StringBuilder buffer = new StringBuilder(64);
            pad(buffer, lastLength);
            buffer.append('\r');
            log.trace(buffer);

        }

        public void transferCorrupted(TransferEvent event) {
            log.debug(event.getException());
        }

        long toKB(long bytes) {
            return (bytes + 1023) / 1024;
        }

    }
}
