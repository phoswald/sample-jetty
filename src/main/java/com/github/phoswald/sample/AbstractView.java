package com.github.phoswald.sample;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public abstract class AbstractView<T> {

    private static final TemplateEngine templateEngine = createTemplateEngine();
    private final String templateName;
    private final String modelName;

    protected AbstractView(String templateName, String modelName) {
        this.templateName = templateName;
        this.modelName = modelName;
    }

    public String render(T model) {
        Context context = new Context();
        context.setVariable(modelName, model);
        String page = templateEngine.process(templateName, context);
        return page;
    }

    private static TemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver(AbstractView.class.getClassLoader());
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setCacheable(false); // defaults to true, use false to update when modified

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }
}
