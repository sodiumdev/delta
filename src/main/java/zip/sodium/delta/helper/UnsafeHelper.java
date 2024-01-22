package zip.sodium.delta.helper;

import io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess;

import java.lang.reflect.Field;

public final class UnsafeHelper {
    private UnsafeHelper() {}

    public static void setStaticField(final Field field, final Object value) {
        final var unsafe = UnsafeAccess.UNSAFE;

        field.setAccessible(true);

        final var staticFieldBase = unsafe.staticFieldBase(field);
        final long staticFieldOffset = unsafe.staticFieldOffset(field);

        unsafe.putObject(staticFieldBase, staticFieldOffset, value);
    }

    public static void setField(final Object base, final Field field, final Object value) {
        final var unsafe = UnsafeAccess.UNSAFE;

        final var offset = unsafe.objectFieldOffset(field);

        if (value instanceof Integer val) {
            unsafe.putInt(base, offset, val);
        } else if (value instanceof Short val) {
            unsafe.putShort(base, offset, val);
        } else if (value instanceof Boolean val) {
            unsafe.putBoolean(base, offset, val);
        } else if (value instanceof Float val) {
            unsafe.putFloat(base, offset, val);
        } else if (value instanceof Double val) {
            unsafe.putDouble(base, offset, val);
        } else if (value instanceof Byte val) {
            unsafe.putByte(base, offset, val);
        } else unsafe.putObject(base, offset, value);
    }

    public static Object getObject(final Object base, final Field field) {
        final var unsafe = UnsafeAccess.UNSAFE;

        return unsafe.getObject(
                base,
                unsafe.objectFieldOffset(field)
        );
    }

    public static Object getObjectStatic(final Field field) {
        final var unsafe = UnsafeAccess.UNSAFE;

        return unsafe.getObject(
                unsafe.staticFieldBase(field),
                unsafe.staticFieldOffset(field)
        );
    }
}
