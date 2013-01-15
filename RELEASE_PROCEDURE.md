BOMs Release Procedure
=======================

Publishing builds to Maven
--------------------------

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

  4. Add `org.sonatype.plugins` plugin group to your `settings.xml` so nexus plugin can be available for publishing scripts.

          <pluginGroups>
              <pluginGroup>org.sonatype.plugins</pluginGroup>
          </pluginGroups>

Release
-------

To release, simply run:  
      
      ./release.sh -s <old snapshot version> -r <release version>

  This will  update the version number, commit and tag and publish the BOMs to the JBoss Maven repository, and prompt to close and promote the Maven Nexus staging repository. Then it will reset the version number back to the snapshot version number. The artifacts will be replicated to Maven central within 24 hours.
