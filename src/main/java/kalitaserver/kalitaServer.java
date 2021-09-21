package kalitaserver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.*;

import java.nio.charset.StandardCharsets;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.HttpURLConnection;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Properties;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.github.pemistahl.lingua.api.*;

public class kalitaServer {

	public static void main(String[] args) throws Exception {
		kalitaServer server = new kalitaServer();

		String server_address = "127.0.0.1";
		int port = 8080;

		Map<String, String> config = server.getConfig();
		for (String key : config.keySet()){
			if(key.equals("server_address")) {
				server_address = config.get(key);
			}
			if(key.equals("port")) {
				port = Integer.parseInt(config.get(key));
			}
		}
		try {
			InetSocketAddress address = new InetSocketAddress(server_address, port);
			HttpServer httpServer = HttpServer.create(address, 0);
			System.out.println("Http server started at " + address);
			httpServer.createContext("/speak", new GetHandler());
			httpServer.setExecutor(null);
			httpServer.start();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	static class GetHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange he) throws IOException {
			// parse request
			URI requestedUri = he.getRequestURI();
			String query = requestedUri.getRawQuery();

			Map<String,List<String>> requestHeaders = he.getRequestHeaders();

			he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			he.getResponseHeaders().add("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-*, Accept, Authorization");
			he.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
			he.getResponseHeaders().add("Access-Control-Allow-Methods", "POST,GET");
			he.getResponseHeaders().add("Max-Age-Seconds", "3000");
			he.getResponseHeaders().add("Accept-Ranges", "bytes");

			try {
				String range = "";
				for (String key : requestHeaders.keySet()){
					if(key.equals("Range")) {
						range = requestHeaders.get(key).get(0);
					}
				}

				String text = "";
				String language = "";
				Map<String, String> parameterMap = splitQuery(query);
				for (String key : parameterMap.keySet()){
					if(key.equals("text")) {
						text = parameterMap.get(key);
					}
					if(key.equals("language")) {
						language = parameterMap.get(key);
					}
				}

				if(text.length() > 0) {
					final LanguageDetector detector = LanguageDetectorBuilder.fromLanguages(Language.ENGLISH, Language.FRENCH, Language.GERMAN).build();
					final Language detectedLanguage = detector.detectLanguageOf(text);
					System.out.println("Detected Language: " + detectedLanguage.toString());
					if(language.equals("")) {
						language = detectedLanguage.toString().toLowerCase();
					}
					byte[] response = tts(text, language);
					int length = response.length;
					if(!range.equals("")) {
						String[] ranges = range.substring("bytes=".length()).split("-");
						int from = Integer.valueOf(ranges[0]);
						int to = response.length-1;
						if(ranges.length > 1) {
							to = Integer.valueOf(ranges[1]);
						}
						response = Arrays.copyOfRange(response, from, to);
						if(from > 44) {
							response = addWavHeader(response);
						}
						he.getResponseHeaders().add("Content-Range", "bytes " + from + "-" + to + "/" + length);
						he.getResponseHeaders().add("Content-Length", String.valueOf(response.length));

					}

					DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss");  
					LocalDateTime time = LocalDateTime.now();  
					he.getResponseHeaders().add("Content-Disposition", "attachment; filename=" + dtf.format(time) + "-speak.wav");

					he.sendResponseHeaders(200, response.length);
					OutputStream os = he.getResponseBody();
					os.write(response);
					os.close();
				} else {
					String response = "Please provide a text string that should be converted to audio.";
					he.sendResponseHeaders(400, response.length());

					OutputStream os = he.getResponseBody();
					os.write(response.getBytes());
					os.close();
				}
				

			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	private static byte[] addWavHeader(byte[] bytes) throws IOException {

		ByteBuffer bufferWithHeader = ByteBuffer.allocate(bytes.length + 44);
		bufferWithHeader.order(ByteOrder.LITTLE_ENDIAN);
		bufferWithHeader.put("RIFF".getBytes());
		bufferWithHeader.putInt(bytes.length + 36);
		bufferWithHeader.put("WAVE".getBytes());
		bufferWithHeader.put("fmt ".getBytes());
		bufferWithHeader.putInt(16);
		bufferWithHeader.putShort((short) 1);
		bufferWithHeader.putShort((short) 1);
		bufferWithHeader.putInt(16000);
		bufferWithHeader.putInt(32000);
		bufferWithHeader.putShort((short) 2);
		bufferWithHeader.putShort((short) 16);
		bufferWithHeader.put("data".getBytes());
		bufferWithHeader.putInt(bytes.length);
		bufferWithHeader.put(bytes);
		return bufferWithHeader.array();
	}

	public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
		Map<String, String> query_pairs = new LinkedHashMap<String, String>();
		String[] pairs = query.split("&");

		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
		}
		return query_pairs;
	}

	static byte[] tts(String inputText, String language) throws IOException {
		String text = URLEncoder.encode(inputText, StandardCharsets.UTF_8).replace("+","%20");
		
		String GET_URL = "http://localhost:1337/api/tts?text=%s&language=%s";

		GET_URL = String.format(GET_URL, text, language);

		URL obj = new URL(GET_URL);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		int responseCode = con.getResponseCode();
		System.out.println("GET Response Code :: " + responseCode);
		if (responseCode == HttpURLConnection.HTTP_OK) { // success
			InputStream in = null;
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			try {
				in = con.getInputStream();
				byte[] buffer = new byte[1024];
				int length;
				while ((length = in.read(buffer)) != -1) {
					bout.write(buffer, 0, length);
				}
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException ignored) {
					}
				}
			}
			return bout.toByteArray();
		} else {
			System.out.println("GET request failed");
			return null;
		}
	}
 
	private Map<String, String> getConfig() throws IOException {
		Map<String, String> properties = new HashMap<String, String>();

		try {
			Properties prop = new Properties();
			String configFileName = System.getProperty("user.dir") + "\\config.properties";

			InputStream input = new FileInputStream(configFileName);

			if (input != null) {
				prop.load(input);
			} else {
				throw new FileNotFoundException("Config file '" + configFileName + "' not found in the classpath");
			}

			// get the property value and print it out
			properties.put("server_address", prop.getProperty("server_address"));
			properties.put("port", prop.getProperty("port"));
			input.close();
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}
		return properties;
	}
}
