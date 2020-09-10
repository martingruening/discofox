package eu.gruning.discofox.apiclient;

import static java.nio.charset.CodingErrorAction.REPORT;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.github.scribejava.core.utils.StreamUtils;
import com.google.gson.Gson;

/**
 * Client for the Discovergy API (<a href=
 * "https://api.discovergy.com/docs/">https://api.discovergy.com/docs/</a>)
 */
public class DiscovergyApiClient {

	/**
	 * Unique client id
	 */
	private final String clientId;

	private final DiscovergyApi api;

	private final OAuth10aService authenticationService;
	private final OAuth1AccessToken accessToken;

	public DiscovergyApiClient(String clientId) throws InterruptedException, ExecutionException, IOException {
		this(createDiscovergyApi(), clientId);
	}

	public DiscovergyApiClient(DiscovergyApi api, String clientId)
			throws InterruptedException, ExecutionException, IOException {
		this.api = api;
		this.clientId = clientId;
		Map<String, String> consumerTokenEntries = getConsumerToken();
		authenticationService = new ServiceBuilder(consumerTokenEntries.get("key"))
				.apiSecret(consumerTokenEntries.get("secret")).build(api);
		OAuth1RequestToken requestToken = authenticationService.getRequestToken();
		String authorizationURL = authenticationService.getAuthorizationUrl(requestToken);
		String verifier = authorize(authorizationURL);
		accessToken = authenticationService.getAccessToken(requestToken, verifier);
	}

	private static DiscovergyApi createDiscovergyApi() throws IOException {
		File file = new File("credentials.properties").getAbsoluteFile();
		Properties properties = new Properties();
		try (Reader reader = new InputStreamReader(new FileInputStream(file),
				UTF_8.newDecoder().onMalformedInput(REPORT).onUnmappableCharacter(REPORT))) {
			properties.load(reader);
		} catch (IOException e) {
			throw new IOException("Failed to read credentials from file " + file, e);
		}
		String email = properties.getProperty("email");
		String password = properties.getProperty("password");
		if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
			throw new RuntimeException("The properties \"email\" and \"password\" must be set in file " + file);
		}
		return new DiscovergyApi(email, password);
	}

	public DiscovergyApi getApi() {
		return api;
	}

	public OAuthRequest createRequest(Verb verb, String endpoint)
			throws InterruptedException, ExecutionException, IOException {
		return new OAuthRequest(verb, api.getBaseAddress() + endpoint);
	}

	public Response executeRequest(OAuthRequest request) throws InterruptedException, ExecutionException, IOException {
		authenticationService.signRequest(accessToken, request);
		return authenticationService.execute(request);
	}

	public Response executeRequest(OAuthRequest request, int expectedStatusCode)
			throws InterruptedException, ExecutionException, IOException {
		Response response = executeRequest(request);
		if (response.getCode() != expectedStatusCode) {
			response.getBody();
			throw new RuntimeException("Status code is not " + expectedStatusCode + ": " + response);
		}
		return response;
	}

	private Map<String, String> getConsumerToken() throws IOException {
		byte[] rawRequest = ("client=" + clientId).getBytes(StandardCharsets.UTF_8);
		HttpURLConnection connection = getConnection(api.getBaseAddress() + "/oauth1/consumer_token", "POST", true,
				true);
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
		connection.setRequestProperty("Content-Length", Integer.toString(rawRequest.length));
		connection.connect();
		connection.getOutputStream().write(rawRequest);
		connection.getOutputStream().flush();
		String content = StreamUtils.getStreamContents(connection.getInputStream());
		connection.disconnect();

		return new Gson().fromJson(content, Map.class);
	}

	private static String authorize(String authorizationURL) throws IOException {
		HttpURLConnection connection = getConnection(authorizationURL, "GET", true, false);
		connection.connect();
		String content = StreamUtils.getStreamContents(connection.getInputStream());
		connection.disconnect();
		return content.substring(content.indexOf('=') + 1);
	}

	private static HttpURLConnection getConnection(String rawURL, String method, boolean doInput, boolean doOutput)
			throws IOException {
		URL url = new URL(rawURL);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoInput(doInput);
		connection.setDoOutput(doOutput);
		connection.setRequestMethod(method);
		connection.setRequestProperty("Accept", "*");
		connection.setInstanceFollowRedirects(false);
		connection.setRequestProperty("charset", "utf-8");
		connection.setUseCaches(false);
		return connection;
	}
}
