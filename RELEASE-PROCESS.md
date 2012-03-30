Release process for BOMs 
===============================

Setup
-----

1. You must have gpg set up and your key registered, as described at <http://www.sonatype.com/people/2010/01/how-to-generate-pgp-signatures-with-maven/>
2. You must provide a property `gpg.passphrase` in your `settings.xml` in the `release` profile e.g.

        <profile>
             <id>release</id>
             <properties>
                 <gpg.passphrase>myPassPhrase</gpg.passphrase>
             </properties>
        </profile>
3. You must have a JBoss Nexus account, configured with the server id in `settings.xml` with the id `jboss-releases-repository` e.g.

        <server>
            <id>jboss-releases-repository</id>
            <username>myUserName</username>
            <password>myPassword</password>
        </server>

Perform a release
-----------------

Simply run:  

    ./release.sh -s <old snapshot version> -r <release version>

