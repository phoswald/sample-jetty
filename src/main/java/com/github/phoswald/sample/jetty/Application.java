package com.github.phoswald.sample.jetty;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;

import com.github.phoswald.sample.ConfigProvider;
import com.github.phoswald.sample.sample.EchoRequest;
import com.github.phoswald.sample.sample.EchoResponse;
import com.github.phoswald.sample.sample.SampleController;
import com.github.phoswald.sample.sample.SampleResource;
import com.github.phoswald.sample.task.TaskController;
import com.github.phoswald.sample.task.TaskRepository;
import com.google.gson.Gson;
import com.thoughtworks.xstream.XStream;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class Application {

    private static final Logger logger = LogManager.getLogger();
    private static final ConfigProvider config = new ConfigProvider();
    private static final int port = Integer.parseInt(config.getConfigProperty("app.http.port").orElse("8080"));
    private static final XStream xstream = new XStream(); // TODO configure security
    private static final Gson gson = new Gson();
    private static final EntityManagerFactory emf = createEntityManagerFactory();

    static {
        xstream.alias("EchoRequest", EchoRequest.class);
        xstream.alias("EchoResponse", EchoResponse.class);
    }

    public static void main(String[] args) throws Exception {
        logger.info("sample-jetty is starting, port=" + port);

        Server server = new Server(port);
        server.setHandler(routes( //
                files("/resources"), //
                get("/app/rest/sample/time", createHandler(() -> new SampleResource().getTime())), //
                get("/app/rest/sample/config", createHandler(() -> new SampleResource().getConfig())), //
                post("/app/rest/sample/echo-xml",
                        createXmlHandler(EchoRequest.class, reqBody -> new SampleResource().postEcho(reqBody))), //
                post("/app/rest/sample/echo-json",
                        createJsonHandler(EchoRequest.class, reqBody -> new SampleResource().postEcho(reqBody))), //
                get("/app/pages/sample", createHtmlHandler(() -> new SampleController().getSamplePage())), //
                get("/app/pages/tasks", createHtmlHandler(() -> createTaskController().getTasksPage())), //
                post("/app/pages/tasks",
                        createHtmlHandlerEx(params -> createTaskController().postTasksPage(params.get("title"),
                                params.get("description")))), //
                get("/app/pages/tasks/([0-9a-z-]+)",
                        createHtmlHandlerEx(params -> createTaskController().getTaskPage(params.get("1" /* "id" */),
                                params.get("action")))), //
                post("/app/pages/tasks/([0-9a-z-]+)",
                        createHtmlHandlerEx(params -> createTaskController().postTaskPage(params.get("1" /* "id" */),
                                params.get("action"), params.get("title"), params.get("description"),
                                params.get("done")))) //
        ));
        server.start();
    }

    static Handler routes(Handler... routes) {
        HandlerList handlers = new HandlerList();
        Arrays.asList(routes).forEach(handlers::addHandler);
        return handlers;
    }

    static Handler files(String classPath) {
        ResourceHandler handler = new ResourceHandler();
        handler.setBaseResource(Resource.newClassPathResource(classPath));
        handler.setDirectoriesListed(false);
        handler.setWelcomeFiles(new String[] { "index.html" });
        return handler;
    }

    static Handler get(String path, MyHandler handler) {
        return method(path, "GET", handler);
    }

    static Handler post(String path, MyHandler handler) {
        return method(path, "POST", handler);
    }

    static Handler method(String path, String method, MyHandler handler) {
        Pattern pattern = Pattern.compile("^" + path + "$");
        return new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request,
                    HttpServletResponse response) throws IOException, ServletException {
                Matcher matcher = pattern.matcher(target);
                if (matcher.matches() && Objects.equals(baseRequest.getMethod(), method)) {
                    logger.info("Handling {} {}", request.getMethod(), target);
                    Map<String, String> params = new HashMap<>();
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        params.put("" + i, matcher.group(i));
                    }
                    for (String paramName : request.getParameterMap().keySet()) {
                        params.put(paramName, request.getParameter(paramName));
                    }
                    handler.handle(params, request, response);
                    baseRequest.setHandled(true);
                }
            }
        };
    }

    static MyHandler createHandler(Supplier<String> callback) {
        return (Map<String, String> params, HttpServletRequest request, HttpServletResponse response) -> {
            response.getOutputStream().write(callback.get().getBytes(StandardCharsets.UTF_8));
        };
    }

    static <R> MyHandler createXmlHandler(Class<R> reqClass, Function<R, Object> callback) {
        return (Map<String, String> params, HttpServletRequest request, HttpServletResponse response) -> {
            response.setContentType("text/xml");
            response.getOutputStream()
                    .write(xstream.toXML(callback.apply(reqClass.cast(xstream.fromXML(request.getInputStream()))))
                            .getBytes(StandardCharsets.UTF_8));
        };
    }

    static <R> MyHandler createJsonHandler(Class<R> reqClass, Function<R, Object> callback) {
        return (Map<String, String> params, HttpServletRequest request, HttpServletResponse response) -> {
            response.setContentType("application/json");
            response.getOutputStream().write(gson
                    .toJson(callback.apply(gson.fromJson(new InputStreamReader(request.getInputStream()), reqClass)))
                    .getBytes(StandardCharsets.UTF_8));
        };
    }

    private static MyHandler createHtmlHandler(Supplier<String> callback) {
        return (Map<String, String> params, HttpServletRequest request, HttpServletResponse response) -> {
            response.setContentType("text/html");
            response.getOutputStream().write(callback.get().getBytes(StandardCharsets.UTF_8));
        };
    }

    private static MyHandler createHtmlHandlerEx(Function<Map<String, String>, String> callback) {
        return (Map<String, String> params, HttpServletRequest request, HttpServletResponse response) -> {
            response.setContentType("text/html");
            sendHtmlOrRedirect(response, callback.apply(params));
        };
    }

    private static void sendHtmlOrRedirect(HttpServletResponse response, String result) throws IOException {
        if(result.startsWith("REDIRECT:")) {
            response.sendRedirect(result.substring(9));
        } else {
            response.getOutputStream().write(result.getBytes(StandardCharsets.UTF_8));
        }
    }
    
    private static TaskController createTaskController() {
        return new TaskController(createTaskRepository());
    }

    private static TaskRepository createTaskRepository() {
        return new TaskRepository(emf.createEntityManager()); // TODO review cleanup
    }

    private static EntityManagerFactory createEntityManagerFactory() {
        Map<String, String> props = new HashMap<>();
        config.getConfigProperty("app.jdbc.driver").ifPresent(v -> props.put("javax.persistence.jdbc.driver", v));
        config.getConfigProperty("app.jdbc.url").ifPresent(v -> props.put("javax.persistence.jdbc.url", v));
        config.getConfigProperty("app.jdbc.username").ifPresent(v -> props.put("javax.persistence.jdbc.user", v));
        config.getConfigProperty("app.jdbc.password").ifPresent(v -> props.put("javax.persistence.jdbc.password", v));
        return Persistence.createEntityManagerFactory("taskDS", props);
    }

    interface MyHandler {
        void handle(Map<String, String> params, HttpServletRequest request, HttpServletResponse response)
                throws IOException;
    }
}
