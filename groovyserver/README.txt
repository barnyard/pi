http://iterative.com/blog/2007/05/14/embedding-a-groovy-console-in-a-java-server-application/

As Lisp and Smalltalk developers have known for decades, one of the advantages of dynamic, interpreted languages is that they generally include interactive development environments that allow programmers to try out ideas and experiment with APIs much more quickly than is usually possible with compiled languages.  Although tools such as Eclipse's incremental compiler have improved the situation for static, compiled languages such as Java, it's still usually much faster to use an interpreted language to enter a few lines of code and immediately execute it to see the results.

In recent years projects such as <a href="http://www.beanshell.org/">BeanShell</a>, <a href="http://jruby.sourceforge.net/">JRuby</a>, <a href="http://www.mozilla.org/rhino/">Rhino</a>, <a href="http://groovy.codehaus.org/">Groovy</a> and <a href="http://www.robert-tolksdorf.de/vmlanguages.html">several dozen other languages</a> have appeared, allowing so-called "scripting languages" to execute in a JVM and interoperate with pre-existing Java classes, making some of the advantages previously only available on more dynamic platforms accessible to Java developers.  This is a very welcome development to those of us who would rather use a dynamic language, but have a large body of legacy Java code we need to interoperate with, and don't want to give up the huge selection of libraries and frameworks that are available in Java.

One solution for integrating a dynamic language into a Java application is to use <a href="http://www.springframework.org">Spring 2</a>'s virtually transparent support for using beans written in BeanShell, JRuby and Groovy.  While this approach can greatly improve the speed of development and the flexibility of a code base, it still doesn't quite approach the utility of having a tool like Ruby's irb, in which you can simply execute code on the fly.  While each of these languages include a shell that provides this type of interaction, it can be tricky to use them to simulate an environment that is similar to that of a large running server application.  So rather than try to do so, why not just embed a dynamic language interpreter in a running application and execute commands on it while the application is running?  It turns out that doing so is not only very useful for experimentation and debugging, but is surprisingly simple.  Although I've adopted Groovy as my JVM-based dynamic language of choice, and the rest of this article explains how to use it to embed a console in a Java server application, the same approach could no doubt be adopted for virtually any other dynamic language that includes some sort of interactive interpreter and integrates with Java.

Groovy includes a command-line utility called "groovysh" that is simply a wrapper around a Java class called <a href="http://groovy.codehaus.org/api/groovy/ui/InteractiveShell.html">InteractiveShell</a>, which can be embedded in virtually any environment.  InteractiveShell's constructor can be given an <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/io/InputStream.html">InputStream</a> for input, a <a href="http://java.sun.com/j2se/1.5.0/ docs/api/java/io/PrintStream.html">PrintStream</a> for output (and errors), and a <a href="http://groovy.codehaus.org/api/groovy/lang/Binding.html">Binding</a> object, which is a mapping between tokens and Java objects that will be made available to the interpreter.  By simply wiring this class up with some standard <a href="http://java.sun.com/docs/books/tutorial/networking/sockets/index.html">Java networking code</a>, the interpreter can be launched in any server application, providing an environment that a programmer can telnet into and execute Groovy code in the running application.

In order to demonstrate exactly how to do this, I've written a few simple classes that you can download <a href="http://iterative.com/GroovyServer.tar.gz">here</a>, which can be dropped into almost any server application to add this capability.  To provide an example of how you might use it, I've integrated it into <a href="http://www.springframework.org/node/206">PimpMyShirt</a>, which was the simplest example of a Spring web application that I could find.  I've included the source to the slightly modified example application in the GroovyServer distribution.  What follows are some examples of using the GroovyServer to explore the services and APIs available in the PimpMyShirt application.

In order to add the GroovyServer to the application, I added the following lines to the Spring appContext.xml file:

    <bean id="shirtService" class="org.pimpmyshirt.service.ShirtServiceImpl" />

    <bean id="contextWrapper" class="org.pimpmyshirt.spring.ApplicationContextWrapper" />

    <bean id="groovyService" abstract="true" init-method="initialize" destroy-method="destroy">
        <property name="bindings">
            <map>
                <entry key="context" value-ref="contextWrapper" />
                <entry key="shirtService" value-ref="shirtService" />
            </map>
        </property>
    </bean>

    <bean id="groovyShellService" class="com.iterative.groovy.service.GroovyShellService" parent="groovyService">
        <property name="socket" value="6789" />
        <property name="launchAtStart" value="true" />
    </bean>

    <bean id="groovyConsoleService" class="com.iterative.groovy.service.GroovyConsoleService" parent="groovyService" />

