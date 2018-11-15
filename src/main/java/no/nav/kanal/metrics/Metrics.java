package no.nav.kanal.metrics;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.component.metrics.routepolicy.MetricsRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;

public class Metrics implements CamelContextAware {

	private static Logger log = LoggerFactory.getLogger(Metrics.class);
	
	private CamelContext camelContext;
	
	public String getMetrics() {

		MetricsRegistryService registryService = camelContext.hasService(MetricsRegistryService.class);
		if (registryService != null) {
			log.debug("registryService: " + registryService);
			String metricsJson = registryService.dumpStatisticsAsJsonTimeUnitSeconds();
			log.debug("Json dump of registryService: " + metricsJson);
			MetricRegistry metricsRegistry = registryService.getMetricsRegistry();
			log.debug("Metricsregistry from registryService: " + metricsRegistry);
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("sendDigitalPostStandard.antall", metricsRegistry.timer("sdpkanal.sendDigitalPostStandard.responses").getCount());
			map.put("sendDigitalPostStandard.mean", metricsRegistry.timer("sdpkanal.sendDigitalPostStandard.responses").getSnapshot().getMean());
			map.put("sendDigitalPostStandard.mean_kallprsekund", metricsRegistry.timer("sdpkanal.sendDigitalPostStandard.responses").getMeanRate());
			map.put("sendDigitalPostPrioritert.antall", metricsRegistry.timer("sdpkanal.sendDigitalPostPrioritert.responses").getCount());
			map.put("sendDigitalPostPrioritert.mean", metricsRegistry.timer("sdpkanal.sendDigitalPostPrioritert.responses").getSnapshot().getMean());
			map.put("sendDigitalPostPrioritert.mean_kallprsekund", metricsRegistry.timer("sdpkanal.sendDigitalPostPrioritert.responses").getMeanRate());
			map.put("dlqMeldingStandard.antall_BOQ", metricsRegistry.timer("sdpkanal.dlqMeldingStandard.responses").getCount());
			map.put("dlqMeldingPrioritert.antall_BOQ", metricsRegistry.timer("sdpkanal.dlqMeldingPrioritert.responses").getCount());
			map.put("ebmsPullNormal.antall", metricsRegistry.counter("ebmsPullNormal.antall").getCount());
			map.put("ebmsPullPrioritert.antall", metricsRegistry.counter("ebmsPullPrioritert.antall").getCount());
			String jsonMetrics = "";
			try {
				jsonMetrics = mapper.writeValueAsString(map);
				log.debug("Custom JSON Metrics" + jsonMetrics);
			} catch (IOException e) {
				log.error("Error during collection of metrics: " + e.getMessage());
			}
			return jsonMetrics;
			
		} else {
			log.warn("registryService is null. Cannot get metrics.");
			return null;
		}
	}
	
	@Override
	public void setCamelContext(CamelContext camelContext) {
		this.camelContext = camelContext;
	}

	@Override
	public CamelContext getCamelContext() {
		return camelContext;
	}
	
}
