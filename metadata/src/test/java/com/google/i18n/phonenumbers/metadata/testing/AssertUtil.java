package com.google.i18n.phonenumbers.metadata.testing;

/** Additional useful assertion methods for testing. */
public final class AssertUtil {

  /** Asserts the given code threw the expected exception, and returns it for further checking. */
  public static <T extends Throwable> T assertThrows(Class<T> clazz, Runnable fn) {
    String message;
    try {
      fn.run();
      message = String.format("expected exception (%s) was not thrown", clazz.getSimpleName());
    } catch (Throwable t) {
      if (clazz.isInstance(t)) {
        return clazz.cast(t);
      }
      message = String.format("expected (%s), but caught: %s", clazz.getSimpleName(), t);
    }
    throw new AssertionError(message);
  }

  private AssertUtil() {}
}
