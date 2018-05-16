package com.omnix.manager.websocket;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 
 * @author seokwon
 *
 */
public class ByteBufferSupport {
    
    private ByteBuf buffer;
    
    public ByteBufferSupport() {
        this(0);
    }
    
    public ByteBufferSupport(int initialSizeHint) {
        buffer = Unpooled.unreleasableBuffer(Unpooled.buffer(initialSizeHint, Integer.MAX_VALUE));
    }
    
    public ByteBufferSupport(byte[] bytes) {
        buffer = Unpooled.unreleasableBuffer(Unpooled.buffer(bytes.length, Integer.MAX_VALUE)).writeBytes(bytes);
    }
    
    public ByteBufferSupport(String str, String enc) {
        this(str.getBytes(Charset.forName(Objects.requireNonNull(enc))));
    }
    
    public ByteBufferSupport(String str, Charset cs) {
        this(str.getBytes(cs));
    }
    
    public ByteBufferSupport(String str) {
        this(str, StandardCharsets.UTF_8);
    }
    
    public ByteBufferSupport(ByteBuf buffer) {
        this.buffer = Unpooled.unreleasableBuffer(buffer);
    }
    
    public String toString() {
        return buffer.toString(StandardCharsets.UTF_8);
    }
    
    public String toString(String enc) {
        return buffer.toString(Charset.forName(enc));
    }
    
    public String toString(Charset enc) {
        return buffer.toString(enc);
    }
    
    public byte getByte(int pos) {
        return buffer.getByte(pos);
    }
    
    public short getUnsignedByte(int pos) {
        return buffer.getUnsignedByte(pos);
    }
    
    public int getInt(int pos) {
        return buffer.getInt(pos);
    }
    
    public int getIntLE(int pos) {
        return buffer.getIntLE(pos);
    }
    
    public long getUnsignedInt(int pos) {
        return buffer.getUnsignedInt(pos);
    }
    
    public long getUnsignedIntLE(int pos) {
        return buffer.getUnsignedIntLE(pos);
    }
    
    public long getLong(int pos) {
        return buffer.getLong(pos);
    }
    
    public long getLongLE(int pos) {
        return buffer.getLongLE(pos);
    }
    
    public double getDouble(int pos) {
        return buffer.getDouble(pos);
    }
    
    public float getFloat(int pos) {
        return buffer.getFloat(pos);
    }
    
    public short getShort(int pos) {
        return buffer.getShort(pos);
    }
    
    public short getShortLE(int pos) {
        return buffer.getShortLE(pos);
    }
    
    public int getUnsignedShort(int pos) {
        return buffer.getUnsignedShort(pos);
    }
    
    public int getUnsignedShortLE(int pos) {
        return buffer.getUnsignedShortLE(pos);
    }
    
    public int getMedium(int pos) {
        return buffer.getMedium(pos);
    }
    
    public int getMediumLE(int pos) {
        return buffer.getMediumLE(pos);
    }
    
    public int getUnsignedMedium(int pos) {
        return buffer.getUnsignedMedium(pos);
    }
    
    public int getUnsignedMediumLE(int pos) {
        return buffer.getUnsignedMediumLE(pos);
    }
    
    public byte[] getBytes() {
        byte[] arr = new byte[buffer.writerIndex()];
        buffer.getBytes(0, arr);
        return arr;
    }
    
    public byte[] getBytes(int start, int end) {
        byte[] arr = new byte[end - start];
        buffer.getBytes(start, arr, 0, end - start);
        return arr;
    }
    
    public String getString(int start, int end, String enc) {
        byte[] bytes = getBytes(start, end);
        Charset cs = Charset.forName(enc);
        return new String(bytes, cs);
    }
    
    public String getString(int start, int end) {
        byte[] bytes = getBytes(start, end);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    public ByteBuf getByteBuf() {
        // Return a duplicate so the Buffer can be written multiple times.
        // See #648
        return buffer.duplicate();
    }
    
    public ByteBuf getBuffer() {
        return buffer;
    }
    
}
