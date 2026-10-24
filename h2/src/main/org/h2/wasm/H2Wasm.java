/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 *
 * RenYuan wasm branch: JavaScript entry point for reading H2 databases inside a browser runtime.
 */
package org.h2.wasm;

import java.util.Base64;
import java.util.Properties;
import org.h2.command.Prepared;
import org.h2.engine.ConnectionInfo;
import org.h2.engine.Engine;
import org.h2.engine.Session;
import org.h2.result.ResultInterface;
import org.h2.store.fs.wasm.FilePathWasm;
import org.h2.value.Value;
import org.h2.value.ValueNull;
import org.teavm.jso.JSExport;

/**
 * Exposes a small read-only API to JavaScript. Everything is returned as JSON strings so that the
 * caller does not need any Java object bridge.
 */
public final class H2Wasm {

    private static Session session;

    private H2Wasm() {
    }

    /**
     * Open a database image.
     *
     * @param base64 the database file bytes encoded with Base64
     * @param user the user name, may be empty to use "sa"
     * @param password the password, may be empty
     * @return a JSON result
     */
    @JSExport
    public static String open(String base64, String user, String password) {
        try {
            close();
            byte[] data = Base64.getDecoder().decode(base64);
            FilePathWasm.setDatabase(data);
            String userName = user == null || user.isEmpty() ? "sa" : user;
            Properties properties = new Properties();
            properties.setProperty("USER", userName);
            properties.setProperty("PASSWORD", password == null ? "" : password);
            ConnectionInfo info = new ConnectionInfo(
                    "jdbc:h2:wasm:db;ACCESS_MODE_DATA=r;FILE_LOCK=NO;IFEXISTS=TRUE", properties);
            session = Engine.getInstance().createSession(info);
            return "{\"ok\":true}";
        } catch (Throwable e) {
            session = null;
            FilePathWasm.clearDatabase();
            return error(e);
        }
    }

    /**
     * List base tables.
     *
     * @return a JSON result with columns and rows
     */
    @JSExport
    public static String tables() {
        return query("SELECT TABLE_SCHEMA, TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA <> 'INFORMATION_SCHEMA' ORDER BY TABLE_SCHEMA, TABLE_NAME");
    }

    /**
     * Execute a statement and return its result as JSON.
     *
     * @param sql the SQL statement
     * @return a JSON result
     */
    @JSExport
    public static String query(String sql) {
        if (session == null) {
            return "{\"ok\":false,\"error\":\"database is not open\"}";
        }
        try {
            Prepared prepared = session.prepare(sql);
            if (!prepared.isQuery()) {
                int updated = prepared.update();
                return "{\"ok\":true,\"updated\":" + updated + '}';
            }
            ResultInterface result = prepared.query(100000);
            try {
                int columnCount = result.getVisibleColumnCount();
                StringBuilder builder = new StringBuilder(4096);
                builder.append("{\"ok\":true,\"columns\":[");
                for (int i = 0; i < columnCount; i++) {
                    if (i > 0) {
                        builder.append(',');
                    }
                    appendJsonString(builder, result.getAlias(i));
                }
                builder.append("],\"rows\":[");
                boolean firstRow = true;
                while (result.next()) {
                    if (!firstRow) {
                        builder.append(',');
                    }
                    firstRow = false;
                    Value[] row = result.currentRow();
                    builder.append('[');
                    for (int i = 0; i < columnCount; i++) {
                        if (i > 0) {
                            builder.append(',');
                        }
                        Value value = i < row.length ? row[i] : null;
                        if (value == null || value == ValueNull.INSTANCE) {
                            builder.append("null");
                        } else {
                            appendJsonString(builder, value.getString());
                        }
                    }
                    builder.append(']');
                }
                builder.append("]}");
                return builder.toString();
            } finally {
                result.close();
            }
        } catch (Throwable e) {
            return error(e);
        }
    }

    /**
     * Close the database and release the image.
     */
    @JSExport
    public static void close() {
        if (session != null) {
            try {
                session.close();
            } catch (Throwable ignored) {
                // ignore
            }
            session = null;
        }
        FilePathWasm.clearDatabase();
    }

    private static String error(Throwable e) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"ok\":false,\"chain\":[");
        Throwable current = e;
        boolean first = true;
        int depth = 0;
        while (current != null && depth++ < 8) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append("{\"type\":");
            appendJsonString(builder, current.getClass().getName());
            builder.append(",\"message\":");
            appendJsonString(builder, String.valueOf(current.getMessage()));
            builder.append('}');
            current = current.getCause();
        }
        builder.append("],\"error\":");
        String message = e.getMessage();
        if (message == null || message.isEmpty()) {
            message = e.getClass().getName();
        }
        appendJsonString(builder, message);
        try {
            java.io.StringWriter writer = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(writer));
            builder.append(",\"trace\":");
            appendJsonString(builder, writer.toString());
        } catch (Throwable ignored) {
            // ignore
        }
        builder.append('}');
        return builder.toString();
    }

    private static void appendJsonString(StringBuilder builder, String value) {
        if (value == null) {
            builder.append("null");
            return;
        }
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        builder.append("\\u");
                        for (int padding = hex.length(); padding < 4; padding++) {
                            builder.append('0');
                        }
                        builder.append(hex);
                    } else {
                        builder.append(c);
                    }
            }
        }
        builder.append('"');
    }
}
