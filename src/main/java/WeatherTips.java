// random tips of the day from adviceslip api
public class WeatherTips {

    // one api call per tip
    public static String[] fetchTips(int count) {
        String[] tips = new String[count]; // an array to store all the tips
        String fallback = "Stay prepared for changing conditions."; // in case it did not work or crash. this is the default message
        for (int i = 0; i < count; i++) {
            try {
                // link to the advise website
                java.net.URL url = new java.net.URL("https://api.adviceslip.com/advice");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Cache-Control", "no-cache"); // avoid cached response
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                // check if the request worked
                if (conn.getResponseCode() == 200) {
                    java.util.Scanner sc = new java.util.Scanner(conn.getInputStream(), "UTF-8");
                    String body = sc.useDelimiter("\\A").next();
                    sc.close(); // close the scanner
                    int adviceKey = body.indexOf("\"advice\"");
                    if (adviceKey != -1) {
                        int colon  = body.indexOf(":", adviceKey);
                        int openQ  = body.indexOf("\"", colon);
                        int closeQ = body.indexOf("\"", openQ + 1);
                        if (openQ != -1 && closeQ > openQ)
                            tips[i] = body.substring(openQ + 1, closeQ);
                        else
                            tips[i] = fallback;
                    } else {
                        tips[i] = fallback; // if "advice" was not found, use the default message
                    }
                } else {
                    tips[i] = fallback;
                }
                conn.disconnect();
                Thread.sleep(150); // wait a little so we don't get the same Tip message again
            } catch (Exception e) {
                tips[i] = fallback; // // if anything goes wrong, use the default message
            }
        }
        return tips; // return all the tips we collected as an array
    }
}
