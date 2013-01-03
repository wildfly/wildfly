The version of remoting in 7.1.2.Final and 7.1.2.Final do not work with clients using the version of remoting in 7.2.x.
This can be illustrated by running a CLI from 7.2.x against 7.1.2/3 and executing an operation returning a lot of data.
Here is the slave thread dump: http://pastebin.com/e32D97gg, and here the test thread dump http://pastebin.com/XsjbCWBc.

Slaves of these versions will have to have their versions of remoting and xnio upgraded to the jars contained in this directory.