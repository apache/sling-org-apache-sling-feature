# Provisioning Applications Proposal

There are use case where you want to describe a whole application based on features. In addition some parts might to be optional. In the below proposal we use the term "option" and "choice" which in the past is referred to as a runmode in the Sling world. To distinguish these, we use more general terms in this proposal.

The two proposals basically differ in where the knowledge whether a feature should be included is stored: does the feature itself know when it should be included (proposal 2) or is this knowledge outside of the feature (proposal 1).

## Proposal 1 : Additional Configuration

A application configuration for such use cases could look like this:

    {
        // this is a list of required features
        "application" : [
            "org.apache.sling:org.apache.sling.launchpad:10"
        ],
        // this is a list of optional features
        "repository" : [
            "org.apache.sling:org.apache.sling.portal:1.0."
        ],
        // choices provide two or more names which are mutually exclusive
        // like "author" and "publish" in the example below
        "choices" : [
            [                 
                {
                    "name" : "author",
                    "features" : [
                        "org.apache.sling:org.apache.sling.cms:1.0.0"                                    
                    ],
                    "variables" : {
                        "service_id" : "author"
                    },
                    options : {
                        "jsp" : true
                    }
                },
                {
                    "name" : "publish",
                    "variables" : {
                        "service_id" : "publish"
                    },
                    "features" : [
                        "org.apache.sling:org.apache.sling.cache:1.3.6"                                    
                    ],
                }
            ],
        ]
        "options" : {
           "jsp" : {
               // this feature is only included if the jsp option is selected
               "features" : [
                   "org.apache.sling:org.apache.sling.scripting.jsp:1.0.0"                   
               ]
           },
           "samplecontent" : {
               "choices" : {
                    // the key refers to a comma separated list of choices that need to be selected in order to
                    // include the below features
                    "author" : {
                        "features" : [
                            "org.apache.sling:org.apache.sling.samples.author:1.0.0"
                        ]
                    },
                    "publish" : {
                        "features" : [
                            "org.apache.sling:org.apache.sling.samples.publish:1.0.0"
                        ]
                    }
                }
            }
        },
        "defaults" : {
            "options": []
        },
        "framework" : {
            "id" : "org.apache.felix:org.apache.felix.framework:6.0.0"
        }
    }

The *application* section list all features that are always used for the application. The features listed in the repository section are only pulled in if another included feature has a requirement for them.

The options section contains labeled options. An option has a list of features and optionally an activation section which either enables or disables other options. Options can be nested, allowing for sub options.

An option can also set additional variables - which in turn can be used within the included features.

The defaults section specifies the options activated by default and the framework section defines the framework to be used.

## Proposal 2 : Requirements Based

Instead of maintaining an additional configuration a feature could itself define when it should be activated. Therefore for example a feature which should be bound to the author could have a requirement on the "option" "author". Similar with publish based features. As there is no limit on requirements, a feature can have a requirement on more than one option. These options could be set through some runtime switch similar to how we enable run modes today.

If a 3rd party feature should be used in this way but does not declare such a requirement, the inheritance mechanism could be used to create a new feature inheriting that one and adding the requirement.

It would still be beneficial to have a variable mechanism as shown with proposal 1 to set a variable to either "author" or "publish" to be reused within a feature.
