package life.genny.shleemy.utils;

import java.io.IOException;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

import life.genny.shleemy.models.GennyToken;
import life.genny.shleemy.models.QMessage;

public class WriteToBridge {
	 private static final Logger log = Logger.getLogger(WriteToBridge.class);	

		public static String writeMessage(String bridgeUrl, QMessage msg, final GennyToken userToken) {

			Jsonb jsonb = JsonbBuilder.create();

			String entityString = jsonb.toJson(msg);
			return writeMessage(bridgeUrl, entityString, userToken);
		}
	 
	public static String writeMessage(String bridgeUrl, String entityString, final GennyToken userToken) {


		String responseString = null;
		CloseableHttpClient httpclient = HttpClientBuilder.create().build();
		CloseableHttpResponse response = null;
		try {

			HttpPost post = new HttpPost(bridgeUrl + "?channel=webdata");

			StringEntity postEntity = new StringEntity(entityString, "UTF-8");

			post.setEntity(postEntity);
			post.setHeader("Content-Type", "application/json; charset=UTF-8");
			if (userToken != null) {
				post.addHeader("Authorization", "Bearer " + userToken.getToken()); // Authorization": `Bearer
			}

			response = httpclient.execute(post);
			HttpEntity entity = response.getEntity();
			responseString = EntityUtils.toString(entity);
			return responseString;
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (response != null) {
				try {
					response.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				log.error("postApi response was null");
			}
			try {
				httpclient.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return responseString;
	}
}
