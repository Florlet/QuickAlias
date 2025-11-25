package com.florlet.quickalias;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom Logger wrapper.
 *
 * @author Florlet
 */
public class QuickAliasLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("quickalias");
    private static final String PREFIX = "[QuickAlias]: ";

    public static void info(String message) {
        LOGGER.info(PREFIX + "{}", message);
    }

    public static void error(String message) {
        LOGGER.error(PREFIX + "{}", message);
    }

    public static void error(String message, Throwable t) {
        LOGGER.error(PREFIX + "{}", message, t);
    }
}
