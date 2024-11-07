package dzervas.classucker;

import fi.iki.elonen.NanoHTTPD;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Agent {
	public static void premain(String agentArgs, final Instrumentation inst) {
		System.out.println("ClasSucker injected");

		try {
		FileWriter myWriter = new FileWriter("C:\\Users\\User\\Desktop\\test.txt");
		myWriter.write("Files in Java might be tricky, but it is fun enough!" + new Date() + "\n");
		myWriter.close();
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		int port = 8080; // Default port
		if (agentArgs != null && !agentArgs.isEmpty()) {
			try {
				port = Integer.parseInt(agentArgs);
			} catch (NumberFormatException e) {
				System.err.println("Invalid port number, using default port 8080.");
			}
		}

		final int finalPort = port;
		System.out.println("Starting server on " + finalPort);

		new Thread(() -> {
			try {
				new ClassListingServer(finalPort, inst).start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
				System.out.println("ClassListingAgent web server started on port " + finalPort);
			} catch (IOException e) {
				System.err.println("Failed to start web server: " + e.getMessage());
			}
		}).start();
	}

	private static class ClassListingServer extends NanoHTTPD {
		private final Instrumentation instrumentation;

		public ClassListingServer(int port, Instrumentation inst) {
			super(port);
			this.instrumentation = inst;
		}

		@Override
		public Response serve(IHTTPSession session) {
			String uri = session.getUri();
			if (uri.equals("/")) {
				return newFixedLengthResponse(Response.Status.OK, "text/html", generateClassListPage());
			} else if (uri.startsWith("/download/")) {
				String className = uri.substring("/download/".length()).replace('/', '.');
				return serveClassFile(className);
			} else {
				return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
			}
		}

		private String generateClassListPage() {
			StringBuilder html = new StringBuilder("<html><body>");
			html.append("<h1>Loaded Classes</h1>");
			Map<String, Set<String>> packageMap = getClassPackageMap();
			for (Map.Entry<String, Set<String>> entry : packageMap.entrySet()) {
				html.append("<h2>").append(entry.getKey()).append("</h2><ul>");
				for (String className : entry.getValue()) {
					String downloadLink = "/download/" + className;
					html.append("<li><a href=\"").append(downloadLink).append("\">")
						.append(className).append("</a></li>");
				}
				html.append("</ul>");
			}
			html.append("</body></html>");
			return html.toString();
		}

		private Map<String, Set<String>> getClassPackageMap() {
			Map<String, Set<String>> packageMap = new TreeMap<String, Set<String>>();
			for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
				String className = clazz.getName();
				String packageName = "";
				int lastDot = className.lastIndexOf('.');
				if (lastDot != -1) {
					packageName = className.substring(0, lastDot);
				}
				if (!packageMap.containsKey(packageName)) {
					packageMap.put(packageName, new TreeSet<String>());
				}
				packageMap.get(packageName).add(className);
			}
			return packageMap;
		}

		private Response serveClassFile(String className) {
			try {
				Class<?> clazz = Class.forName(className);
				String resourcePath = "/" + className.replace('.', '/') + ".class";
				InputStream classStream = clazz.getResourceAsStream(resourcePath);
				if (classStream == null) {
					return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Class " + className + " was not found");
				}
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				byte[] data = new byte[1024];
				int nRead;
				while ((nRead = classStream.read(data, 0, data.length)) != -1) {
					buffer.write(data, 0, nRead);
				}
				buffer.flush();
				byte[] classData = buffer.toByteArray();
				return newFixedLengthResponse(Response.Status.OK, "application/java-vm", new ByteArrayInputStream(classData), classData.length);
			} catch (ClassNotFoundException | IOException e) {
				return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error retrieving class file");
			}
		}
	}
}
