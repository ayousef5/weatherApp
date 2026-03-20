import javafx.scene.paint.Color;
import java.time.LocalDateTime;
import weather.Period;

// adapter pattern — wraps Period (the adaptee) with a client-friendly interface
public class PeriodAdapter {

    // holds the adaptee — client never touches Period directly
    private Period period;

    // constructor receives the adaptee and stores it for all method calls
    public PeriodAdapter(Period period) {
        this.period = period;
    }

    // exposes temperature as a formatted string — translates raw int to the form the client expects
    public String getDisplayTemperature() {
        // reads raw field from adaptee and appends the unit the client needs
        return period.temperature + "°F";
    }

    // exposes a Color that the client can use directly — adaptee has no such concept
    public Color getWindowColor() {
        // translates adaptee's boolean isDaytime into a Color the ui layer understands
        return period.isDaytime ? Color.web("#a8d8f0") : Color.web("#ffe066");
    }

    // exposes description safely — the adaptee field may be null, which the client should not handle
    public String getDescription() {
        // defensive adaptation: null-checks the adaptee field before passing it to the client
        return (period.shortForecast != null) ? period.shortForecast : "No data"; // null guard
    }

    // exposes precipitation as a plain int — shields the client from the adaptee's nested object
    public int getPrecipitation() {
        // adaptee stores precip in a nested wrapper; adapter flattens it for the client
        return (period.probabilityOfPrecipitation != null)
            ? period.probabilityOfPrecipitation.value : 0; // null guard on nested object
    }

    // static helper — maps a row index to a day/night boolean without exposing time logic to the client
    public static boolean isNightHour(int rowIndex) {
        // projects the current hour forward by rowIndex to get the hour for that row
        int h = (LocalDateTime.now().getHour() + rowIndex) % 24;
        // returns true for hours the client treats as nighttime
        return h >= 19 || h < 6; // night = after 7pm or before 6am
    }
}
