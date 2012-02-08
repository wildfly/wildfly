Generating configurations
=========================

To avoid duplication of subsystem code among the multiple domain and standalone configurations, the configs are 
now generated. build-configs.xml contains the maven tasks to assemble the configs from subsystem snippets. The 
main inputs are:
1) A template.xml file
2) A subsystems.xml file listing the different subsystem snippet files
3) The subsystem snippet files referenced by 2)
4) The name of the configuration file to generate 

1.0 The template.xml file
---------------------
There is one for domain configs at src/resources/configuration/domain/template.xml, and one for stanalone configs at 
src/resources/configuration/standalone/template.xml. The assembly process reads the subsystems.xml files and the 
subsystem snippet files and builds up the configuration. The template.xml file has the following processing
instruction placeholders into which the submitted data gets added:

<?EXTENSIONS?>
Gets substituted with a list of all the extensions brought in by reading the subsystem snippets referenced by the 
subsystems.xml.

<$SUBSYSTEMS socket-binding-group="group-name"?>
Gets substituted with the subsystem configurations brought in by reading the subsystem snippets referenced by the 
subsystems.xml. 
In standalone there is only one profile, so it uses the unnamed <subsystems> entry from subsystems.xml as the input
for the subsystems to use. 
In domain there may be several profiles, so each of these place holders uses the name of the containing <profile> 
element to look up the <subsystem> element to use from subsystems.xml as the input for the subsystems to use.
Socket bindings for each of these sets get associated with the name 'group-name'
 
<?SOCKET-BINDINGS?>
This gets substituted with the relevant socket bindings. The containing <socket-binding-group>'s name is used 
to look up the socket bindings brought in from the subsystems we are interested in.

2.0 The subsystem.xml files
-----------------------
This simply lists the file paths for each subsystem snippet file you want in your configuration. The paths are
relative to the src/main/resources folder. For standalone configurations there will be only one (unnamed) list 
of subsystems, for domain configurations there needs to be a named list of subsystem per profile in the template. 

Most subsystems are the same no matter where they are used, so they are just included in the basic form, e.g:

   <subsystem>configuration/subsystems/ee.xml</subsystem>

Some subsystems have different flavours depending on for example if they are run in ha or not, which is set up in the
corresponding snippet file, you can choose these by passing in the 'substitution' parameter, e.g.:

   <subsystem supplement="ha">configuration/subsystems/ejb3.xml</subsystem>
   
3.0 The subsystem snippet files
---------------------------
This contains the definition for a subsystem. For a straight-forward subsystem which is always used the same it
will look something like:

   <?xml version='1.0' encoding='UTF-8'?>
   <config>
      <extension-module>org.jboss.as.simple</extension-module>
      <subsystem xmlns="urn:jboss:domain:simple:1.1">
         <!-- whatever settings the subsystem takes -->
      </subsystem>
      <socket-binding name="simple" interface="unsecure" port="3528"/>
   </config>
   
* 'extension-module' contains the name of the module containing the subsystem. This will go into the <?EXTENSTIONS?>
part of the template.
* 'subsystem' contains the subsystem configuration you would like to appear in the resulting configuration file.
This will go into the <$SUBSYSTEMS socket-binding-group="group-name"?> part of the template.
* You can have >=0 'socket-binding' entries showing the socket bindings needed for the subsystem. This will go into 
the <?SOCKET-BINDINGS?> part of the template.

3.1 substititions
-----------------
Some subsystems might need something special happening in 'ha' mode only for example, so we can create a 
'supplement' to be able to choose what happens.

   <?xml version='1.0' encoding='UTF-8'?>
   <config>
      <extension-module>org.jboss.as.simple</extension-module>
      <subsystem xmlns="urn:jboss:domain:simple:1.1">
         <?CLUSTERED?>
      </subsystem>
      <supplement name="ha">
         <replacement placeholder="CLUSTERED">
            <cluster-config> 
               <config1/>
            </cluster-config>
         </replacement>
      </supplement>
      <socket-binding name="simple" interface="unsecure" port="3528"/>
   </config>

The <?CLUSTERED?> processing instruction will be ignored, and removed, if included with no specified supplement from the 
subsystem.xml, e.g.: <subsystem>configuration/subsystems/simple.xml</subsystem>

If included specifying a supplement, e.g.: <subsystem supplement="ha">configuration/subsystems/simple.xml</subsystem>
the <?CLUSTERED?> processing instruction gets replaced by the contents of the ha supplement's 'CLUSTERED' replacement, so
in the output file we end up with the following subsystem.
      <subsystem xmlns="urn:jboss:domain:simple:1.1">
         <cluster-config> 
            <config1/>
         </cluster-config>
      </subsystem>

You can have as many replacements as you like within a substitution. Processing instructions within a subsystem that 
do not match any replacements are removed.

3.2 default substitutions and attribute replacements
----------------------------------------------------

   <?xml version='1.0' encoding='UTF-8'?>
   <config default-supplement="default">
      <extension-module>org.jboss.as.simple</extension-module>
      <subsystem xmlns="urn:jboss:domain:simple:1.1" simple-setting="@@simple-setting@@">
      </subsystem>
      <supplement name="default">
         <replacement placeholder="@@simple-setting@@" attributeValue="normal"/>
      </supplement>
      <supplement name="ha">
         <replacement placeholder="@@simple-setting@@" attributeValue="clustered"/>
      </supplement>
      <socket-binding name="simple" interface="unsecure" port="3528"/>
   </config>

Here we have a placeholder for the subsystem's 'simple-setting' attribute.
If included as <subsystem supplement="default">configuration/subsystems/simple.xml</subsystem> the 'simple-setting' attribute 
value is replaced with 'normal' from the 'default' supplement.
If included as <subsystem supplement="ha">configuration/subsystems/simple.xml</subsystem> the 'simple-setting' attribute value
is replaced with 'clustered' from the 'ha' supplement.

There is a 'default-supplement' attribute on the root 'config' element, so if you just include as
<subsystem>configuration/subsystems/simple.xml</subsystem> the 'default' supplement is automatically chosen and 
'clustered' is used for the 'simple-setting' attribute value.

3.3 substitution includes
-------------------------

   <config default-supplement="default">
      <extension-module>org.jboss.as.simple</extension-module>
      <subsystem xmlns="urn:jboss:domain:simple:1.1" simple-setting="@@simple-setting@@">
         <?BEAN?>
         <?FULL?>
         <?CLUSTER?>
      </subsystem>
      <supplement name="default">
         <replacement placeholder="BEAN">
            <bean config="simple"/>
         </replacement>
      </supplement>
      <supplement name="full" includes="default">
         <replacement placeholder="FULL">
            <full/>
         </replacement>
      </supplement>
      <supplement name="ha">
         <replacement placeholder="BEAN">
            <bean config="clustered"/>
         </replacement>
         <replacement placeholder="CLUSTER">
            <clustered/>
         </replacement>
      </supplement>
      <supplement name="full-ha" includes="full ha"/>
      <socket-binding name="simple" interface="unsecure" port="3528"/>
   </config>

If using the 'full-ha' supplement, the resulting subsystem will be
      <subsystem xmlns="urn:jboss:domain:simple:1.1" simple-setting="@@simple-setting@@">
         <bean config="clustered"/>
         <full/>
         <clustered/>

Includes are processed left-to-right, top-to-bottom, so the replacements will be added in the following 
order of substitutions: default, full, ha. Later replacements repace earlier ones.  
