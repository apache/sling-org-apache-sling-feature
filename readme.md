[<img src="http://sling.apache.org/res/logos/sling.png"/>](http://sling.apache.org)

 [![Build Status](https://builds.apache.org/buildStatus/icon?job=sling-org-apache-sling-feature-1.8)](https://builds.apache.org/view/S-Z/view/Sling/job/sling-org-apache-sling-feature-1.8) [![Test Status](https://img.shields.io/jenkins/t/https/builds.apache.org/view/S-Z/view/Sling/job/sling-org-apache-sling-feature-1.8.svg)](https://builds.apache.org/view/S-Z/view/Sling/job/sling-org-apache-sling-feature-1.8/test_results_analyzer/) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.feature/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.feature%22) [![JavaDocs](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.feature.svg)](https://www.javadoc.io/doc/org.apache.sling/org.apache.sling.feature) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# The Sling OSGi Feature Model

OSGi is a platform capable of running large applications for a variety of purposes, including rich client applications, 
server-side systems and cloud and container based architectures. 

As these applications are generally based on many bundles, describing each bundle individually in the application 
definition becomes unwieldy once the number of bundles reaches a certain level. 

Addinally, OSGi has no mechanism to describe other elements of the application definition, such as configuration or custom artifacts.

The Sling OSGi Feature Model introduces a higher level to describe OSGi applications that encapsulates the details of the various 
components that the application is built up from. It allows the description of an entire OSGi-based application based on reusable 
components and includes everything related to this application, including bundles, configuration, framework properties, capabilities, 
requirements and custom artifacts.

# Features

Features are the central concept of the Feature Model. Features are normally defined in a Feature file which is
a JSON file. An example feature file can be found here: https://github.com/apache/sling-org-apache-sling-feature-io/blob/master/design/feature-model.json
and the JSON Schema for feature files is available from here: https://github.com/apache/sling-org-apache-sling-feature-io/blob/master/src/main/resources/META-INF/feature/Feature-1.0.0.schema.json

All features have a unique identity.
Features can have an optional `title`, `description`, `vendor` name, `license`.

Features typically reference one or more bundles that declare the behaviour provided by the feature. These
bundles may have dependencies on other bundles or capabilities in the feature, e.g. defined by other features or bundles.

Features can define OSGi Configurations that will be provided into the runtime with the feature and features can also declare additional requirements and capabilities over and above the ones coming from the bundles
that are part of the feature.  

Features can be declared _from scratch_ or can use another pre-existing feature as a prototype. In this case
the new feature starts off as a feature identical to its prototype, with some exceptions:

* it does not get the prototype's identity. 
* bundles, configurations and framework properties can be removed from the prototypel, in the `removals` section.
* anything declared in the feature definition of a feature based on a prototype adds or overrides to 
what came from the prototype. 

Features can also be marked as `final` and/or `complete`.
A `final` feature cannot be used as a prototype for another feature. A feature marked as `complete` indicates
that all its dependencies are met by capabilities inside the feature, i.e. it has no external dependencies. 

aggregates - launching
  
## Feature Identity

A feature has a unique id. Maven coordinates (https://maven.apache.org/pom.html#Maven_Coordinates) provide a well defined and accepted way of uniquely defining such an id. The coordinates include at least a group id, an artifact id, a version and optionally a type and classifier.

While group id, artifact id, version and the optional classifier can be freely choosen for a feature, the type/packaging is defined as `slingosgifeature`.

## Maven Coordinates

Maven coordinates are used to define the feature id and to refer to artifacts contained in the feature, e.g. bundles, content packages or other features. There are two supported ways to write down such a coordinate:

* Using a colon as a separator for the parts: groupId:artifactId[:type[:classifier]]:version as defined in https://maven.apache.org/pom.html#Maven_Coordinates
* Using a mvn URL: `'mvn:' group-id '/' artifact-id [ '/' [version] [ '/' [type] [ '/' classifier ] ] ] ]`

In some cases only the coordinates are specified as a string in one of the above mentioned formats. In other cases, the artifact is described through a JSON object. In that case, the *id* property holds the coordinates in one of the formats.


## Bundles

## Configuration

## Requirements and Capabilities

# Extensions

# References

This project aims to define a common OSGi feature model to build an OSGi application out of higher level modules.

* [Requirements](requirements.md)
* [File format](https://github.com/apache/sling-org-apache-sling-feature-io/blob/master/design/feature-model.json)

* [Prototype](prototype.md)
* [API Controller Proposal](apicontroller.md)
* [Application Configuration Proposal](appconf.md)
