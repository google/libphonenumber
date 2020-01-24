# How to run the Java demo

## About this document

This document explains how to build and run the demo of the Java version of
libphonenumber, **from the command line** on **Linux** or **Mac**, using Google
App Engine. By following the instructions here, you can build a demo running
against any revision of the Java code by supplying jar files you build on your
own, or downloading the ones from [Maven
Central](http://repo1.maven.org/maven2/com/googlecode/libphonenumber/libphonenumber/).

## Detailed steps

### Install Google App Engine

Download and follow [the
instructions](http://cloud.google.com/appengine/downloads) to install the Java
SDK of Google App Engine. This document assumes it is named as
`appengine-java-sdk`. You may want to rename the directory after unpacking.

### Check out the demo code

Check out the Java code:

```
git clone https://github.com/google/libphonenumber.git
```

Create a symlink to your unpacked appengine sdk directory at the same level as
the `java` directory. Alternatively, update `java/demo/build.xml` to point to
the right location for the appengine sdk.

### Get the phone number library jars

Save the `libphonenumber`, `geocoder`, `carrier`, and `prefixmapper` jars under
`demo/war/WEB-INF/lib/`.

You can either download them from the  [Maven
repository](http://repo1.maven.org/maven2/com/googlecode/libphonenumber/) or
build them yourself by running:

```
git clone https://github.com/google/libphonenumber.git
cd libphonenumber/java
ant jar
cp build/jar/* demo/war/WEB-INF/lib
```

### Run the demo code

Start the server:

```
cd demo
ant runserver
```

This will start a server on your localhost, and you can try it out by pointing
your browser to http://localhost:8080/.

## Troubleshooting

If you get a warning from App Engine asking you to upgrade your JRE to Java 1.6
or later, you might need to install Java 1.6 and point your `JAVA_HOME` to it.

*   To see your current `JAVA_HOME`, run `echo $JAVA_HOME`.
*   To see the list of JDKs currently installed, use `update-java-alternatives
    -l`.
*   To reset `JAVA_HOME`, use `export JAVA_HOME=[path to Java 1.6 SDK]/jre`.

Now run `ant runserver` to start the server on your localhost.

## Uploading your own demo App Engine application

If you want to upload the demo to your own App Engine application, follow the
steps to [register an application with App
Engine](http://cloud.google.com/appengine/docs/standard/java/gettingstarted/deploying-the-application).

Then run:

```
../appengine-java-sdk/bin/appcfg.sh update war
```

The demo will be uploaded to `http://application-id.appspot.com/`.
