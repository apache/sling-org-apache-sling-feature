# Provisioning Applications Proposal

There are use case where you want to describe a whole application based on features. In addition some parts might to be optional.

A application configuration for such use cases could look like this:

    {
        "application" : [
            "org.apache.sling:org.apache.sling.launchpad:10"
        ],
        "repository" : [
            "org.apache.sling:org.apache.sling.portal:1.0."
        ],
        "options" : {
           "jsp" : {
               "features" : [
                   "org.apache.sling:org.apache.sling.scripting.jsp:1.0.0"                   
               ]
           },
           "author" : {
               "features" : [
                   "org.apache.sling:org.apache.sling.cms:1.0.0"                                    
               ],
               "variables" : {
                   "service_id" : "author"
               },
               "activation" : {
                   "publish" : false,
                   "jsp" : true
               }
           },
           "publish" : {
               "features" : [
                   "org.apache.sling:org.apache.sling.cache:1.3.6"                                    
               ],
               "variables" : {
                   "service_id" : "publish"
               },
               "activation" : {
                   "author" : false                
               }
           },
           "samples" : {
               "options" : {
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
            "options": ["author"]
        },
        "framework" : {
            "id" : "org.apache.felix:org.apache.felix.framework:6.0.0"
        }
    }

The *application* section list all features that are always used for the application. The features listed in the repository section are only pulled in if another included feature has a requirement for them.

The options section contains labeled options. An option has a list of features and optionally an activation section which either enables or disables other options. Options can be nested, allowing for sub options.

An option can also set additional variables - which in turn can be used within the included features.

The defaults section specifies the options activated by default and the framework section defines the framework to be used.
