class MultiTenantProxyGrailsPlugin {

    def version = "1.0"

    def grailsVersion = "1.3.6 > *"
  	def dependsOn = [multiTenantCore: "1.0.0 > *"]
	def loadAfter = ['multiTenantCore']

    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Lucas Teixeira"
    def authorEmail = "lucastex@gmail.com"
    def title = "Multi-Tenant Proxy Tenant Resolver"
    def description = "Multi-tenant add-on to use domain tenant resolver with http proxy in front of your app server"

    def documentation = "http://github.com/blanq/grails-multi-tenant-proxy"

    def doWithWebDescriptor = { xml ->
    }

    def doWithSpring = {
	
		tenantResolver(grails.plugin.multitenant.proxy.MultiTenantProxyTenantResolver)
    }

    def doWithDynamicMethods = { ctx ->
    }

    def doWithApplicationContext = { applicationContext ->
    }

    def onChange = { event ->
    }

    def onConfigChange = { event ->
    }

}
