Release process for BOMs 
===============================

1. Update versions by running

        ./release-utils.sh -u -o <old snapshot version> -n <release version>

2. Commit the version update
3. Tag
    
        git tag -a <release version> -m "Release <release version>"
4. Stage the release
        
        ./release-utils.sh -r
5. Reset version numbers to snapshots
        
        ./release-utils.sh -u -o <release version> -n <new snapshot version>
6. Commit this
7. Promote the staged repo
