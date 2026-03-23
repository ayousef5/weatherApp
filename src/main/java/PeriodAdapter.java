import javafx.scene.paint.Color;
import java.time.LocalDateTime;
import weather.Period;

// class helps us use Period data in an easier way by using the adapter design pattern
public class PeriodAdapter {

    // stores the weather data
    private Period period;

    // runs when we create a PeriodAdapter
    public PeriodAdapter(Period period) {
        this.period = period;
    }

    // gets the temperature and turns it into a string like "72°F"
    public String getDisplayTemperature() {
        return period.temperature + "°F";
    }

    // gives a color depending on day or night
    public Color getWindowColor() {
        return period.isDaytime ? Color.web("#a8d8f0") : Color.web("#ffe066");
    }
    // gets the weather description safely
    public String getDescription() {
        return (period.shortForecast != null) ? period.shortForecast : "No data"; // null guard
    }
     // gets us the change of rain or snow
    public int getPrecipitation() {
        // if it exists return the number otherwise return 0 so that app doesnt crash
        return (period.probabilityOfPrecipitation != null)
            ? period.probabilityOfPrecipitation.value : 0;
    }
    // checks if a future hour should be night or day
    public static boolean isNightHour(int rowIndex) {
        int h = (LocalDateTime.now().getHour() + rowIndex) % 24; // get the hour for that row
        return h >= 19 || h < 6; // night = after 7pm or before 6am
    }
}
