package org.ualberta.smr.explainmergeconflict.utils;

public class StringUtils {
    
    public static void getFirstLine(String result, int index) {
        for (int i = index + 1; i < result.length(); i++) {
            if ((result.charAt(i)) == ' ') {
                System.out.println(i);
            }
        }
    }
}
