package com.github.mateohi.poc7_ocr;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.regex.Pattern;

public class OcrUtils {

    private static final String PRICE_FORMAT = "%s %s";

    public static String extractPrice(String text, List<String> symbols) {
        List<String> possiblePrices = extractPossiblePrices(text, symbols);

        int matches = possiblePrices.size();

        if (matches == 0) {
            return null;
        } else if (matches == 1) {
            return possiblePrices.get(0);
        } else {
            throw new RuntimeException("Unable to identify between several possible prices");
        }
    }

    private static List<String> extractPossiblePrices(String text, List<String> symbols) {
        List<String> possiblePrices = Lists.newArrayList();

        for (String symbol : symbols) {
            if (text.contains(symbol)) {
                String[] subTexts = text.trim().split(Pattern.quote(symbol));

                for (String subText : subTexts) {
                    String value = subText.trim().split(Pattern.quote(" "))[0];

                    if (isNumber(value)) {
                        possiblePrices.add(String.format(PRICE_FORMAT, symbol, value));
                    }
                }
            }
        }
        return possiblePrices;
    }

    private static boolean isNumber(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
