Releasing
=========

1. Make sure you have credentials set up to deploy to `jboss-releases-repository` in your `settings.xml`
2. Release the archetypes
3. Regenerate the quickstart based archetypes

        ./dist/release-utils.sh -r

4. Regenerate html readmes from markdown
        
         ./dist/release-utils.sh -m
5. Commit this
6. Update the version numbers

        dist/release-utils.sh -u -o <old snapshot version> -n <release version>

7. Commit this
8. Tag using
        
        git tag -a <release version> -m "Tag <release version>"

9. Reset the version numbers for development

        dist/release-utils.sh -u -n <new snapshot version> -o <release version>

10. Check out the tag

        git checkout <release version>
        
11. Deploy to Maven

        mvn clean deploy -DaltDeploymentRepository=jboss-releases-repository::default::https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/
        
12. Log in to the Nexus interface, and release 
13. Build the distro

        mvn clean install -f dist/pom.xml
14. Rsync the zip to download.jboss.org

        rsync -Pv --protocol=28 jbossas@download.jboss.org:download_htdocs/jbossas/7.<minor version>/jboss-as-<version>
15. Update the jboss.org/jbossas/downloads magnolia page