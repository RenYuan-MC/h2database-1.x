/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 *
 * RenYuan wasm branch: a read-only file system that exposes a database image
 * passed in from JavaScript.
 */
package org.h2.store.fs.wasm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import org.h2.store.fs.FilePath;

/**
 * A read-only file system backed by a single in-memory database image.
 */
public class FilePathWasm extends FilePath {

    /**
     * The scheme of this file system.
     */
    public static final String SCHEME = "wasm";

    private static volatile byte[] database;

    /**
     * Set the database image that will be served by this provider.
     *
     * @param data the database file bytes
     */
    public static void setDatabase(byte[] data) {
        database = data;
    }

    /**
     * Get the database image, or {@code null} if none is loaded.
     *
     * @return the database file bytes
     */
    public static byte[] getDatabase() {
        return database;
    }

    /**
     * Release the database image.
     */
    public static void clearDatabase() {
        database = null;
    }

    public FilePathWasm() {
    }

    private FilePathWasm(String name) {
        this.name = name;
    }

    @Override
    public FilePath getPath(String path) {
        return new FilePathWasm(path.replace('\\', '/'));
    }

    @Override
    public long size() {
        byte[] data = database;
        return isDatabase() && data != null ? data.length : 0;
    }

    @Override
    public void moveTo(FilePath newName, boolean atomicReplace) {
        throw unsupported();
    }

    @Override
    public boolean createFile() {
        return false;
    }

    @Override
    public boolean exists() {
        return isDatabase() && database != null;
    }

    @Override
    public void delete() {
        throw unsupported();
    }

    @Override
    public List<FilePath> newDirectoryStream() {
        return new ArrayList<>();
    }

    @Override
    public FilePath toRealPath() {
        return this;
    }

    @Override
    public FilePath getParent() {
        int index = name.lastIndexOf('/');
        return index < 0 ? null : new FilePathWasm(name.substring(0, index));
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isAbsolute() {
        return true;
    }

    @Override
    public long lastModified() {
        return 0L;
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public void createDirectory() {
        throw unsupported();
    }

    @Override
    public OutputStream newOutputStream(boolean append) throws IOException {
        throw new IOException("read-only file system");
    }

    @Override
    public FileChannel open(String mode) throws IOException {
        if (!"r".equals(mode)) {
            throw new IOException("read-only file system");
        }
        byte[] data = database;
        return new FileWasm(this, data != null ? data : new byte[0]);
    }

    @Override
    public InputStream newInputStream() throws IOException {
        throw new IOException("read-only file system");
    }

    @Override
    public boolean setReadOnly() {
        return true;
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    private boolean isDatabase() {
        return name != null && name.endsWith(".mv.db");
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("read-only file system");
    }
}
