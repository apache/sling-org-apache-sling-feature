# API Regions

If you're assembling a platform (in contrast to a final application) out of several features and provide this platform for customers to build their application on top of, additional control of the API provided by the platform is needed. The bundles within the features provide all kinds of APIs but you might not want to expose all of these as extension points. You would rather want to use some of them internally within either a single feature or share within your platform features.

This is a proposal about how to add such additional metadata to the feature model. An API Regions runtime component enforces the rules.

# Visibility of API

A feature exports some api, however there are different types of clients of the API:

* Bundles shipped as part of the platform
* Application bundles using the platform

We can generalize this by saying that API is either globally visible (to every client) or only visible to features within the same context. Usually this is referred to as a "region": The platform spawns its own region and a customer application has its own region, too. In theory there could be several customer applications running in the same framework on top of the platform, and each application has its own region.

Without any further information, API is globally visible by default. However, for platform features we want the opposite as we want to ensure that newly added API is not visible to all bundles by default. 

A feature can have an additional extension JSON named api-regions. The following example exposes some packages to the global region and an additional package to the platform region. Exports declared earlier in the api-regions array also apply to later elements in the array, so the `platform` region also contains all exports declared for the `global` region.

Note that the `global` region is a predefined region that exports the listed packages to everyone. Other region names can be chosen freely. Packages listed in these other regions are only exposed to bundles in features that are in the same region.

    "api-regions:JSON|optinal" : [
        {
            "name": "global",
            "exports": [
                "# Export Sling's resource API in the global region", 
                "org.apache.sling.resource.api",
                "org.apache.sling.resource.api.adapter",
                "org.apache.sling.resource.api.auth",
                "org.apache.sling.resource.api.request",
                "org.apache.sling.resource.api.resource"
            ]
        },{
            "name": "platform",
            "exports": [
                "# Export the scheduler API in the platform region.",
                "# All exports in earlier regions defined here also apply.",
                "org.apache.sling.commons.scheduler"
            ]
        }
    ]

Of course the above mentioned packages need to be exported by some bundle within the feature.
By exporting packages to a given region, a feature automatically also sees all packages available to that region (or regions).

A feature can also just consume packages from a region, without having to export any packages to it. This can be done by exporting an empty list of packages. For example:

    "api-regions:JSON|optional" : [ 
        {
            "name": "platform",
            "exports": []
        }
    ]

If the api-regions extension is missing or the api-regions information is missing, it is assumed that all packages are exported to the "global" region and all packages in the global region are visible to the feature.

If a feature exports no packages and only wants to have visibility of packages from the global region, this can be specified as follows:
    
    "api-regions:JSON|optional" : [ 
        {
            "name": "global",
            "exports": []
        }
    ]

To support feature inheritance, the custom extension handler must be registered which will merge the extension: https://github.com/apache/sling-org-apache-sling-feature-extension-apiregions .

A number of API region related analysers/validators exist. Documentation can be found here: https://github.com/apache/sling-org-apache-sling-feature-analyser . These can be run as part of the analyse-features goal with the slingfeature-maven-plugin: https://github.com/apache/sling-slingfeature-maven-plugin#analyse-features  
