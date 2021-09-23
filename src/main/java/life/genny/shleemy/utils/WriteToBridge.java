package life.genny.shleemy.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.shleemy.models.GennyToken;
import life.genny.shleemy.models.QMessage;

public class WriteToBridge {
	 private static final Logger log = Logger.getLogger(WriteToBridge.class);	

		public static String writeMessage(String bridgeUrl, final String channel,QMessage msg, final GennyToken userToken) {

			Jsonb jsonb = JsonbBuilder.create();

			String entityString = jsonb.toJson(msg);
			return writeMessage(bridgeUrl, channel,entityString, userToken);
		}
	 
	public static String writeMessage(String bridgeUrl, final String channel,String entityString, final GennyToken userToken) {
		ExecutorService executorService = Executors.newFixedThreadPool(20);
		HttpClient httpClient = HttpClient.newBuilder().executor(executorService)
				.version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofSeconds(20)).build();

		Integer httpTimeout = 7;  // 7 secnds
		String postUrl = bridgeUrl + "?channel="+channel;

		if (StringUtils.isBlank(postUrl)) {
			log.error("Blank url in apiPostEntity");
		}

		HttpRequest.BodyPublisher requestBody = HttpRequest.BodyPublishers.ofString(entityString);

		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().POST(requestBody).uri(URI.create(postUrl))
				.setHeader("Content-Type", "application/json")
				.setHeader("Authorization", "Bearer " + userToken);


		if (postUrl.contains("genny.life")) { // Hack for local server not having http2
			requestBuilder = requestBuilder.version(HttpClient.Version.HTTP_1_1);
		}

		HttpRequest request = requestBuilder.build();

		String result = null;
		Boolean done = false;
		int count = 1;
		while ((!done) && (count > 0)) {
			CompletableFuture<HttpResponse<String>> response = httpClient.sendAsync(request,
					java.net.http.HttpResponse.BodyHandlers.ofString());

			try {
				result = response.thenApply(java.net.http.HttpResponse::body).get(httpTimeout, TimeUnit.SECONDS);
				done = true;
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				// TODO Auto-generated catch block
				log.error("Count:" + count + ", Exception occurred when post to URL: "+ postUrl + ",Body is entityString:" + entityString + ", Exception details:"  + e.getCause() );
				// try renewing the httpclient
				httpClient = HttpClient.newBuilder().executor(executorService).version(HttpClient.Version.HTTP_2)
						.connectTimeout(Duration.ofSeconds(httpTimeout)).build();
				if (count <= 0) {
					done = true;
				}
			}
			count--;
		}
		return result;
	}
}