You'll note that there are actually two Groovy-related services.  The first one, groovyShellService, is the networked wrapper around the InteractiveShell.  The second one, groovyConsoleService, is a wrapper around the <a href="http://groovy.codehaus.org/Groovy+Console">GroovyConsole</a>, which is a Swing-based application that provides essentially the same facility as the InteractiveShell, but in an application with a nice GUI.  Since only the GroovyShellService allows remote access to an application it is focus of this article.  But if you're running the server application on the same machine you're developing on, you can hit the URL http://localhost:8080/pimpmyshirt/launchGroovyConsole.html, which will trigger a simple Spring web controller to launch an instance of the GroovyConsole.  Just note that for some reason exiting the GroovyConsole will cause Tomcat to exit, but since I mostly use the GroovyShellService, and this is only intended for development and testing purposes, I haven't bothered to try and find out why this is.

Both services inherit from the groovyService abstract bean, which includes bindings for the shirtService, which is a service included in the PimpMyShirt application that we'll explore with Groovy, and an instance of ApplicationContextWrapper, which is a class that implement's Springs <a href="http://www.springframework.org/docs/api/org/springframework/context/ApplicationContext.html">ApplicationContext</a> and <a href="http://www.springframework.org/docs/api/org/springframework/context/ApplicationContextAware.html">ApplicationContextAware</a> interfaces.  The ApplicationContextWrapper is given a reference to the Spring application context through the ApplicationContextAware interface, and delegates all of ApplicationContext's methods to this instance.  I did this because I didn't want the GroovyServices to be dependent on Spring, and while there might very well be a simpler way to pass an instance of the application context to a bean without implementing ApplicationContextAware, I don't know what it is.

