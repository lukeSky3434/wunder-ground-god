package luke.sky.wunderground;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.glassfish.jersey.client.ClientProperties;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author pendl2
 */
public class RealTimeDataReader implements Runnable
{
	private static final String PATH = "https://api.weather.com/v2/pws/observations/current";
	private static final String ENV_LOG_LEVEL = "LOG.LEVEL";
	private static final String ENV_POLL_TIME = "POLL.TIME.MS"; // default 10000
	private static final String ENV_DEVICE_ID = "DEVICE.ID"; // must be set
	private static final String ENV_API_KEY = "API.KEY"; // must be set
	private static final String ENV_UNIT = "UNIT"; // default: m, e = english units, m = metric units, h = hybrid units

	private final Client client;
	private final WebTarget webTarget;
	private static final Logger LOG = LogManager.getLogger(RealTimeDataReader.class);
	private long pollTime;
	private String deviceId;
	private String apiKey;
	private String unit;

	private static List<String> IGNORE = new ArrayList<>(Arrays.asList("epoch", "obsTimeUtc", "obsTimeLocal"));

	private void initialize()
	{
		Configurator.initialize(new DefaultConfiguration());
		String level = System.getenv(ENV_LOG_LEVEL);
		Level ll = Level.getLevel(level == null ? Level.INFO.name() : level);
		Configurator.setRootLevel(ll);

		final long defaultMs = 10000L;
		String timeMs = System.getenv(ENV_POLL_TIME);
		try {
			pollTime = Long.parseLong(timeMs);
		}
		catch (NumberFormatException x) {
			LOG.warn("<{}> is not a number, taking default value <{}>", timeMs, defaultMs);
			pollTime = defaultMs;
		}

		deviceId = System.getenv(ENV_DEVICE_ID);
		apiKey = System.getenv(ENV_API_KEY);
		unit = System.getenv(ENV_UNIT);
		if (Objects.isNull(unit)) {
			unit = "m";
		}

		if (Objects.isNull(deviceId) || Objects.isNull(apiKey)) {
			throw new ExceptionInInitializerError(ENV_DEVICE_ID + " and " + ENV_API_KEY + " must be set");
		}
	}

	public RealTimeDataReader()
	{
		initialize();

		client = ClientBuilder.newClient();
		client.property(ClientProperties.CONNECT_TIMEOUT, 2000);
		client.property(ClientProperties.READ_TIMEOUT, 2000);

		LOG.info("initialization done");

		URI hostUri = UriBuilder.fromUri(PATH).build();

		webTarget = client
			.target(UriBuilder.fromUri(hostUri)
				.queryParam("stationId", deviceId)
				.queryParam("format", "json")
				.queryParam("units", unit)
				.queryParam("apiKey", apiKey)
				.build());
	}

	public static void main(String[] args) throws Exception
	{
		try {
			Thread th = new Thread(new RealTimeDataReader());
			th.setName("ReaderThread");
			th.start();
		}
		catch (Exception x) {
			LOG.error("caught exception", x);
			throw x;
		}
	}

	@Override
	public void run()
	{
		DbAdapter dbAdpater = new InfluxAdapter();

		while (true) {
			LOG.info("doing webservice call - uri <" + webTarget.getUri() + ">");
			try (Response response = webTarget
				.request()
				.get()) {
				String entity = response.readEntity(String.class);

				JSONObject obj = new JSONObject(entity);
				JSONArray observations = obj.getJSONArray("observations");

				for (int i = 0; i < observations.length(); i++) {
					JSONObject item = observations.getJSONObject(i);
					List<Dimension> dim = new ArrayList<>();
					Map<String, Long> metrics = new HashMap<>();
					for (String k : item.keySet()) {
						if (IGNORE.contains(k)) {
							// IGNORE
						}
						else if ("imperial".equals(k) || "metric".equals(k) || "uk_hybrid".equals(k)) {
							JSONObject imp = item.getJSONObject(k);
							for (String impK : imp.keySet()) {
								metrics.put(impK, imp.optLong(impK));
							}
						}
						else {
							Object v = item.get(k);
							if (!item.isNull(k)) {
								try {
									dim.add(new Dimension(k, (String) v));
								}
								catch (ClassCastException x) {
									BigDecimal d = item.getBigDecimal(k);
									dim.add(new Dimension(k, d.toString()));
								}
							}
						}
					}
					metrics.keySet().forEach(k -> {
						dbAdpater.sendMeasurement(k, k, metrics.get(k), dim);
					});
				}
			}
			catch (Exception x) {
				LOG.error("error while processing data", x);
			}
			try {
				LOG.info("sleep {} milliseconds ...", pollTime);

				Thread.sleep(pollTime);
			}
			catch (Exception e) {
				LOG.error("error while sleeping", e);
			}
		}
	}
}
