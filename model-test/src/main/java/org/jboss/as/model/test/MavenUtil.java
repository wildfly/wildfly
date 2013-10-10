/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.model.test;

import java.io.File;
import java.io.PrintStream;
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

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class MavenUtil {

    private static final String AETHER_API_NAME = File.separatorChar == '/' ? "/org/eclipse/aether/aether-api/" : "\\org\\eclipse\\aether\\aether-api\\";

    private final RepositorySystem REPOSITORY_SYSTEM;
    private final List<RemoteRepository> remoteRepositories;

    private static String mavenRepository;

    private MavenUtil(RepositorySystem repositorySystem, List<RemoteRepository> remoteRepositories) {
        this.REPOSITORY_SYSTEM = repositorySystem;
        this.remoteRepositories = remoteRepositories;
    }

    static MavenUtil create(boolean useEapRepository) {
        return new MavenUtil(newRepositorySystem(), createRemoteRepositories(useEapRepository));
    }

    URL createMavenGavURL(String artifactGav) throws MalformedURLException {
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
        System.out.println(file);
        return file.toURI().toURL();
    }

    List<URL> createMavenGavRecursiveURLs(String artifactGav, String... excludes) throws MalformedURLException, DependencyCollectionException, DependencyResolutionException {
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
        //TODO add more remote repositories - especially the JBoss one

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

        List<URL> urls = new ArrayList<URL>();
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

        System.out.println("--------------------");
        System.out.println(nlg.getClassPath());
        System.out.println("--------------------");

        return urls;
    }

    private static List<RemoteRepository> createRemoteRepositories(boolean useEapRepository) {
        String remoteReposFromSysProp = System.getProperty(ChildFirstClassLoaderBuilder.MAVEN_REPOSITORY_URLS);
        List<RemoteRepository> remoteRepositories = new ArrayList<RemoteRepository>();
        if (remoteReposFromSysProp == null || remoteReposFromSysProp.trim().length() == 0 || remoteReposFromSysProp.startsWith("${")) {
            if (useEapRepository) {
                remoteRepositories.add(new RemoteRepository.Builder("jboss-product-repository", "default", "http://download.lab.bos.redhat.com/brewroot/repos/jb-eap-6-rhel-6-build/latest/maven/").build());
            } else {
                remoteRepositories.add(new RemoteRepository.Builder("jboss-developer", "default", "http://repository.jboss.org/nexus/content/groups/developer/").build());
            }
        } else {
            int i = 0;
            for (String repoUrl : remoteReposFromSysProp.split(",")) {
                //remoteRepositories.add(new RemoteRepository("repo" + i, "default", repoUrl.trim()));
                remoteRepositories.add(new RemoteRepository.Builder("repo" + i, "default", repoUrl.trim()).build());
                i++;
            }
        }
        return remoteRepositories;
    }

    private RepositorySystemSession newRepositorySystemSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        //TODO make local repo more pluggable?
        LocalRepository localRepo = new LocalRepository(determineLocalMavenRepositoryHack());
        session.setLocalRepositoryManager(REPOSITORY_SYSTEM.newLocalRepositoryManager(session, localRepo));

        //Copy these from the aether demo if they are nice to have
        session.setTransferListener(new ConsoleTransferListener());
        session.setRepositoryListener(new ConsoleRepositoryListener());

        return session;
    }

    private static String determineLocalMavenRepositoryHack() {
        //TODO Uuuugly :-)
        if (mavenRepository == null) {
            String classPath = System.getProperty("java.class.path");
            int end = classPath.indexOf(AETHER_API_NAME) + 1;
            int start = classPath.lastIndexOf(File.pathSeparatorChar, end) + 1;
            String localRepositoryRoot = classPath.substring(start, end);
            mavenRepository = localRepositoryRoot;
        }
        return mavenRepository;
    }

    private static URL artifactToUrl(Artifact artifact) throws MalformedURLException {
        return artifact.getFile().toURI().toURL();
    }

    public static RepositorySystem newRepositorySystem() {
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

    protected static class MyErrorHandler extends DefaultServiceLocator.ErrorHandler {
        @Override
        public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
            System.out.println("Could not create type: " + type + " impl: " + impl);
            exception.printStackTrace();
        }
    }

    private static class ConsoleRepositoryListener extends AbstractRepositoryListener {

        private PrintStream out;

        public ConsoleRepositoryListener() {
            this(null);
        }

        public ConsoleRepositoryListener(PrintStream out) {
            this.out = (out != null) ? out : System.out;
        }

        public void artifactDeployed(RepositoryEvent event) {
            out.println("Deployed " + event.getArtifact() + " to " + event.getRepository());
        }

        public void artifactDeploying(RepositoryEvent event) {
            out.println("Deploying " + event.getArtifact() + " to " + event.getRepository());
        }

        public void artifactDescriptorInvalid(RepositoryEvent event) {
            out.println("Invalid artifact descriptor for " + event.getArtifact() + ": " + event.getException().getMessage());
        }

        public void artifactDescriptorMissing(RepositoryEvent event) {
            out.println("Missing artifact descriptor for " + event.getArtifact());
        }

        public void artifactInstalled(RepositoryEvent event) {
            out.println("Installed " + event.getArtifact() + " to " + event.getFile());
        }

        public void artifactInstalling(RepositoryEvent event) {
            out.println("Installing " + event.getArtifact() + " to " + event.getFile());
        }

        public void artifactResolved(RepositoryEvent event) {
            out.println("Resolved artifact " + event.getArtifact() + " from " + event.getRepository());
        }

        public void artifactDownloading(RepositoryEvent event) {
            out.println("Downloading artifact " + event.getArtifact() + " from " + event.getRepository());
        }

        public void artifactDownloaded(RepositoryEvent event) {
            out.println("Downloaded artifact " + event.getArtifact() + " from " + event.getRepository());
        }

        public void artifactResolving(RepositoryEvent event) {
            out.println("Resolving artifact " + event.getArtifact());
        }

        public void metadataDeployed(RepositoryEvent event) {
            out.println("Deployed " + event.getMetadata() + " to " + event.getRepository());
        }

        public void metadataDeploying(RepositoryEvent event) {
            out.println("Deploying " + event.getMetadata() + " to " + event.getRepository());
        }

        public void metadataInstalled(RepositoryEvent event) {
            out.println("Installed " + event.getMetadata() + " to " + event.getFile());
        }

        public void metadataInstalling(RepositoryEvent event) {
            out.println("Installing " + event.getMetadata() + " to " + event.getFile());
        }

        public void metadataInvalid(RepositoryEvent event) {
            out.println("Invalid metadata " + event.getMetadata());
        }

        public void metadataResolved(RepositoryEvent event) {
            out.println("Resolved metadata " + event.getMetadata() + " from " + event.getRepository());
        }

        public void metadataResolving(RepositoryEvent event) {
            out.println("Resolving metadata " + event.getMetadata() + " from " + event.getRepository());
        }

    }

    private static class ConsoleTransferListener extends AbstractTransferListener {

        private PrintStream out;
        private Map<TransferResource, Long> downloads = new ConcurrentHashMap<TransferResource, Long>();
        private int lastLength;

        public ConsoleTransferListener() {
            this(null);
        }

        public ConsoleTransferListener(PrintStream out) {
            this.out = (out != null) ? out : System.out;
        }

        @Override
        public void transferInitiated(TransferEvent event) {
            String message = event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading";

            out.println(message + ": " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
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

            out.print(buffer);
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

                out.println(type + ": " + resource.getRepositoryUrl() + resource.getResourceName() + " (" + len + throughput
                        + ")");
            }
        }

        @Override
        public void transferFailed(TransferEvent event) {
            transferCompleted(event);

            event.getException().printStackTrace(out);
        }

        private void transferCompleted(TransferEvent event) {
            downloads.remove(event.getResource());

            StringBuilder buffer = new StringBuilder(64);
            pad(buffer, lastLength);
            buffer.append('\r');
            out.print(buffer);
        }

        public void transferCorrupted(TransferEvent event) {
            event.getException().printStackTrace(out);
        }

        protected long toKB(long bytes) {
            return (bytes + 1023) / 1024;
        }

    }
}