After building the application with the included ant script, a war file is produced that it should be possible to deploy to any J2EE application server (although I've only tested it with Tomcat).  Once it's deployed and launched, the first thing to do is to connect to the web application at http://hostname:8080/pimpmyshirt/index.html and enter some ratings for the shirts, in order to have some data in the application before we test it:

<a href="http://iterative.com/blog/wp-content/uploads/2007/05/pimpmyshirt.jpg" alt="pimpmyshirt.jpg"><img id="image9" src="http://iterative.com/blog/wp-content/uploads/2007/05/pimpmyshirt-sm.jpg" alt="pimpmyshirt-sm.jpg" /></a>

Now we can connect to the GroovyServer and run some code to display the application's state.  As configured, the application will launch the server on port 6789 when it starts, so assuming the application is running on the same machine you're sitting in front of, you can connect to it by just opening a shell and typing <code>telnet localhost 6789</code>.  What you'll see is exactly what you'd get if you were to run groovysh on it's own:

Let's get Groovy!
================
Version: 1.0 JVM: 1.5.0_07-87
Type 'exit' to terminate the shell
Type 'help' for command help
Type 'go' to execute the statements

groovy>

We can now issue commands to the interpreter and see the results:

def shirts = shirtService.getAllShirts();

shirts.each()  {
	def shirtRating = shirtService.getRating(it);
	out.println "Color: ${it.color}, Long Sleeve: ${it.longSleeve}, Graphical: ${it.print.graphical},
Text: ${it.print.text}, Low Votes: ${shirtRating.numberOfLowVotes}, Medium Votes: ${shirtRating.numberOfMediumVotes},
High Votes: ${shirtRating.numberOfHighVotes}" 
}

go

Color: WHITE, Long Sleeve: false, Graphical: false, Text: JavaPolis, Low Votes: 1, Medium Votes: 0, High Votes: 2
Color: BLUE, Long Sleeve: true, Graphical: false, Text: Spring Rocks!, Low Votes: 0, Medium Votes: 3, High Votes: 0

This code uses a Groovy closure to iterate over the ratings for available shirts and prints out the stats for each one.  I won't explain Groovy syntax in detail as I go through these examples, since full documentation on programming in Groovy can be found on the <a href="http://groovy.codehaus.org/">Groovy web site</a>.  However, before going further I need to point out one difference between executing Groovy in this environment compared to its typical usage.  You'll note that in the above example I've used "out.println" instead of just "println," as is usually the case in Groovy code.  This is because in normal Groovy "println" writes to System.out, and in a server application, System.out is usually redirected to a log file, which is not where we usually want the output of our scripts to go.  To work around this, the GroovyShellService passes in the socket's OutputStream bound to the token "out."  So to print to the interactive console, we need to use "out.println" instead of just "println."  Although there are other ways to work around this problem, which might be more transparent from the point of view of a user of the shell, I've chosen to do it this way since it's the easiest to implement, and the most explicit with regards to what's actually happening under the covers.

Note that since we've configured the GroovyServer in the Spring application context to have a binding to "shirtService," it's already available to us.  If we hadn't done so, we could've also gotten a reference to the service from the application context by prepending the following to the code snippet above:

def shirtService = context.getBean("shirtService");

We can also call methods on the Spring application context to see, for example, what beans are available:

context.getBeanDefinitionNames().each() { out.println it };
go

shirtService
contextWrapper
groovyService
groovyShellService
groovyConsoleService

===> null

And in addition to the application services that are defined in the Spring context, we can also interrogate the web and application server layers to see how they're configured.  For example, we could get the <a href="http://java.sun.com/webservices/docs/1.5/api/javax/servlet/ServletContext.html">ServletContext</a> from Spring and display its attribute names and values:

def servletContext = context.getServletContext();

Enumeration e = servletContext.getAttributeNames();
while (e.hasMoreElements()) {
	def attributeName = e.nextElement();
	out.println "${attributeName}: ${servletContext.getAttribute(attributeName)} \n";
}
go

org.apache.catalina.jsp_classpath: /usr/local/apache-tomcat-5.5.23/webapps/pimpmyshirt/WEB-INF/classes/:
/usr/local/apache-tomcat-5.5.23/webapps/pimpmyshirt/WEB-INF/lib/commons-collections-3.1.jar:(etc . . .)

javax.servlet.context.tempdir: /usr/local/apache-tomcat-5.5.23/work/Catalina/localhost/pimpmyshirt 

org.springframework.web.servlet.FrameworkServlet.CONTEXT.pimpmyshirt:
org.springframework.web.context.support.XmlWebApplicationContext: display name [WebApplicationContext for
namespace 'pimpmyshirt-servlet']; startup date [Mon Apr 30 01:52:03 EDT 2007]; child of
[org.springframework.web.context.support.XmlWebApplicationContext: display name [Root WebApplicationContext];
startup date [Mon Apr 30 01:52:02 EDT 2007]; root of context hierarchy; config locations
[classpath:org/pimpmyshirt/service/applicationContext.xml]]; config locations [/WEB-INF/pimpmyshirt-servlet.xml] 

interface org.springframework.web.context.WebApplicationContext.ROOT:
org.springframework.web.context.support.XmlWebApplicationContext: display name [Root WebApplicationContext];
startup date [Mon Apr 30 01:52:02 EDT 2007]; root of context hierarchy; config locations
[classpath:org/pimpmyshirt/service/applicationContext.xml] 

org.apache.catalina.resources: org.apache.naming.resources.ProxyDirContext@fc1695 

org.apache.catalina.WELCOME_FILES: {"index.html", "index.htm", "index.jsp"} 

===> null

We can see from this output that Spring's WebApplicationContext, in which the servlets that Spring uses to provide hooks into it's web framework are defined is bound to the ServletContext's "org.springframework.web.servlet.FrameworkServlet.CONTEXT.pimpmyshirt" attribute.  If we wanted a list of which beans were configured in the WebApplicationContext, we could print them out by doing the following:

def servletContext = context.getServletContext();
def servletAppContext = servletContext.getAttribute("org.springframework.web.servlet.FrameworkServlet.CONTEXT.pimpmyshirt");
servletAppContext.getBeanDefinitionNames().each() { out.println it };
go

viewResolver
messageSource
multipartResolver
/index.html
/image.html
flowController
composeShirt
composeShirtAction
/launchGroovyConsole.html

We could also explore further, and get an instance of the RateShirts view from Spring's <a href="http://www.springframework.org/docs/api/org/springframework/web/servlet/ViewResolver.html">ViewResolver</a>:

def servletContext = context.getServletContext()
def servletAppContext = servletContext.getAttribute("org.springframework.web.servlet.FrameworkServlet.CONTEXT.pimpmyshirt");
def viewResolver = servletAppContext.getBean("viewResolver");
def view = viewResolver.buildView("RateShirts");
view
go

===> org.springframework.web.servlet.view.JstlView: name 'RateShirts'; URL [/WEB-INF/jsp/RateShirts.jsp]

Or we could get an instance of the RateShirts web controller and view the contents of it's model data after it's initialized:

def servletContext = context.getServletContext();
def servletAppContext = servletContext.getAttribute("org.springframework.web.servlet.FrameworkServlet.CONTEXT.pimpmyshirt");
def rateShirtsController = servletAppContext.getBean("/index.html");
rateShirtsController.getModel()
go

===> {shirtRatings=[org.pimpmyshirt.domain.ShirtRating@6ebc80, org.pimpmyshirt.domain.ShirtRating@198a2f],
ratings=[Lorg.pimpmyshirt.domain.Rating;@88b2fa}

Obviously this is a sample application with a single service that doesn't do very much, so there isn't that much more we can do with it that would be all that interesting.  However, for a real application with dozens or more services that were reasonably complex, it shouldn't be hard to imagine the usefulness of being able to interact with them to test their functionality and experiment with using them.
