/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.core;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epam.dlab.configuration.BillingToolConfigurationFactory;
import com.epam.dlab.core.parser.ParserBase;
import com.epam.dlab.exception.InitializationException;
import com.epam.dlab.logging.AppenderBase;
import com.google.common.io.Resources;

/** Billing toll utilities. 
 */
public class BillingUtils {

	/** Name of resource with the names of module classes. */
	private static final String RESOURCE_MODULE_NAMES = "/" + BillingToolConfigurationFactory.class.getName(); 
	
	/** Create and return map from given key/values.
	 * @param keyValues the key/value pairs.
	 */
	public static Map<String, String> stringsToMap(String ... keyValues) {
		Map<String, String> map = new HashMap<>();
		if (keyValues == null) {
			return map;
		}
		if (keyValues.length % 2 != 0) {
			throw new IllegalArgumentException("Missing key or value in arguments");
		}
		
		for (int i = 1; i < keyValues.length; i+=2) {
			map.put(keyValues[i - 1], keyValues[i]);
		}
		return map;
	}
	
	/** Read and return content as string list from resource.
	 * @param resourceName the name of resource.
	 * @return list of strings.
	 * @throws InitializationException
	 */
	public static List<String> getResourceAsList(String resourceName) throws InitializationException {
        try {
    		URL url = BillingToolConfigurationFactory.class.getResource(resourceName);
            if (url == null) {
            	throw new InitializationException("Resource " + resourceName + " not found");
            }
    		return Resources.readLines(url, Charset.forName("utf-8"));
		} catch (IllegalArgumentException | IOException e) {
			throw new InitializationException("Cannot read resource " + resourceName + ": " + e.getLocalizedMessage(), e);
		}
	}
	
	/** Return the list of billing tool modules.
	 * @throws InitializationException
	 */
	public static List<Class<?>> getModuleClassList() throws InitializationException {
		List<String> modules = getResourceAsList(RESOURCE_MODULE_NAMES);
		List<Class<?>> classes = new ArrayList<>();
		
		for (String className : modules) {
			try {
				classes.add(Class.forName(className));
			} catch (ClassNotFoundException e) {
				throw new InitializationException("Cannot add the sub type " + className +
						" from resource " + RESOURCE_MODULE_NAMES + ": " + e.getLocalizedMessage(), e);
			}
		}
    	return classes;
	}
	
	/** Check if child class belongs to parent by hierarchy.
	 * @param child the child class for check.
	 * @param parent the parent class from hierarchy.
	 */
	public static boolean classChildOf(Class<?> child, Class<?> parent) {
		if (!parent.isAssignableFrom(child)) {
			return false;
		}
		while (child != null) {
			if (parent.getName().equals(child.getName())) {
				return true;
			}
			child = child.getSuperclass();
		}
		return false;
	}

	/** Return the type of module if class is module otherwise <b>null</b>.
	 * @param moduleClass the class.
	 */
	public static ModuleType getModuleType(Class<?> moduleClass) {
		if (classChildOf(moduleClass, AdapterBase.class)) {
			return ModuleType.ADAPTER;
		} else if (classChildOf(moduleClass, FilterBase.class)) {
			return ModuleType.FILTER;
		} else if (classChildOf(moduleClass, ParserBase.class)) {
			return ModuleType.PARSER;
		} else if (classChildOf(moduleClass, AppenderBase.class)) {
			return ModuleType.LOGAPPENDER;
		}
		return null;
	}
	
	/** Returns the closest value to the argument.
	 * @param value the value.
	 * @param scale the scale.
	 */
	public static double round(double value, int scale) {
		int d = (int) Math.pow(10, scale);
		return (double) (Math.round(value * d)) / d;
	}

	/** Returns the closest value to the argument.
	 * @param value the value.
	 * @param scale the scale.
	 */
	public static Double round(Double value, int scale) {
		if (value == null) {
			return null;
		}
		int d = (int) Math.pow(10, scale);
		return (double) (Math.round(value * d)) / d;
	}

	/** Format and return the double value as string.
	 * @param value the value.
	 */
	public static String formatDouble(Double value) {
		return (value == null ? null : String.format("%,.2f", value));
	}
	
	/** Return the name of user without domain.
	 * @param value the value.
	 */
	public static String getSimpleUserName(String username) {
        return (username == null ? null : username.replaceAll("@.*", ""));
    }
}
