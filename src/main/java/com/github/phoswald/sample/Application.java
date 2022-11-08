package com.github.phoswald.sample;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
import jakarta.servlet.ServletException;
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
                get("/app/rest/sample/time", createHandler(() -> sampleResource.getTime())), //
                get("/app/rest/sample/config", createHandler(() -> sampleResource.getConfig())), //
                post("/app/rest/sample/echo-xml", createXmlHandler(EchoRequest.class, reqBody -> sampleResource.postEcho(reqBody))), //
                post("/app/rest/sample/echo-json", createJsonHandler(EchoRequest.class, reqBody -> sampleResource.postEcho(reqBody))), //
//              get("/app/rest/tasks", createJsonHandler0(() -> taskResource.getTasks())), //
                post("/app/rest/tasks", createJsonHandler(TaskEntity.class, reqBody -> taskResource.postTasks(reqBody))), //
//              get("/app/rest/tasks/:id", createJsonHandlerEx(req -> taskResource.getTask(req.params("id")))), //
//              put("/app/rest/tasks/:id", createJsonHandlerEx(TaskEntity.class, (req, reqBody) -> taskResource.putTask(req.params("id"), reqBody))), //
//              delete("/app/rest/tasks/:id", createJsonHandlerEx(req -> taskResource.deleteTask(req.params("id")))), //
                get("/app/pages/sample", createHtmlHandler(() -> sampleController.getSamplePage())), //
                get("/app/pages/tasks", createHtmlHandler(() -> taskController.getTasksPage())), //
                post("/app/pages/tasks", createHtmlHandlerEx(params -> taskController.postTasksPage(params.get("title"), params.get("description")))), //
                get("/app/pages/tasks/([0-9a-z-]+)", createHtmlHandlerEx(params -> taskController.getTaskPage(params.get("1" /* "id" */), params.get("action")))), //
                post("/app/pages/tasks/([0-9a-z-]+)", createHtmlHandlerEx(params -> taskController.postTaskPage(params.get("1" /* "id" */), params.get("action"), params.get("title"), params.get("description"), params.get("done")))) //
        ));
        server.start();
    }

    void stop() throws Exception {
        server.stop();
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
            response.getOutputStream().write(serializeXml(callback.apply(
                    deserializeXml(reqClass, request.getInputStream()))).getBytes(StandardCharsets.UTF_8));
        };
    }

//    static <R> MyHandler createJsonHandler0(Supplier<Object> callback) {
//        return (Map<String, String> params, HttpServletRequest request, HttpServletResponse response) -> {
//            response.setContentType("application/json");
//            response.getOutputStream().write(serializeJson(callback.get()).getBytes(StandardCharsets.UTF_8));
//        };
//    }

    static <R> MyHandler createJsonHandler(Class<R> reqClass, Function<R, Object> callback) {
        return (Map<String, String> params, HttpServletRequest request, HttpServletResponse response) -> {
            response.setContentType("application/json");
            response.getOutputStream().write(serializeJson(callback.apply(
                    deserializeJson(reqClass, request.getInputStream()))).getBytes(StandardCharsets.UTF_8));
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

    private static String serializeXml(Object object) {
        var buffer = new StringWriter();
        JAXB.marshal(object, buffer);
        return buffer.toString();
    }

    private static <T> T deserializeXml(Class<T> clazz, InputStream stream) {
        return JAXB.unmarshal(stream, clazz);
    }

    private static String serializeJson(Object object) {
        return json.toJson(object);
    }

    private static <T> T deserializeJson(Class<T> clazz, InputStream stream) {
        return json.fromJson(stream, clazz);
    }

    interface MyHandler {
        void handle(Map<String, String> params, HttpServletRequest request, HttpServletResponse response)
                throws IOException;
    }
}
