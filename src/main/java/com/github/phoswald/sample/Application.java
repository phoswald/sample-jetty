package com.github.phoswald.sample;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;

import com.github.phoswald.sample.sample.EchoRequest;
import com.github.phoswald.sample.sample.SampleController;
import com.github.phoswald.sample.sample.SampleResource;
import com.github.phoswald.sample.task.TaskController;
import com.github.phoswald.sample.task.TaskEntity;
import com.github.phoswald.sample.task.TaskResource;
import com.github.phoswald.sample.utils.ConfigProvider;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXB;

public class Application {

    private static final Logger logger = LogManager.getLogger();
    private static final Jsonb json = JsonbBuilder.create();

    private final int port;
    private final SampleResource sampleResource;
    private final SampleController sampleController;
    private final TaskResource taskResource;
    private final TaskController taskController;

    private Server server;

    public Application( //
            ConfigProvider config, //
            SampleResource sampleResource, //
            SampleController sampleController, //
            TaskResource taskResource, //
            TaskController taskController) {
        this.port = Integer.parseInt(config.getConfigProperty("app.http.port").orElse("8080"));
        this.sampleResource = sampleResource;
        this.sampleController = sampleController;
        this.taskResource = taskResource;
        this.taskController = taskController;
    }

    public static void main(String[] args) throws Exception {
        var module = new ApplicationModule() { };
        module.getApplication().start();
    }

    void start() throws Exception {
        logger.info("sample-jetty is starting, port=" + port);

        server = new Server(port);
        server.setHandler(routes( //
                files("/resources"), //
                get("/app/rest/sample/time", createHandler(params -> sampleResource.getTime())), //
                get("/app/rest/sample/config", createHandler(params -> sampleResource.getConfig())), //
                post("/app/rest/sample/echo-xml", createXmlHandler(EchoRequest.class, (params, reqBody) -> sampleResource.postEcho(reqBody))), //
                post("/app/rest/sample/echo-json", createJsonHandler(EchoRequest.class, (params, reqBody) -> sampleResource.postEcho(reqBody))), //
                get("/app/rest/tasks", createJsonHandler(params -> taskResource.getTasks())), //
                post("/app/rest/tasks", createJsonHandler(TaskEntity.class, (params, reqBody) -> taskResource.postTasks(reqBody))), //
                get("/app/rest/tasks/([0-9a-z-]+)", createJsonHandler(params -> taskResource.getTask(params.get("1" /* "id" */)))), //
                put("/app/rest/tasks/([0-9a-z-]+)", createJsonHandler(TaskEntity.class, (params, reqBody) -> taskResource.putTask(params.get("1" /* "id" */), reqBody))), //
                delete("/app/rest/tasks/([0-9a-z-]+)", createJsonHandler(params -> taskResource.deleteTask(params.get("1" /* "id" */)))), //
                get("/app/pages/sample", createHtmlHandler(params -> sampleController.getSamplePage())), //
                get("/app/pages/tasks", createHtmlHandler(params -> taskController.getTasksPage())), //
                post("/app/pages/tasks", createHtmlHandler(params -> taskController.postTasksPage(params.get("title"), params.get("description")))), //
                get("/app/pages/tasks/([0-9a-z-]+)", createHtmlHandler(params -> taskController.getTaskPage(params.get("1" /* "id" */), params.get("action")))), //
                post("/app/pages/tasks/([0-9a-z-]+)", createHtmlHandler(params -> taskController.postTaskPage(params.get("1" /* "id" */), params.get("action"), params.get("title"), params.get("description"), params.get("done")))) //
        ));
        server.start();
    }

    void stop() throws Exception {
        server.stop();
    }

    private static Handler routes(Handler... routes) {
        HandlerList handlers = new HandlerList();
        Arrays.asList(routes).forEach(handlers::addHandler);
        return handlers;
    }

    private static Handler files(String classPath) {
        ResourceHandler handler = new ResourceHandler();
        handler.setBaseResource(Resource.newClassPathResource(classPath));
        handler.setDirectoriesListed(false);
        handler.setWelcomeFiles(new String[] { "index.html" });
        return handler;
    }

    private static Handler get(String path, MyHandler handler) {
        return method(path, "GET", handler);
    }

    private static Handler post(String path, MyHandler handler) {
        return method(path, "POST", handler);
    }

    private static Handler put(String path, MyHandler handler) {
        return method(path, "PUT", handler);
    }

    private static Handler delete(String path, MyHandler handler) {
        return method(path, "DELETE", handler);
    }

    private static Handler method(String path, String method, MyHandler handler) {
        Pattern pattern = Pattern.compile("^" + path + "$");
        return new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
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

    private static MyHandler createHandler(Function<Map<String, String>, Object> callback) {
        return (params, request, response) -> {
            Object result = callback.apply(params);
            write(response, result);
        };
    }

    private static <R> MyHandler createXmlHandler(Class<R> reqClass, BiFunction<Map<String, String>, R, Object> callback) {
        return (params, request, response) -> handleXml(response,
                () -> callback.apply(params, deserializeXml(reqClass, read(request))));
    }

    private static void handleXml(HttpServletResponse response, Supplier<Object> callback) {
        Object result = callback.get();
        response.setContentType("text/xml");
        write(response, serializeXml(result));
    }

    private static MyHandler createJsonHandler(Function<Map<String, String>, Object> callback) {
        return (params, request, response) -> handleJson(response, () -> callback.apply(params));
    }

    private static <R> MyHandler createJsonHandler(Class<R> reqClass, BiFunction<Map<String, String>, R, Object> callback) {
        return (params, request, response) -> handleJson(response,
                () -> callback.apply(params, deserializeJson(reqClass, read(request))));
    }

    private static void handleJson(HttpServletResponse response, Supplier<Object> callback) {
        Object result = callback.get();
        if(result == null) {
            response.setStatus(404);
        } else if(result instanceof String resultString) {
            write(response, resultString);
        } else {
            response.setContentType("application/json");
            write(response, serializeJson(result));
        }
    }

    private static MyHandler createHtmlHandler(Function<Map<String, String>, Object> callback) {
        return (params, request, response) -> handleHtml(response, () -> callback.apply(params));
    }

    private static void handleHtml(HttpServletResponse response, Supplier<Object> callback) {
        Object result = callback.get();
        if(result instanceof Path resultPath) {
            try {
                response.sendRedirect(resultPath.toString());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            response.setContentType("text/html");
            write(response, result);
        }
    }

    private static void write(HttpServletResponse response, Object object) {
        try {
            response.getOutputStream().write(object.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(HttpServletRequest request) {
        try {
            return new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String serializeXml(Object object) {
        var buffer = new StringWriter();
        JAXB.marshal(object, buffer);
        return buffer.toString();
    }

    private static <T> T deserializeXml(Class<T> clazz, String text) {
        return JAXB.unmarshal(new StringReader(text), clazz);
    }

    private static String serializeJson(Object object) {
        return json.toJson(object);
    }

    private static <T> T deserializeJson(Class<T> clazz, String text) {
        return json.fromJson(text, clazz);
    }

    interface MyHandler {
        void handle(Map<String, String> params, HttpServletRequest request, HttpServletResponse response);
    }
}
