// random tips from adviceslip api
public class WeatherTips {

    // one api call per tip
    public static String[] fetchTips(int count) {
        String[] tips = new String[count];
        String fallback = "Stay prepared for changing conditions."; // fallback on failure
        for (int i = 0; i < count; i++) {
            try {
                java.net.URL url = new java.net.URL("https://api.adviceslip.com/advice");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Cache-Control", "no-cache"); // avoid cached response
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                if (conn.getResponseCode() == 200) {
                    java.util.Scanner sc = new java.util.Scanner(conn.getInputStream(), "UTF-8");
                    String body = sc.useDelimiter("\\A").next();
                    sc.close();
                    int adviceKey = body.indexOf("\"advice\""); // find advice key
                    if (adviceKey != -1) {
                        int colon  = body.indexOf(":", adviceKey);
                        int openQ  = body.indexOf("\"", colon); // extract quoted string
                        int closeQ = body.indexOf("\"", openQ + 1);
                        if (openQ != -1 && closeQ > openQ)
                            tips[i] = body.substring(openQ + 1, closeQ);
                        else
                            tips[i] = fallback;
                    } else {
                        tips[i] = fallback;
                    }
                } else {
                    tips[i] = fallback;
                }
                conn.disconnect();
                Thread.sleep(150); // avoid duplicate response
            } catch (Exception e) {
                tips[i] = fallback; // any failure uses fallback
            }
        }
        return tips;
    }
}
