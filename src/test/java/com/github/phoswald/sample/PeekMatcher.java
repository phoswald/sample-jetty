package com.github.phoswald.sample;

import java.util.function.Consumer;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

class PeekMatcher {

    static Matcher<String> peek(Consumer<String> consumer) {
        return new BaseMatcher<String>() {

            @Override
            public boolean matches(Object actual) {
                System.out.println("spy:" + actual);
                if (actual instanceof String actualString) {
                    consumer.accept(actualString);
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("XXXX");
            }
        };
    }
}
