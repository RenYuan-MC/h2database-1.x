/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 *
 * RenYuan wasm branch: read-only channel over an in-memory database image.
 */
package org.h2.store.fs.wasm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonWritableChannelException;
import org.h2.store.fs.FakeFileChannel;
import org.h2.store.fs.FileBase;

/**
 * A read-only file channel over a byte array.
 */
final class FileWasm extends FileBase {

    private final FilePathWasm path;

    private final byte[] data;

    private long pos;

    FileWasm(FilePathWasm path, byte[] data) {
        this.path = path;
        this.data = data;
    }

    FilePathWasm getPath() {
        return path;
    }

    @Override
    public long size() {
        return data.length;
    }

    @Override
    public long position() {
        return pos;
    }

    @Override
    public FileChannel position(long newPosition) {
        pos = newPosition;
        return this;
    }

    @Override
    public FileChannel truncate(long newLength) throws IOException {
        throw new NonWritableChannelException();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        int length = read(dst, pos);
        if (length > 0) {
            pos += length;
        }
        return length;
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        if (position < 0) {
            return 0;
        }
        if (position >= data.length) {
            return -1;
        }
        int length = (int) Math.min(dst.remaining(), data.length - position);
        dst.put(data, (int) position, length);
        return length;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        throw new NonWritableChannelException();
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        throw new NonWritableChannelException();
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) {
        // RenYuan wasm branch: there is a single process and a single database image,
        // so locking always succeeds and does nothing.
        return new FileLock(FakeFileChannel.INSTANCE, position, size, shared) {
            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public void release() {
            }
        };
    }
}
