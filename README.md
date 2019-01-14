# Example Cassandra+Javalin Asynchronous REST Server

This project is a simple example of using 
[Apache Cassandra](http://cassandra.apache.org) and 
[Javalin](https://javalin.io/) to build a high-performance asynchronous REST 
server.

## About the Project

The project is presently using the Apache Cassandra 3.x driver.  The 4.x driver 
has significant improvements that remove the need for the asynchronous 
boilerplate code in 
[WidgetDao](src/main/java/com/kineticdata/examples/javalin/daos/WidgetDao.java)
(most notably natively working with CompletableFuture rather than Guava's 
ListenableFuture and better built in support for asynchronous paging), however
it has yet to be release as of the time of this writing.

The simplicity of Javalin is very evident in this Java project, however much of
the Javalin framework is even more impressive when usin Kotlin.  The intent of
this project was to demonstrate the asynchronous interactions between Cassandra
and Javalin in Java, however I'm also working on an example project using
Cassandra and Javalin in Kotlin.

The portion of the project that leverages Cassandra is in the
[WidgetDao](src/main/java/com/kineticdata/examples/javalin/daos/WidgetDao.java)
and the portion of the project that leverages Javalin is in the 
[ExampleApp](src/main/java/com/kineticdata/examples/javalin/ExampleApp.java).

There is also an example E2E test available in the
[ExampleAppE2ETest](src/test/java/com/kineticdata/examples/javalin/ExampleAppE2ETest.java),
that extends the
[E2ETestBase](src/test/java/testing/kineticdata/examples/javalin/E2ETestBase.java)
class, which is responsible for starting the Javalin webserver in a background
thread for E2E tests.

## About Me

My name is Ben Christenson and I am the Tech Lead (meaning that I am responsible
for a combination of architecture, development, and team leadership) at 
[Kinetic Data](http://www.kineticdata.com).  I'm heavily involved with 3 
platform components heavily leveraging Cassandra and 2 components exposed via
Javalin (one high performance REST API and one that exposes platform events via 
Websockets).

## Prerequisites

### Cassandra

Cassandra is a prerequisite for running the sample app and E2E test case.
Comprehensive instructions for installation and configuration of Cassandra is
out of scope for this project, but a bare-bones example installation process is 
included below.

Example installation (OSX):
```
mkdir ~/servers
cd ~/servers
wget https://www-us.apache.org/dist/cassandra/3.11.3/apache-cassandra-3.11.3-bin.tar.gz
tar xf apache-cassandra-3.11.3-bin.tar.gz
cd apache-cassandra-3.11.3/bin
./cassandra
```

Copy and past the following into CQLSH (`$ ~/servers/apache-cassandra-3.11.3/bin/cqlsh`):
```
CREATE KEYSPACE IF NOT EXISTS cassandra_javalin_example 
   WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };
 
USE cassandra_javalin_example;

CREATE TABLE IF NOT EXISTS widgets (
    tenant_key text,
    key text,
    description text,
    PRIMARY KEY ((tenant_key), key)
);
```