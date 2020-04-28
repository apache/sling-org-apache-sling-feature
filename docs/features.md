# Features

A feature is the central entity for the Feature Model. A feature is described through a JSON object and can contain:

* Metadata like a unique identifier, description etc.
* OSGi bundles
* OSGi configurations
* OSGi Framework properties
* Extensions - a plugin mechanism to add additional information to the feature

## Feature Metadata

Each feature must specify a unique identifier. Other attributes like `title`, `description`, `vendor` name, and `license` are optional.

## Feature Identity

A feature has a unique identifier. [Maven coordinates](https://maven.apache.org/pom.html#Maven_Coordinates) provide a well defined and accepted way of uniquely defining such an id. The coordinates include at least a group id, an artifact id, a version and optionally a type and classifier.

While group id, artifact id, version and the optional classifier can be freely choosen for a feature, the type/packaging is defined as `slingosgifeature`.

## Maven Coordinates

Maven coordinates are used to define the feature id and to refer to artifacts contained in the feature, e.g. bundles, content packages or other features. There are two supported ways to write down such a coordinate:

* Using a `Maven id` which uses a colon as a separator for the parts: *groupId:artifactId[:type[:classifier]]:version* as defined in [Maven Coordinates](https://maven.apache.org/pom.html#Maven_Coordinates)
* Using a `Maven URL` : *'mvn:' group-id '/' artifact-id [ '/' [version] [ '/' [type] [ '/' classifier ] ] ] ]*

In some cases only the coordinates are specified as a string in one of the above mentioned formats. In other cases, the artifact is described through a JSON object. In that case, the *id* property holds the coordinates in one of the formats.

```json
Example for specifying a feature id using a Maven id:
{
  "id" : "org.apache.sling:org.apache.sling.core.feature:slingosgifeature:1.0.0"
}

Same id specified as a Maven url:
{
  "id" : "org.apache.sling/org.apache.sling.core.feature/1.0.0/slingosgifeature"
}

```

## Feature File Format

Features are defined as a JSON object. An example feature file can be found here: [feature-model.json](feature-model.json)
and the JSON Schema for feature files is available from here: [Feature-1.0.0.schema.json](../src/main/resources/META-INF/feature/Feature-1.0.0.schema.json).

Comments in the form of [JSMin (The JavaScript Minifier)](https://www.crockford.com/javascript/jsmin.html) comments are supported, that is, any text on the same line after `//` is ignored and any text between `/* */` is ignored.

Java API for Serialization/Deserialization into/from this format is part of this module.

## OSGi Bundles

Features typically declare one or more bundles that provide the behaviour of the feature. The bundles are not stored inside the feature but referenced via their Maven coordinates in the `bundles` section of the feature model. The bundles are defined using an array. Each bundle is referenced either by a string value in that array or by a JSON object which at least must have an `id` property for the bundle coordinates. Additional metadata can also be placed inside that object. It is possible to mix bundles specified by just the id (string) and object in a single array.

```json
A feature with bundles:
{
  "id" : "org.apache.sling:org.apache.sling.core.feature:slingosgifeature:1.0.0",
  "bundles" : [
      // defining a bundle by just using the id
      "org.apache.sling:org.apache.sling.api:2.1.14",
      // defining a bundle by using an object
      {
        "id" : "org.apache.sling:org.apache.sling.engine:2.5.0",
        "info" : "This is the core implementation of Apache Sling"
      }
  ]
}
```

Multiple versions of a bundle with the same group ID, artifact ID (,classifier and type) are allowed.

Bundles usually have requirements, for example they contain package imports - such requirements can be satisfied either by other bundles from the same or other features. Or in other words, a feature must not be complete and fulfill all requirements mentioned within the parts of the feature.

## OSGi Configurations

OSGi configurations are specified in the `configurations` section of the feature model as a JSON object. The format for configurations is using the standard OSGi configuration resource format as defined by the [OSGi Configurator
specification](https://osgi.org/specification/osgi.cmpn/7.0.0/service.configurator.html).

The configuration section contains set of configuration dictionaries each with a `persistence identifier` (PID) key to target a specific PID in the `Configuration Admin Service` and zero or more configuration values for this PID. Factory configurations can be addressed using the `factory PID` and a name by starting with the factory PID, appending a tilde, and then appending the name. This ensures a well-known name for the factory configuration instance. In the case of single configurations, the PID is used as the key.

```json
A feature with configurations:
{
  "id" : "org.apache.sling:org.apache.sling.core.feature:slingosgifeature:1.0.0",
  "configurations" : {
    // single configuration
    "org.apache.sling.engine.MainServlet" : {
      "title" : "Apache Sling",
      "allowPost" : true,
      "port:Integer" : "8080"
    },
    // a factory configuration
    "org.apache.sling.logging.Logger~core" : {
      "name" : "core",
      "level" : "DEBUG"
    }
  }
}
```

### Configurations belonging to Bundles

In most cases, configurations belong to a bundle. The most common use case is a configuration for a (Declarative Service) component. Therefore instead of having a separate configurations section, it is more intuitive to specify configurations as part of a bundle. The benefit of this approach is, that it can easily be decided if a configuration is to be used: if exactly that bundle is used, the configurations are used; otherwise they are not.

Instead of defining configurations globally for the feature as seen above, a configuration can be specified as part of a bundle.

```json
A feature with a configuration attached to a bundle:
{
  "id" : "org.apache.sling:org.apache.sling.core.feature:slingosgifeature:1.0.0",
  "bundles" : [
    {
      "id" : "org.apache.sling:security-server:2.2.0",
      "configurations" : {
        "org.apache.sling.security.Server" : {
          "debug" : true,
          "port" : 4920
        }
      }
    }
  ]
}
```

## OSGi Framework properties

Apart from OSGi configurations, sometimes OSGi framework properties are used for configurations. Framework properties are key value pairs where the value is a string. They can be specified with the 'framework-properties' key in the feature:

```json
A feature with framework properties:
{
  "id" : "org.apache.sling:org.apache.sling.core.feature:slingosgifeature:1.0.0",
  "framework-properties" : {
    "org.apache.sling.logging.default.level" : "DEBUG",
    "org.apache.felix.framework.cache"  : "true"
  }
}
```

## Requirements and Capabilities

In order to avoid a concept like *Require-Bundle* a feature does not explicitly declare dependencies to other features. These are declared by the required capabilities, either explicit or implicit. The implicit requirements are calculated by inspecting the contained bundles (and potentially other artifacts like content packages).

Features can also explicitly declare additional requirements they have over and above the ones coming from the bundles. This is done in the `requirements` section of the feature. Each requirement has a namespace and optional directives and attributes.

```json
A feature declaring additional requirements:
{
  "id" : "org.apache.sling:org.apache.sling.core.feature:slingosgifeature:1.0.0",
  "requirements" : [
    {
      "namespace" : "osgi.contract",
      "directives" : {
          "filter" : "(&(osgi.contract=JavaServlet)(version=3.1))"
      }
    }
  ]
}
```

Features can declare additional capabilities that are provided by the feature in addition to the capabilities provided by the bundles. For example a number of bundles might together provide an `osgi.implementation` capability, which is not provided by any of those bundles individually. The Feature can be used to add this capability to the provided set. These additional capabilities are specified in the `capabilities` section of the feature. Each capability has a namespace and optional directives and attributes.

```json
A feature declaring additional capabilities:
{
  "id" : "org.apache.sling:org.apache.sling.core.feature:slingosgifeature:1.0.0",
  "capabilities" : [
    {
       "namespace" : "osgi.implementation",
       "attributes" : {
             "osgi.implementation" : "osgi.http",
             "version:Version" : "1.1"
       },
       "directives" : {
             "uses" : "javax.servlet,javax.servlet.http,org.osgi.service.http.context,org.osgi.service.http.whiteboard"
       }
    }
  ]
}
```

## Extensions

The Feature Model is extensible, meaning that it can be augmented with custom content in a number of ways. Custom content be useful to co-locate metadata with its associated feature or to enhance the feature model with new functionality. The API Regions described in the [API Regions documentation](https://github.com/apache/sling-org-apache-sling-feature-extension-apiregions/blob/master/docs/api-regions.md) is an example of enhancing the feature functionality.

Custom content can have one of the following formats/types:

* Plain text
* A JSON structure (object or array)
* An array of artifacts

The extension can have one of the following states:

* 'required' : Required extensions need to be processed by tooling
* 'optional' : Optional extensions might be processed by tooling, for example they might contain environment specific parts
* 'transient': Transient extensions are cache like extensions where tooling can store additional information to avoid reprocessing of down stream tooling. However such tooling must work without the transient extension being available.

Extensions are declared in the feature by adding a JSON structure with a key following this syntax:

```json
"extention-name:<type>|{optional|required|transient}": JSON Structure
```

For example, the following declaration defines an extension with name `api-regions` using the type `JSON`. The declaration also states that if no plugin is present that knows about this extension it should be ignored and execution should continue.

```json
"api-regions:JSON|optional" : [
   {"name": "global"}
]
```

## Variables

Variables can be declared with the 'variables' key in the feature. This can be used for late binding of values, for example when launching a feature.

```json
A feature declaring variables:
{
  "id" : "org.apache.sling:org.apache.sling.core.feature:slingosgifeature:1.0.0",
  "variables" : {
    "port" : 8080
  },
  "framework-properties" : {
    "org.apache.felix.http.port" : "${port}"
  }
}
```

Variables can be used in the values of framework properties and in the values of configurations.

## Prototype

Features can be declared _from scratch_ or they can use another pre-existing feature as a prototype. Using a prototype is especially useful to make minor adjustments to an existing feature and use the modified feature instead of the original, for example replacing a single bundle.

```json
A feature using a prototype:
{
  "id" : "org.apache.sling:my-sling-core-feature:slingosgifeature:1.0.0",
  "prototype" : {
     "id" : "org.apache.sling:org.apache.sling.core.feature:slingosgifeature:1.0.0",
     "removals" : {
         ...
     }
  }
}
```

If a feature uses a prototype, the following parts are copied from the prototype into the feature:

* Variables
* Bundles
* Configurations
* Framework properties
* Extensions
* Requirements and capabilities

Then the removal instructions of the prototype object are handled next.

Finally, the contents from the prototype is then overlayed with the definitions from the feature using the prototype. In detail this means:

* Variables from the feature overwrite variables from the prototype.
* A clash of artifacts (such as bundles) between the prototype and the feature is resolved by picking the version from the feature, not its prototype. Artifact clashes are detected based on Maven Coordinates, not on the content of the artifact. So if a prototype defines a bundle with artifact ID `org.sling:somebundle:1.2.0` and the feature itself declares `org.sling:somebundle:1.0.1` in its `bundles` section, the bundle with version `1.0.1` is used.
* Configurations will be merged by default, the ones from the feature potentially overwriting configurations from the prototype:
  * If the same property is declared in more than one feature, the last one wins - in case of an array value, this requires redeclaring all values (if they are meant to be kept).
* Framework properties from the feature overwrite framework properties from the prototype.
* Extensions are handled in an extension specific way, by default the contents are appended. In the case of extensions of type `Artifact` these are handled just like bundles. Extension merge plugins can be configured to perform custom merging.
* Capabilities and requirements are appended - this might result in duplicates, but that doesn't really hurt in practice.

## Final and Complete Features

A feature might be self-contained, for example a feature describing a complete application has no external dependencies. Such a feature can be marked as `complete` indicating
that all its dependencies and requirements are met by capabilities inside the feature.

```json
A feature marked as complete
{
  "id" : "org.apache.sling:org.apache.sling.core.feature:slingosgifeature:1.0.0",
  "complete" : true
}
```

Features can also be marked as `final`. Such a feature cannot be used as a prototype for another feature, similar to the final classifier on a class file in Java.

```json
A feature marked as final
{
  "id" : "org.apache.sling:org.apache.sling.core.feature:slingosgifeature:1.0.0",
  "final" : true
}
```

## Relation to Repository Specification (Chapter 132)

There are two major differences between a repository as described in the [Repository Service Description](https://osgi.org/specification/osgi.cmpn/7.0.0/service.repository.html) and the feature model. A repository contains a list of more or less unrelated resources whereas a feature describes resources as a unit. For example a feature allows to define a bundle together with OSGi configurations - which ensures that whenever this feature is used, the bundle *together with* the configurations are deployed. A repository can only describe the bundle as a separate resource and the OSGi configurations as additional unrelated resources.

The second difference is the handling of requirements and capabilities. While a repository is supposed to list all requirements and capabilities of a resource as part of the description, the feature model does not require this. As the feature model refers to the bundle and the bundle has the requirements and capabilities as metadata, there is no need to repeat that information.

By these two differences you can already tell, that a repository contents is usually generated by tools while a feature is usually a human created resource. While it is possible to create a repository index out of a feature, the other way round does not work as the repository has no standard way to define relationships between resources.

However, the approaches can of course be combined. If a feature has requirements which are not fulfilled by the feature itself, a repository can be used to find the matching capabilities.
