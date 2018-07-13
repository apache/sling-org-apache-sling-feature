# API Controller and API Regions

If you're assembling a platform (in contrast to a final application) out of several features and provide this platform for customers to build their application on top if, an additional control of the API provided by the platform is needed. The bundles within the features provide all kinds of APIs but you might not want to expose all of these as extension points but rather want to use some of it internally within either a single feature or share within your features.

This is a proposal about how to add such additional metadata to the feature model. An API controller at runtime enforces the rules.

# Visibility of API

A feature exports some api, however there are different types of clients of the API:

* Bundles shipped as part of the platform
* Application bundles using the platform

We can generalize this by saying that API is either globally visible (to every client) or only visible to features within the same context. Usually this is referred to as a "region": The platform spawns its own region and a customer application has its own region, too. In theory there could be several customer applications running in the same framework on top of the platform, and each application has its own region.

Without any further information, API is globally visible by default. However, for platform features we want the opposite as we want to ensure that newly added API is not world-wide visible by default. Therefore we'll add an additional build time check (analyzer) that checks that each platform feature has an api controller configuration as below.

A feature can have an additional extension JSON named api-regions. The following example exposes some packages to the global region and an additional package to the platform region. Exports declared earlier in the api-regions array also apply to later elements in the array, so the platform region also contains all exports declared for the global region.

Note that the 'global' region is a predefined region that exports the listed packages to everyone. Other region names can be chosen freely. Packages listed in these other regions are only exposed to bundles in features that are in the same region.

    "api-regions:JSON|false" : [
        {
            "name": "global",
            "exports": [
                // Export Sling's resource API in the global region.
                "org.apache.sling.resource.api",
                "org.apache.sling.resource.api.adapter",
                "org.apache.sling.resource.api.auth",
                "org.apache.sling.resource.api.request",
                "org.apache.sling.resource.api.resource"
            ]
        },{
            "name": "platform",
            "exports": [
                // Export the scheduler API in the platform region.
                // All exports in earlier regions defined here also apply.
                "org.apache.sling.commons.scheduler"
            ]
        }
    ]

Of course the above mentioned packages need to be exported by some bundle within the feature.
By exporting packages to a given region, a feature automatically also sees all packages available to that region (or regions).

A feature can also just consume packages from a region, without having to export any packages to it. This can be done by just mentioning the region name. For example:

    "api-regions:JSON|false" : [ "platform" ]

If the api-regions extension is missing or the api-regions information is missing, it is assumed that all packages are exported to the "global" region and all packages in the global region are visible to the feature.

If a feature exports no packages and only wants to have visibility of packages from the global region, this can be specified as follows:
    
    "api-regions:JSON|false" : [ "global" ]

To support feature inheritance, a custom extension handler must be registered which will merge the extension - if the inherited one and the target feature use a different region, this is considered an error. If they have the same region, the packages are merged. Of course the inheriting feature can remove this extension before processing. In addition the extension handler must mark each bundle with the region, otherwise this relationship gets lost later on when the application is build.
