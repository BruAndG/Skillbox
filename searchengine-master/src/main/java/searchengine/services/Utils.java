package searchengine.services;

public class Utils {
    public static String getRegexToFilterUrl(String url) {
        if (!url.startsWith("http")) {
            return null;
        }

        String[] str1 = url.split("/");
        if ((str1.length < 3) || (str1[2] == null)) {
            return null;
        }

        StringBuilder result = new StringBuilder("http[s]?://(www\\.)?");
        String[] str2 = str1[2].split("\\.");
        for (int i = 0; i < str2.length; i++) {
            if (!((i == 0) && str2[i].equals("www"))) {
                result.append(str2[i]);
                if (i != str2.length - 1) {
                    result.append(".");
                }
            }
        }
        result.append(".*");

        return result.toString();
    }

    public static boolean isCorrectDomain(String url, String regex) {
        return ((regex == null) || url.matches(regex));
    }

    public static boolean isFile(String url) {
        return url.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|webp|pdf|eps|xlsx|xls|doc|docx|ppts|icon|bmp)$");
    }
}
