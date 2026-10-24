/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 *
 * RenYuan wasm branch: the browser runtime has no Java compiler, so runtime
 * source compilation is not available.
 */
package org.h2.util;

import java.lang.reflect.Method;

import org.h2.message.DbException;

/**
 * This class allows to convert source code to a class.
 *
 * <p>In the browser build there is no compiler (javax.tools / javax.script are not part of the
 * runtime), so every compilation request is rejected. Aliases and triggers that reference an
 * existing class still work.</p>
 */
public class SourceCompiler {

    /**
     * Set the source code for the specified class.
     *
     * @param className the class name
     * @param source the source code
     */
    public void setSource(String className, String source) {
        // RenYuan wasm branch: source compilation is not available.
    }

    /**
     * Enable or disable the usage of the Java system compiler.
     *
     * @param enabled true to enable
     */
    public void setJavaSystemCompiler(boolean enabled) {
        // RenYuan wasm branch: source compilation is not available.
    }

    /**
     * Get the class object for the given name.
     *
     * @param packageAndClassName the class name
     * @return the class
     * @throws ClassNotFoundException on failure
     */
    public Class<?> getClass(String packageAndClassName) throws ClassNotFoundException {
        throw DbException.getUnsupportedException("source compilation");
    }

    /**
     * Get the first public static method of the given class.
     *
     * @param className the class name
     * @return the method name
     * @throws ClassNotFoundException on failure
     */
    public Method getMethod(String className) throws ClassNotFoundException {
        throw DbException.getUnsupportedException("source compilation");
    }
}
