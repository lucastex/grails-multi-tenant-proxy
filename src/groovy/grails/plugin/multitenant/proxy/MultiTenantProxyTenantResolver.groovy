package grails.plugin.multitenant.proxy

import net.sf.ehcache.Cache
import net.sf.ehcache.Element
import net.sf.ehcache.CacheManager

import org.apache.log4j.Logger

import javax.servlet.http.HttpServletRequest

import com.infusion.util.domain.event.HibernateEvent
import com.infusion.util.event.groovy.GroovyEventBroker

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

import org.codehaus.groovy.grails.commons.ConfigurationHolder

import grails.plugin.multitenant.core.TenantResolver
import grails.plugin.multitenant.core.InvalidTenantException

class MultiTenantProxyTenantResolver implements TenantResolver, ApplicationContextAware {

    private static Logger log = Logger.getLogger(MultiTenantProxyTenantResolver.class)

	def hosts = [:]
	def loaded = false
	
	GroovyEventBroker eventBroker
	ApplicationContext applicationContext
	static String TENANT_DATA_CACHE_NAME = "MultiTenantDomainData"	
	
	public Integer getTenantFromRequest(HttpServletRequest inRequest) {
		
        if (!loaded) {
            initialize()
            loaded = true
        }

		String headerName = "x-forwarded-host"
		String currentServerName = inRequest.getServerName()
		if (inRequest.getHeader(headerName) != null) {
			currentServerName = inRequest.getHeader(headerName)
			log.debug("http request URL ["+currentServerName+"] decoded using header ["+headerName+"] from request")
		}

        Integer tenantId = hosts.get(currentServerName)
        if (tenantId == null) {
            log.fatal("Could not decode valid tenant id from request server " + currentServerName)
            throw new InvalidTenantException("Could not decode mapped tenant id from request server name " + currentServerName)
        }

        if (log.isDebugEnabled()) {
            log.debug("Decoded tenant id " + tenantId + " from http request URL " + currentServerName)
        }

        return tenantId
    }

    void initialize() {
        log.info "Initializing Domain Name Map for Multi Tenant support"
        loadDomainTenantMap()
    }
	
	void loadDomainTenantMap() {
        
        hosts.clear()
        Cache tenantDataCache = CacheManager.getInstance()?.getCache(TENANT_DATA_CACHE_NAME)
        if (tenantDataCache == null) {
            
            // This insures the cache has no limit and items are never removed from the cache.
            CacheManager.getInstance().addCache(new Cache(TENANT_DATA_CACHE_NAME, 1000, true, true, 120, 120))
      		tenantDataCache = CacheManager.getInstance()?.getCache(TENANT_DATA_CACHE_NAME)
    	} else {
      		tenantDataCache.removeAll()
    	}

    	// See if there is a custom data source bean name
    	def domainTenantBeanName = ConfigurationHolder.config.tenant.domainTenantBeanName
    	if (domainTenantBeanName == null || domainTenantBeanName?.size() == 0) {
      		domainTenantBeanName = "tenant.DomainTenantMap"
    	}

    	// Load the tenant information for the domain to tenant id from the database.
    	def list = applicationContext.getBean(domainTenantBeanName).list()
    	log.info "Loading " + list?.size() + " Domain Name to Mapped Tenant Id entries from the database object " + domainTenantBeanName
    	list.each {map ->
      		if (log.isDebugEnabled()) {
        		log.debug "Domain->Tenant: ${map.domainName}->${map.mappedTenantId}"
      		}
      		hosts.put(map.domainName, map.mappedTenantId)
      		tenantDataCache.put(new Element(map.mappedTenantId, map))
    	}
  	}
 
 	/**
   	* The event broker listens for every time a record is saved, then calls a refresh on the
   	* list of hosts whenever the DomainTenantMap object is changed via a save or update.
   	*/
  	void setEventBroker(GroovyEventBroker inEventBroker) {
    	
		if (inEventBroker != null) {
      		
			inEventBroker.subscribe("hibernate.${HibernateEvent.saveOrUpdate}.DomainTenantMap") { event, broker ->
        		log.info "DomainTenantMap was changed via a save or update. Reloading the Domain to Tenant information from the database."
        		this.initialize()
      		}
    	}
  	}
}