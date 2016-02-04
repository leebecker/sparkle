# sparkle
## Prerequisites ##
 
### Java ###
This code builds on Java 1.8.  Once installed ensure `JAVA_HOME` is set correctly.
```
export JAVA_HOME="$(/usr/libexec/java_home -v 1.8)"
 ```
 
### Maven ###
Spark requires [Maven 3.3.3+](https://maven.apache.org/download.cgi)
To ensure sufficient memory when running Spark run
```
export MAVEN_OPTS="-Xmx2g -XX:ReservedCodeCacheSize=512m"

### ScalaNLP Epic ###
Sparkle makes heavy use of the [ScalaNLP Epic project](https://github.com/dlwh/epic).  Specifically it frequently uses the Slab data structure
for propagating analyses through a pipeline.  Because Spark requires data to be serializable across RDDs,
the current 0.3.1 release of Epic will not work with Sparkle.  Instead follow these steps to install the
necessary jars into your maven .m2 directory.

Clone the code locally
```
git clone https://github.com/dlwh/epic.git
```

Modify the project/Dependencies.scala with the following values.
```
val scala               = "2.10.6"
val breeze              = "0.12-SNAPSHOT"
```

Compile and publish
```
sbt assembly
sbt publishM2
```

