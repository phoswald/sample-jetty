package com.github.phoswald.sample;

import java.util.function.Consumer;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PeekMatcher {

    private static final Logger logger = LoggerFactory.getLogger(PeekMatcher.class);

    static Matcher<String> peek(Consumer<String> consumer) {
        return new BaseMatcher<String>() {

            @Override
            public boolean matches(Object actual) {
                logger.info("peek() matches '{}'", actual);
                if (actual instanceof String actualString) {
                    consumer.accept(actualString);
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("peek()");
            }
        };
    }
}
