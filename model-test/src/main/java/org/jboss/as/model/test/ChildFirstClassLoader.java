/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
import java.net.URLClassLoader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.sonatype.aether.AbstractRepositoryListener;
import org.sonatype.aether.RepositoryEvent;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.transfer.AbstractTransferListener;
import org.sonatype.aether.transfer.TransferEvent;
import org.sonatype.aether.transfer.TransferResource;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;
import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.InvalidVersionSpecificationException;
import org.sonatype.aether.version.VersionScheme;

/**
 * Internal use only.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ChildFirstClassLoader extends URLClassLoader {

    private static final RepositorySystem REPOSITORY_SYSTEM = newRepositorySystem();
    private static final String AETHER_API_NAME = File.separatorChar == '/' ? "/org/sonatype/aether/aether-api/" : "\\org\\sonatype\\aether\\aether-api\\";

    private final ClassLoader parent;
    private final List<Pattern> parentFirst;
    private final List<Pattern> childFirst;


    ChildFirstClassLoader(ClassLoader parent, List<Pattern> parentFirst, List<Pattern> childFirst, URL...urls) {
        super(urls, parent);
        assert parent != null : "Null parent";
        assert parentFirst != null : "Null parent first";
        assert childFirst != null : "Null child first";
        this.parent = parent;
        this.childFirst = childFirst;
        this.parentFirst = parentFirst;
    }

    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

        if (loadFromParentOnly(name)) {
            return parent.loadClass(name);
        }

        // First, check if the class has already been loaded
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                c = findClass(name);
            } catch (ClassNotFoundException e) {

            }
            if (c == null) {
                c = parent.loadClass(name);
            }
            if (c == null) {
                findClass(name);
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }


    @Override
    public URL getResource(String name) {
        URL url = findResource(name);
        if (url != null) {
            return url;
        }
        return super.getResource(name);
    }

    private boolean loadFromParentOnly(String className) {
        boolean parent = false;
        for (Pattern pattern : parentFirst) {
            if (pattern.matcher(className).matches()) {
                parent = true;
                break;
            }
        }

        if (parent) {
            for (Pattern pattern : childFirst) {
                if (pattern.matcher(className).matches()) {
                    return false;
                }
            }
        }
        return parent;
    }

    static URL createSimpleResourceURL(String resource) throws MalformedURLException {
        URL url = ChildFirstClassLoader.class.getResource(resource);
        if (url == null) {
            ClassLoader cl = ChildFirstClassLoader.class.getClassLoader();
            if (cl == null) {
                cl = ClassLoader.getSystemClassLoader();
            }
            url = cl.getResource(resource);
            if (url == null) {
                File file = new File(resource);
                if (file.exists()) {
                    url = file.toURI().toURL();
                }
            }
        }
        if (url == null) {
            throw new IllegalArgumentException("Could not find resource " + resource);
        }
        return url;
    }

    static URL createMavenGavURL(String artifactGav) throws MalformedURLException {
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
        RemoteRepository central = newCentralRepository();
        //TODO add more remote repositories - especially the JBoss one

        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        artifactRequest.addRepository(central);

        ArtifactResult artifactResult;
        try {
            artifactResult = REPOSITORY_SYSTEM.resolveArtifact(session, artifactRequest);
        } catch(ArtifactResolutionException e) {
            throw new RuntimeException(e);
        }

        File file = artifactResult.getArtifact().getFile().getAbsoluteFile();
        System.out.println(file);
        return file.toURI().toURL();
    }

    static List<URL> createMavenGavRecursiveURLs(String artifactGav) throws MalformedURLException, DependencyCollectionException, DependencyResolutionException {
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
        RemoteRepository central = newCentralRepository();
        //TODO add more remote repositories - especially the JBoss one

        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        artifactRequest.addRepository(central);

        ArtifactResult artifactResult;
        try {
            artifactResult = REPOSITORY_SYSTEM.resolveArtifact(session, artifactRequest);
        } catch(ArtifactResolutionException e) {
            throw new RuntimeException(e);
        }

        List<URL> urls = new ArrayList<URL>();
        urls.add(artifactToUrl(artifactResult.getArtifact()));

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, "compile" ));
        collectRequest.addRepository( central );
        DependencyNode node = REPOSITORY_SYSTEM.collectDependencies( session, collectRequest ).getRoot();
        DependencyRequest dependencyRequest = new DependencyRequest( node, null );

        REPOSITORY_SYSTEM.resolveDependencies( session, dependencyRequest  );

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        node.accept( nlg );
        for (Artifact cur : nlg.getArtifacts(false)) {
            urls.add(artifactToUrl(cur));
            System.out.println("--> " + artifactToUrl(cur));
        }
        return null;
    }

    private static RemoteRepository newCentralRepository() {
        return new RemoteRepository("jboss-developer", "default", "http://repository.jboss.org/nexus/content/groups/developer/");
    }

    private static RepositorySystemSession newRepositorySystemSession() {

        MavenRepositorySystemSession session = new MavenRepositorySystemSession();

        //TODO make local repo more pluggable?
        LocalRepository localRepo = new LocalRepository(determineLocalMavenRepositoryHack());
        session.setLocalRepositoryManager( REPOSITORY_SYSTEM.newLocalRepositoryManager(localRepo));

        //Copy these from the aether demo if they are nice to have
        session.setTransferListener(new ConsoleTransferListener());
        session.setRepositoryListener(new ConsoleRepositoryListener());

        // uncomment to generate dirty trees
        // session.setDependencyGraphTransformer( null );

        return session;
    }

    private static String determineLocalMavenRepositoryHack() {
        //TODO Uuuugly :-)
        String classPath = System.getProperty("java.class.path");
        int end = classPath.indexOf(AETHER_API_NAME) + 1;
        int start = classPath.lastIndexOf(File.pathSeparatorChar, end) + 1;
        String localRepositoryRoot = classPath.substring(start, end);
        return localRepositoryRoot;
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
        locator.addService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
        locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
        locator.setServices(WagonProvider.class, new ManualWagonProvider());

        return locator.getService(RepositorySystem.class);
    }

    private static class ManualWagonProvider implements WagonProvider {

        public Wagon lookup(String roleHint) throws Exception {
            if ("http".equals(roleHint)) {
                return new LightweightHttpWagon();
            }
            return null;
        }

        public void release(Wagon wagon) {

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

    public static void main(String[] args) throws Exception {
        ChildFirstClassLoader.createMavenGavRecursiveURLs("org.jboss.as:jboss-as-host-controller:7.1.2.Final");
    }
}




