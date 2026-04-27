# Weather App

  A JavaFX desktop weather application that displays real-time forecasts with animated weather effects. Built
  using the free National Weather Service API — no API key required.

  ## Features

  - **Today View** — current temperature, hourly forecast with precipitation %, and high/low for the day
  - **Weekly Forecast View** — four-day forecast cards with temperature, wind speed/direction, and precipitation
   probability
  - **Dynamic weather animations** — background animates based on conditions (rain, snow, clouds, clear)
  - **Day/Night visuals** — sky, mountain shading, and window colors adjust based on the time of day
  - **Interactive hourly rows** — clicking an hour updates the main temperature display and background
  - **Daily weather tips** sourced from an external advice API
  - **Frosted glass UI panels** with Gaussian blur effects
  - **Real-time clock** that refreshes every minute
  - **Responsive scaling** to fit any monitor resolution

  ## Screenshots

  > *(Add screenshots here)*

  ## Tech Stack

  | Layer | Technology |
  |---|---|
  | Language | Java 11 |
  | UI Framework | JavaFX 19 |
  | Build Tool | Maven |
  | Weather Data | [National Weather Service API](https://www.weather.gov/documentation/services-web-api) |
  | JSON Parsing | Jackson Databind 2.11 |
  | Testing | JUnit Jupiter 5.9 |

  ## Project Structure

  ```
  weatherApp/
  ├── src/
  │   └── main/
  │       ├── java/
  │       │   ├── JavaFX.java           # Main app entry point & scene controller
  │       │   ├── MyWeatherAPI.java     # App-specific API extension
  │       │   ├── SceneBuilder.java     # UI scene construction
  │       │   ├── WeatherEffect.java    # Weather animations & visual effects
  │       │   ├── WeatherTips.java      # Daily tips display
  │       │   ├── PeriodAdapter.java    # Forecast period data adapter
  │       │   └── weather/
  │       │       ├── WeatherAPI.java              # HTTP client for api.weather.gov
  │       │       ├── Root.java                    # Top-level JSON model
  │       │       ├── Properties.java              # Forecast properties model
  │       │       ├── Period.java                  # Individual forecast period
  │       │       ├── ProbabilityOfPrecipitation.java
  │       │       ├── Elevation.java
  │       │       └── Geometry.java
  │       └── resources/
  │           ├── scene1Day.png         # Day background
  │           ├── scene1Night.png       # Night background
  │           ├── scene2Day.png
  │           └── scene2Night.png
  └── pom.xml
  ```
  ## Getting Started

  ### Prerequisites

  - Java 11+
  - Maven 3.x

  ### Build & Run

  ```bash
  git clone https://github.com/ayousef5/weatherApp.git
  cd weatherApp
  mvn javafx:run

  How It Works

  The app calls the National Weather Service API using a grid-based location system:

  https://api.weather.gov/gridpoints/{region}/{gridX},{gridY}/forecast

  You can find the grid coordinates for any US location using the NWS Points API:

  https://api.weather.gov/points/{latitude},{longitude}
