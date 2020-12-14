package com.wanghao.vsrs.rtmp.message.binary;

import com.wanghao.vsrs.err.ProtocolException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.*;

/**
 * @author wanghao
 */
public class AMF0 {
    public static final int Number = 0x00;
    public static final int Boolean = 0x01;
    public static final int String = 0x02;
    public static final int Object = 0x03;
    public static final int MovieClip = 0x04;
    public static final int Null = 0x05;
    public static final int Undefined = 0x06;
    public static final int Reference = 0x07;
    public static final int EcmaArray = 0x08;
    public static final int ObjectEnd = 0x09;
    public static final int StrictArray = 0x0A;
    public static final int Date = 0x0B;
    public static final int LongString = 0x0C;
    public static final int UnSupported = 0x0D;
    public static final int RecordSet = 0x0E;
    public static final int XmlDocument = 0x0F;
    public static final int TypedObject = 0x10;
    // AVM+ object is the AMF3 object.
    public static final int AVMplusObject = 0x11;
    // origin array whose data takes the same form as LengthValueBytes
    public static final int OriginStrictArray = 0x20;

    private static final byte BOOLEAN_TRUE = 0x01;
    private static final byte BOOLEAN_FALSE = 0x00;

    private static final byte[] END_OF_OBJECT = new byte[] {0x00, 0x00, 0x09};
    private static final byte[] END_OF_OBJECT_FFMPEG = new byte[] {0x65, 0x00, 0x00};

    public static int getType(Object obj) {
        if (obj == null) {
            return Null;
        }
        if (obj instanceof Number) {
            return Number;
        }
        if (obj instanceof Boolean) {
            return Boolean;
        }
        if (obj instanceof String) {
            return String;
        }
        if (obj instanceof AMF0Object) {
            return Object;
        }
        if (obj instanceof Map) {
            return EcmaArray;
        }
        if (obj instanceof Object[]) {
            return StrictArray;
        }
        if (obj instanceof Date) {
            return Date;
        }
        throw new RuntimeException("unexpected obj class: " + obj.getClass().getComponentType());
    }

    public static List<Object> decodeAll(final ByteBuf in) {
        List<Object> list = new ArrayList<>();
        while (in.isReadable()) {
            list.add(decode(in));
        }
        return list;
    }

    private static Object decode(final ByteBuf in) {
        byte type = in.readByte();
        return decode(in, type);
    }

    private static String decodeString(final ByteBuf in) {
        short length = in.readShort();
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        return new String(bytes);
    }

    private static Object decode(final ByteBuf in, final byte type) {
        switch (type) {
            case Number:
                return Double.longBitsToDouble(in.readLong());
            case Boolean:
                return in.readByte() == BOOLEAN_TRUE;
            case String: {
                return decodeString(in);
            }
            case Object: {
                AMF0Object obj = new AMF0Object();
                final byte[] endOfObject = new byte[3];
                while (in.isReadable()) {
                    in.getBytes(in.readerIndex(), endOfObject);
                    if (Arrays.equals(endOfObject, END_OF_OBJECT) || Arrays.equals(endOfObject, END_OF_OBJECT_FFMPEG)) {
                        in.skipBytes(3);
                        break;
                    }
                    obj.put(decodeString(in), decode(in));
                }
                return obj;
            }
            case EcmaArray: {
                int len = in.readInt(); // array length
                Map<String, Object> map = new LinkedHashMap<>();
                final byte[] endOfObject = new byte[3];
                while (in.isReadable()) {
                    in.getBytes(in.readerIndex(), endOfObject);
                    if (Arrays.equals(endOfObject, END_OF_OBJECT) || Arrays.equals(endOfObject, END_OF_OBJECT_FFMPEG)) {
                        in.skipBytes(3);
                        break;
                    }
                    map.put(decodeString(in), decode(in));
                }
                return map;
            }
            case StrictArray:
                int size = in.readInt();
                Object[] array = new Object[size];
                for (int i = 0; i < size; i++) {
                    array[i] = decode(in);
                }
                return array;
            case Date:
                long date = in.readLong();
                in.readShort();//ignore timezone
                return new Date(date);
            case LongString:
                int length = in.readInt();
                byte[] bytes = new byte[length];
                in.readBytes(bytes);
                return new String(bytes);
            case Null:
            case Undefined:
            case UnSupported:
                return null;
            default:
                throw new ProtocolException("unexpected AMF type: " + type);
        }
    }

    public static ByteBuf encodeAll(final List<Object> objs) {
        if (objs == null) {
            return null;
        }

        ByteBuf tmpOut = Unpooled.buffer();
        for (Object obj : objs) {
            encode(tmpOut, obj);
        }
        return tmpOut;
    }

    private static void encodeString(final ByteBuf tmpOut, final String str) {
        byte[] bytes = str.getBytes();
        tmpOut.writeShort((short) bytes.length);
        tmpOut.writeBytes(bytes);
    }

    private static void encode(final ByteBuf tmpOut, final Object obj) {
        int type = getType(obj);
        tmpOut.writeByte(type);
        switch (type) {
            case Number:
                if (obj instanceof Double) {
                    tmpOut.writeLong(Double.doubleToLongBits((Double) obj));
                } else {
                    tmpOut.writeLong(Double.doubleToLongBits(Double.parseDouble(obj.toString())));
                }
                return;
            case Boolean:
                tmpOut.writeByte((Boolean) obj ? BOOLEAN_TRUE : BOOLEAN_FALSE);
                return;
            case String: {
                encodeString(tmpOut, (String) obj);
                return;
            }
            case Object:
                AMF0Object amf0Object = (AMF0Object) obj;
                for (Map.Entry<String, Object> entry : amf0Object.entrySet()) {
                    encodeString(tmpOut, entry.getKey());
                    encode(tmpOut, entry.getValue());
                }
                tmpOut.writeBytes(END_OF_OBJECT);
                return;
            case EcmaArray:
                Map<String, Object> map = (Map<String, Object>) obj;
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    encodeString(tmpOut, entry.getKey());
                    encode(tmpOut, entry.getValue());
                }
                tmpOut.writeBytes(END_OF_OBJECT);
                return;
            case StrictArray:
                Object[] array = (Object[]) obj;
                tmpOut.writeInt(array.length);
                for (Object o : array) {
                    encode(tmpOut, o);
                }
                return;
            case Date:
                long time = ((Date) obj).getTime();
                tmpOut.writeLong(Double.doubleToLongBits(time));
                tmpOut.writeShort((short) 0);//timezone
                return;
            case Null:
            case Undefined:
            case UnSupported:
                return;
            default:
                throw new ProtocolException("unexpected AMF type: " + type);
        }
    }
}
