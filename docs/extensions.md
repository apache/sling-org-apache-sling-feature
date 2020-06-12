# Available Feature Extensions

The Feature Model is extensible, meaning that it can be augmented with custom content in a number of ways. Some extensions are supported out of the box. Other extensions are available through additional modules.

## Built-in extension: content-packages

This extension of type `ARTIFACTS` allows listing content packages which will
be installed by the launcher. Example:

```
"content-packages:ARTIFACTS|required":[
    "org.apache.sling.myapp:my-content-package:zip:1.0.0"
]
```

## Built-in extension: repoinit

This extension is of type `TEXT`. It allows the specification of Sling Repository
Initialization statements which will be executed on the repository at startup.
Example:

```
"repoinit:TEXT|required":[
  "create path /content/example.com(mixin mix:referenceable)",
  "create path (nt:unstructured) /var"
]
```

As initializing the repository is usually important for Sling based applications
the extension should be marked as required as in the example above.

## Built-in extension: execution-environment

This extension is of type `JSON` and allows to specify the execution environment for a feature.
Right now it only supports to set the artifact for the framework to launch the feature. The framework can either be specified as a string or a JSON object with an `id` property:

The execution environment - if provided - is used by tooling like the feature launcher or the feature analysers.

```
"execution-environment:JSON|optional" : {
  "framework" : {
    "id" : "org.apache.felix:org.apache.felix.framework:6.0.3"
  }
}
```


## Further extensions

* [API Controller and API Regions](https://github.com/apache/sling-org-apache-sling-feature-extension-apiregions/blob/master/docs/api-regions.md)
