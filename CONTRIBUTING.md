JBoss BOMs Contributing Guide
=============================

BOM's are maven pom.xml files that specify the versions of all runtime dependencies for a given module.  So by importing this BOM, you are specifying the versions of the dependencies required to use the specified stack.

Basic Steps
-----------

To contribute with BOMs, clone your own fork instead of cloning the main BOMs repository, commit your work on topic branches and make pull requests. In detail:

1. [Fork](http://help.github.com/fork-a-repo/) the project.

2. Clone your fork (`git@github.com:<your-username>/jboss-bom.git`).

3. Add an `upstream` remote (`git remote add upstream git@github.com:jboss-jdf/jboss-bom.git`).

4. Get the latest changes from upstream (e.g. `git pull upstream master`).

5. Create a new topic branch to contain your feature, change, or fix (`git checkout -b <topic-branch-name>`).

6. Make sure that your changes follow the General Guide Lines.

7. Commit your changes to your topic branch.

8. Push your topic branch up to your fork (`git push origin  <topic-branch-name>`).

9. [Open a Pull Request](http://help.github.com/send-pull-requests/) with a clear title and description.

If you don't have the Git client (`git`), get it from: <http://git-scm.com/>

General Guidelines
------------------

* It can be tricky to work out when to add a new stack, rather than extend an existing stack. We strongly encourage you to discuss your planned BOM on the [dev list](http://www.jboss.org/jdf/forums/jdf-dev/) before starting.

* Each BOM is a child module of the parent BOM module. Copy an existing module as a template. Remember to give it a unique, and descriptive name. You should follow the conventions defined by the existing BOMs when naming it. All BOMs live in the same repository.

* Most BOMs build on the base Java EE stack, and as such, import it. This is reflected in the name of the BOM "jboss-javaee6-with-XXX".

* All dependencies versions should references properties that is declared on root `pom.xml`

* The BOM should contain a `README.md` file, explaining:
   * What the stack described by the BOM includes 
   * An example of its usage
   * Any notes about plugins included in the stack

* The BOM should be formatted using the JBoss AS profiles found at <https://github.com/jboss/ide-configs/tree/master/ide-configs>

