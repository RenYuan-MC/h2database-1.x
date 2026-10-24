/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 *
 * RenYuan wasm branch: channel over a file of the in-memory file system.
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
 * A file channel backed by a growable byte array.
 */
final class FileWasm extends FileBase {

    private final FilePathWasm.Entry entry;

    private final boolean readOnly;

    private long position;

    private volatile boolean closed;

    FileWasm(FilePathWasm.Entry entry, boolean readOnly) {
        this.entry = entry;
        this.readOnly = readOnly;
    }

    @Override
    public long size() {
        return entry.length();
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public FileChannel position(long newPosition) {
        position = newPosition;
        return this;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int n = read(dst, position);
        if (n > 0) {
            position += n;
        }
        return n;
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        if (closed) {
            throw new ClosedChannelException();
        }
        if (position >= entry.length()) {
            return -1;
        }
        int count = (int) Math.min(dst.remaining(), entry.length() - position);
        byte[] buffer = new byte[count];
        int n = entry.read(position, buffer, 0, count);
        if (n <= 0) {
            return -1;
        }
        dst.put(buffer, 0, n);
        return n;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int n = write(src, position);
        position += n;
        return n;
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        if (readOnly) {
            throw new NonWritableChannelException();
        }
        if (closed) {
            throw new ClosedChannelException();
        }
        int count = src.remaining();
        if (count == 0) {
            return 0;
        }
        byte[] buffer = new byte[count];
        src.get(buffer);
        entry.write(position, buffer, 0, count);
        return count;
    }

    @Override
    public FileChannel truncate(long newLength) throws IOException {
        if (readOnly) {
            throw new NonWritableChannelException();
        }
        if (closed) {
            throw new ClosedChannelException();
        }
        if (newLength < entry.length()) {
            entry.truncate((int) newLength);
            position = Math.min(position, newLength);
        }
        return this;
    }

    @Override
    public void force(boolean metaData) {
        // the data already lives in memory, the host writes it to disk
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) {
        // RenYuan wasm branch: a single browser process owns the image.
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

    @Override
    protected void implCloseChannel() {
        closed = true;
    }

    int read(byte[] buffer, int offset, int length) throws IOException {
        int n = read(ByteBuffer.wrap(buffer, offset, length), position);
        if (n > 0) {
            position += n;
        }
        return n;
    }

    void write(byte[] buffer, int offset, int length) throws IOException {
        write(ByteBuffer.wrap(buffer, offset, length), position);
        position += length;
    }
}
