package com.sarvadalabs.automation.util;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses displayed prices such as "$1,234.50 CAD", "Rs. 1,299.00" or "€12,50" into numbers. */
public final class Money {

    private static final Pattern AMOUNT = Pattern.compile("(\\d[\\d,]*)(?:[.,](\\d{1,2}))?");

    private Money() {
    }

    public static BigDecimal parse(String displayed) {
        Matcher m = AMOUNT.matcher(displayed);
        if (!m.find()) {
            throw new IllegalArgumentException("No monetary amount found in '" + displayed + "'");
        }
        String whole = m.group(1).replace(",", "");
        String fraction = m.group(2) == null ? "00" : m.group(2);
        return new BigDecimal(whole + "." + fraction);
    }
}
