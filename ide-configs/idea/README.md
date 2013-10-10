# Intellij Idea formatter configuration

There is currently no [Idea](http://www.jetbrains.com/idea/) specific code style file available which matches
the Eclipse version which defines the reference format.

The easiest way to still match the Eclipse format is to use the [Eclipse Code Formatter](http://plugins.intellij.net/plugin/index?pr=idea&pluginId=6546)
plugin which allows you to execute the Eclipse formatter from within Idea. You will have to pick the right version of
the plugin depending on the version of Idea you are using. For example at the time of writing the matching version
for Idea 11.1 is Eclipse Code Formatter 2.2.3.

Once installed you can configure the plugin to use the Eclipse formatting style under _eclipse/wildfly-formatter.xml_.
With plugin and configuration in place you can format java code using the default Idea _Reformat Code_ menu option or key binding.
You also get an icon in the menu bar which allows you to toggle between Idea native and Eclipse formatting.

It might be that you are running into the issue that the formatter [adds a single space in empty line comments]
(http://stackoverflow.com/questions/7008340/eclipse-formatter-adds-space-in-empty-comment-lines). This will fail the built time
[checkstyle audit](http://maven.apache.org/plugins/maven-checkstyle-plugin/).
The problem is caused by a [bug](https://bugs.eclipse.org/bugs/show_bug.cgi?id=360523) in the Eclipse formatter. The easiest
workaround for now is to edit _as7formatter.xml_ and disable the comment formatting (getting the javadoc format right should
be quite easy):

    <setting id="org.eclipse.jdt.core.formatter.comment.format_javadoc_comments" value="false"/>
