package com.epam.reportportal.utils;

import com.epam.reportportal.annotations.StepTemplateConfig;

import java.lang.reflect.Array;
import java.util.Iterator;

import static org.joor.Reflect.on;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StepTemplateUtils {

	private StepTemplateUtils() {
		//static only
	}

	public static String retrieveValue(StepTemplateConfig templateConfig, int index, String[] fields, Object object) {

		if (object == null) {
			return "null";
		}

		for (int i = index; i < fields.length; i++) {
			if (object instanceof Object[]) {
				return parseArray(templateConfig, (Object[]) object, i, fields);
			}

			if (object instanceof Iterable) {
				return parseCollection(templateConfig, (Iterable) object, i, fields);
			}

			object = on(object).get(fields[i]);
		}

		return parseDescendant(templateConfig, object);
	}

	private static String parseArray(StepTemplateConfig templateConfig, Object[] array, int index, String[] fields) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(templateConfig.arrayStartSymbol());

		for (int i = 0; i < array.length; i++) {
			stringBuilder.append(retrieveValue(templateConfig, index, fields, array[i]));
			if (i < array.length - 1) {
				stringBuilder.append(templateConfig.arrayElementDelimiter());
			}
		}

		stringBuilder.append(templateConfig.arrayEndSymbol());
		return stringBuilder.toString();
	}

	private static String parseCollection(StepTemplateConfig templateConfig, Iterable<?> iterable, int index, String[] fields) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(templateConfig.iterableStartSymbol());

		Iterator iterator = iterable.iterator();
		while (iterator.hasNext()) {
			stringBuilder.append(retrieveValue(templateConfig, index, fields, iterator.next()));
			if (iterator.hasNext()) {
				stringBuilder.append(templateConfig.iterableElementDelimiter());
			}
		}

		stringBuilder.append(templateConfig.iterableEndSymbol());
		return stringBuilder.toString();
	}

	private static String parseDescendant(StepTemplateConfig templateConfig, Object object) {

		if (object.getClass().isArray()) {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(templateConfig.arrayStartSymbol());

			int length = Array.getLength(object);
			for (int i = 0; i < length; i++) {
				stringBuilder.append(Array.get(object, i));
				if (i < length - 1) {
					stringBuilder.append(templateConfig.arrayElementDelimiter());
				}
			}

			stringBuilder.append(templateConfig.arrayEndSymbol());
			return stringBuilder.toString();
		}

		return String.valueOf(object);
	}
}
