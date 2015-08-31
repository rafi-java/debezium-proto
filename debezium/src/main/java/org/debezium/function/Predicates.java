/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.debezium.function;

import java.util.function.Predicate;

/**
 * @author Randall Hauch
 *
 */
public class Predicates {

    public static <T> Predicate<T> notNull() {
        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return t != null;
            }
        };
    }
    
    private Predicates() {
    }
    
}
