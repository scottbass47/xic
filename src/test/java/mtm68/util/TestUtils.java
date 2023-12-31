package mtm68.util;

import org.junit.jupiter.api.Assertions;

public class TestUtils {

	@SuppressWarnings("unchecked")
	public static <T> T assertInstanceOfAndReturn(Class<T> clazz, Object obj) {
		Assertions.assertTrue(clazz.isAssignableFrom(obj.getClass()), obj.getClass() + " is not an instanceof " + clazz);
		return (T) obj;
	}
	
	public static <T> void assertInstanceOf(Class<T> clazz, Object obj) {
		Assertions.assertTrue(clazz.isAssignableFrom(obj.getClass()), obj.getClass() + " is not an instanceof " + clazz);
	}
}
