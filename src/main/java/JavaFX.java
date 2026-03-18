import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.geometry.*;
import javafx.util.Duration;

import weather.Period;
import weather.WeatherAPI;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class
JavaFX extends Application {

    private Stage primaryStage;
    Scene todayScene;
    Scene forecastScene;

    static final double W = 900, H = 500;
    static final double GRASS_Y = 495;

    // Mutable UI refs — updated when a row is clicked
    private Label bigTempLbl;
    private Label topDescLbl;
    private Label topHiLoLbl;

    // Weather effects layer + animation state
    private Pane effectsPane;
    private Timeline weatherTimeline;
    private Animation currentFadeAnim;
    private Timeline clockTimeline;

    private enum WeatherState { CLEAR, CLOUDY, RAIN, SNOW }

    public static void main(String[] args) { launch(args); }

    // Called by "Later this week" button — classmate can wire this to their Scene 2
    public void showScene2() {
        primaryStage.setScene(forecastScene);
        primaryStage.setFullScreen(true);
    }

    // -------------------------------------------------------
    // Helpers preserved from original
    // -------------------------------------------------------
    private Label styledLabel(String text, double size, boolean bold, Color color) {
        Label l = new Label(text);
        l.setFont(bold ? Font.font("Arial", FontWeight.BOLD, size) : Font.font("Arial", size));
        l.setTextFill(color);
        l.setTextAlignment(TextAlignment.CENTER);
        l.setWrapText(true);
        l.setMaxWidth(200);
        return l;
    }

    private Label makeWrappingLabel(String text, double maxW) {
        Label l = new Label(text);
        l.setFont(Font.font("Arial", 11));
        l.setTextFill(Color.rgb(50, 50, 50));
        l.setWrapText(true);
        l.setMaxWidth(maxW);
        l.setTextAlignment(TextAlignment.LEFT);
        return l;
    }

    private String[] fetchTips(int count) {
        String[] tips = new String[count];
        String fallback = "Stay prepared for changing conditions.";
        for (int i = 0; i < count; i++) {
            try {
                java.net.URL url = new java.net.URL("https://api.adviceslip.com/advice");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Cache-Control", "no-cache");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                if (conn.getResponseCode() == 200) {
                    java.util.Scanner sc = new java.util.Scanner(conn.getInputStream(), "UTF-8");
                    String body = sc.useDelimiter("\\A").next();
                    sc.close();
                    int adviceKey = body.indexOf("\"advice\"");
                    if (adviceKey != -1) {
                        int colon = body.indexOf(":", adviceKey);
                        int openQ = body.indexOf("\"", colon);
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
                Thread.sleep(150);
            } catch (Exception e) {
                tips[i] = fallback;
            }
        }
        return tips;
    }

    // -------------------------------------------------------
    // start()
    // -------------------------------------------------------
    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        stage.setTitle("Weather App");

        LocalDateTime now = LocalDateTime.now();
        int hour   = now.getHour();
        int minute = now.getMinute();
        boolean isNight = (hour >= 18 || hour < 6);

        ArrayList<Period> forecast = WeatherAPI.getForecast("LOT", 77, 70);
        if (forecast == null || forecast.size() < 8)
            throw new RuntimeException("Forecast did not load properly");

        // ====================================================
        // SCENE 1 — scaled to fill the primary screen
        // ====================================================
        Rectangle2D screen = Screen.getPrimary().getBounds();
        double SW = screen.getWidth(), SH = screen.getHeight();

        StackPane scene1Root = buildScene1(forecast, hour, minute, isNight);
        // Scale the 900×500 design space up to fill the actual screen
        scene1Root.getTransforms().add(
            new javafx.scene.transform.Scale(SW / W, SH / H, 0, 0));
        // Wrap in a plain Group so the Scene adopts SW×SH without re-laying-out the pane
        javafx.scene.Group fsRoot = new javafx.scene.Group(scene1Root);
        todayScene = new Scene(fsRoot, SW, SH);

        // ====================================================
        // SCENE 2 — preserved exactly from classmate's build
        // ====================================================
        String scene2Bg = isNight ? "/scene2Night.png" : "/scene2Day.png";
        Image bgImg2 = new Image(getClass().getResource(scene2Bg).toExternalForm());
        ImageView bg2 = new ImageView(bgImg2);
        bg2.setFitWidth(960); bg2.setFitHeight(540); bg2.setPreserveRatio(false);

        Button backButton = new Button("← Back to today!");
        backButton.setStyle(
            "-fx-background-color: #7B5EA7; -fx-background-radius: 24; -fx-text-fill: white;" +
            "-fx-font-family: 'Arial'; -fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-padding: 8 18 8 18; -fx-cursor: hand;"
        );

        Label cityLabel2 = styledLabel("Chicago", 28, true, Color.BLACK);
        BorderPane topBar = new BorderPane();
        topBar.setLeft(backButton);
        topBar.setCenter(cityLabel2);
        BorderPane.setAlignment(backButton, Pos.CENTER_LEFT);
        BorderPane.setAlignment(cityLabel2, Pos.CENTER);
        topBar.setPadding(new Insets(14, 20, 6, 16));

        String[] tips = fetchTips(4);
        int[] dayIdx   = {0, 2, 4, 6};
        int[] nightIdx = {1, 3, 5, 7};

        HBox cardsRow = new HBox(10);
        cardsRow.setAlignment(Pos.TOP_CENTER);
        cardsRow.setPadding(new Insets(4, 10, 10, 10));

        for (int c = 0; c < 4; c++) {
            Period dayP   = forecast.get(dayIdx[c]);
            Period nightP = forecast.get(nightIdx[c]);
            final double CARD_W = 210;

            String dayName   = (c == 0) ? "Today" : dayP.name;
            Label cardHeader = styledLabel(dayName, 16, true, Color.BLACK);
            Label cardTemp   = styledLabel(dayP.temperature + "°", 16, true, Color.BLACK);
            HBox cardTitle   = new HBox(6, cardHeader, cardTemp);
            cardTitle.setAlignment(Pos.CENTER);

            Region headerSep = new Region();
            headerSep.setPrefHeight(2); headerSep.setPrefWidth(CARD_W);
            headerSep.setStyle("-fx-background-color: rgba(100,100,180,0.4);");

            Label dayIconLbl = new Label("☀ DAY:");
            dayIconLbl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            dayIconLbl.setTextFill(Color.rgb(50, 50, 50));
            Label dayTempLbl = makeWrappingLabel("Temp: " + dayP.temperature + "°F", CARD_W);
            Label dayWindLbl = makeWrappingLabel("Wind: " + dayP.windDirection + " " + dayP.windSpeed, CARD_W);
            Label dayPrecLbl = makeWrappingLabel("Precipitation: " + dayP.probabilityOfPrecipitation.value + "%", CARD_W);
            Label dayDescLbl = makeWrappingLabel(dayP.shortForecast, CARD_W);
            dayDescLbl.setTextFill(Color.rgb(70, 70, 70));
            dayDescLbl.setFont(Font.font("Arial", FontPosture.ITALIC, 11));
            VBox daySection = new VBox(3, dayIconLbl, dayTempLbl, dayWindLbl, dayPrecLbl, dayDescLbl);
            daySection.setAlignment(Pos.CENTER_LEFT);

            Region midSep = new Region();
            midSep.setPrefHeight(1); midSep.setPrefWidth(CARD_W);
            midSep.setStyle("-fx-background-color: rgba(100,100,180,0.25);");

            Label nightIconLbl = new Label("🌙 NIGHT:");
            nightIconLbl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            nightIconLbl.setTextFill(Color.rgb(50, 50, 50));
            Label nightTempLbl = makeWrappingLabel("Temp: " + nightP.temperature + "°F", CARD_W);
            Label nightWindLbl = makeWrappingLabel("Wind: " + nightP.windDirection + " " + nightP.windSpeed, CARD_W);
            Label nightPrecLbl = makeWrappingLabel("Precipitation: " + nightP.probabilityOfPrecipitation.value + "%", CARD_W);
            Label nightDescLbl = makeWrappingLabel(nightP.shortForecast, CARD_W);
            nightDescLbl.setTextFill(Color.rgb(70, 70, 70));
            nightDescLbl.setFont(Font.font("Arial", FontPosture.ITALIC, 11));
            VBox nightSection = new VBox(3, nightIconLbl, nightTempLbl, nightWindLbl, nightPrecLbl, nightDescLbl);
            nightSection.setAlignment(Pos.CENTER_LEFT);

            Region botSep = new Region();
            botSep.setPrefHeight(1); botSep.setPrefWidth(CARD_W);
            botSep.setStyle("-fx-background-color: rgba(100,100,180,0.25);");

            Label tipIconLbl = new Label("💡 Tip of the day");
            tipIconLbl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            tipIconLbl.setTextFill(Color.rgb(50, 50, 50));
            Label tipText = makeWrappingLabel("\"" + tips[c] + "\"", CARD_W);
            tipText.setTextFill(Color.rgb(70, 70, 70));
            tipText.setFont(Font.font("Arial", FontPosture.ITALIC, 11));
            tipText.setTextAlignment(TextAlignment.CENTER);
            tipText.setAlignment(Pos.CENTER);
            VBox tipSection = new VBox(4, tipIconLbl, tipText);
            tipSection.setAlignment(Pos.CENTER);

            VBox cardContent = new VBox(8, cardTitle, headerSep, daySection, midSep, nightSection, botSep, tipSection);
            cardContent.setAlignment(Pos.TOP_LEFT);
            cardContent.setPadding(new Insets(10));
            cardContent.setPrefWidth(CARD_W); cardContent.setMaxWidth(CARD_W);

            Region cardBg = new Region();
            cardBg.setStyle(
                "-fx-background-color: rgba(200,210,235,0.55); -fx-background-radius: 18;" +
                "-fx-border-color: rgba(255,255,255,0.6); -fx-border-radius: 18; -fx-border-width: 1.2;"
            );
            cardBg.setEffect(new GaussianBlur(18));

            StackPane card = new StackPane(cardBg, cardContent);
            card.setMaxWidth(CARD_W + 20);
            card.setAlignment(Pos.TOP_LEFT);
            Rectangle cardClip = new Rectangle();
            cardClip.setArcWidth(36); cardClip.setArcHeight(36);
            card.setClip(cardClip);
            card.layoutBoundsProperty().addListener((obs, o, n) -> {
                cardClip.setWidth(n.getWidth()); cardClip.setHeight(n.getHeight());
            });
            cardsRow.getChildren().add(card);
        }

        VBox cardsWrapper = new VBox(cardsRow);
        cardsWrapper.setAlignment(Pos.TOP_CENTER);
        VBox scene2Layout = new VBox(0, topBar, cardsWrapper);
        scene2Layout.setAlignment(Pos.TOP_CENTER);
        StackPane root2 = new StackPane(bg2, scene2Layout);
        // Scale scene 2 to fill the same full screen as scene 1
        root2.getTransforms().add(
            new javafx.scene.transform.Scale(SW / 960.0, SH / 540.0, 0, 0));
        javafx.scene.Group fsRoot2 = new javafx.scene.Group(root2);
        forecastScene = new Scene(fsRoot2, SW, SH);

        backButton.setOnAction(e -> {
            primaryStage.setScene(todayScene);
            primaryStage.setFullScreen(true);
        });

        primaryStage.setScene(todayScene);
        primaryStage.setFullScreenExitHint("");
        primaryStage.setFullScreen(true);
        primaryStage.show();
    }

    // ====================================================
    // SCENE 1 — top-level builder
    // ====================================================
    private StackPane buildScene1(ArrayList<Period> forecast, int hour, int minute, boolean isNight) {
        StackPane root = new StackPane();

        Pane bgPane  = buildBackground(hour, minute, isNight);
        Pane housePn = buildHouse(isNight);

        effectsPane = new Pane();
        effectsPane.setPrefSize(W, H);
        effectsPane.setPickOnBounds(false);
        effectsPane.setMouseTransparent(true); // never steal clicks from the UI layer

        Pane uiPane  = buildUI(forecast, root);

        root.getChildren().addAll(bgPane, housePn, effectsPane, uiPane);

        // Kick off initial weather state from first forecast period
        switchWeatherEffect(getWeatherState(forecast.get(0)));
        return root;
    }

    // ====================================================
    // BACKGROUND — sky, stars, sun/moon, mountains, grass
    // ====================================================
    private Pane buildBackground(int hour, int minute, boolean isNight) {
        Pane pane = new Pane();
        pane.setPrefSize(W, H);

        // Sky rectangle
        Rectangle sky = new Rectangle(0, 0, W, H);
        sky.setFill(buildSkyGradient(hour, minute, isNight));
        pane.getChildren().add(sky);

        // Stars (night only)
        if (isNight) {
            Random rng = new Random(42);
            for (int i = 0; i < 60; i++) {
                double sx = rng.nextDouble() * W;
                double sy = rng.nextDouble() * (GRASS_Y - 30);
                double sr = 0.6 + rng.nextDouble() * 1.4;
                Circle star = new Circle(sx, sy, sr, Color.WHITE);
                star.setOpacity(0.45 + rng.nextDouble() * 0.55);
                pane.getChildren().add(star);
            }
        }

        // Sun (day) or Moon (night)
        if (!isNight) {
            // Semicircular arc: left horizon at 6am, apex at noon, right horizon at 6pm
            double t     = Math.max(0, Math.min(1, (hour + minute / 60.0 - 6.0) / 12.0));
            double angle = Math.PI * (1.0 - t);
            double R     = 270;
            double cx    = W / 2.0 + R * Math.cos(angle);
            double cy    = GRASS_Y  - R * Math.sin(angle);
            Circle sun = new Circle(cx, cy, 26, Color.web("#FFD700"));
            sun.setEffect(new Glow(0.8));
            pane.getChildren().add(sun);
        } else {
            // Moon — top-right corner with crescent overlay
            // Crescent circle is filled with the exact sky background color at that Y
            // so it blends perfectly and creates a clean crescent illusion.
            double mx = W - 85, my = 68;
            double op = getMoonOpacity(hour);
            // Interpolate the night sky gradient at moon's Y position
            double t = Math.max(0, Math.min(1, my / H));
            Color skyTop = Color.web("#03030f");
            Color skyBot = Color.web("#0d1b4b");
            // Near sunrise/sunset the gradient differs but moon only shows at night
            Color crescentColor = skyTop.interpolate(skyBot, t);
            Circle moon     = new Circle(mx, my, 24, Color.web("#FFFDE7"));
            moon.setOpacity(op);
            Circle crescent = new Circle(mx + 10, my - 5, 21, crescentColor);
            // Crescent must be fully opaque and same opacity as moon for clean edge
            crescent.setOpacity(op);
            pane.getChildren().addAll(moon, crescent);
        }

        // -------------------------------------------------------
        // MOUNTAINS — back layer: gradient fill + jagged 5-point ridgelines
        // -------------------------------------------------------
        Color mtPeak = isNight ? Color.web("#2a4a6a") : Color.web("#7a9abf");
        Color mtBase = isNight ? Color.web("#0a1a2a") : Color.web("#3a5a7a");
        // Back mountains are slightly dimmer
        Color mtPeakBack = isNight ? Color.web("#1e3a52") : Color.web("#5a7a9a");
        Color mtBaseBack = isNight ? Color.web("#071422") : Color.web("#2a4560");

        // mtL1 — back-left (dimmer)
        Polygon mtL1 = new Polygon(
            0,   GRASS_Y,
            65,  GRASS_Y - 45,
            130, GRASS_Y - 118,
            202, GRASS_Y - 52,
            275, GRASS_Y);
        mtL1.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0, mtPeakBack), new Stop(1, mtBaseBack)));

        // mtL2 — front-left, tallest left peak
        Polygon mtL2 = new Polygon(
            55,  GRASS_Y,
            130, GRASS_Y - 68,
            205, GRASS_Y - 155,
            290, GRASS_Y - 75,
            375, GRASS_Y);
        mtL2.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0, mtPeak), new Stop(1, mtBase)));

        // mtR1 — front-right, tallest right peak
        Polygon mtR1 = new Polygon(
            525, GRASS_Y,
            600, GRASS_Y - 72,
            675, GRASS_Y - 152,
            757, GRASS_Y - 80,
            840, GRASS_Y);
        mtR1.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0, mtPeak), new Stop(1, mtBase)));

        // mtR2 — back-right (dimmer)
        Polygon mtR2 = new Polygon(
            705, GRASS_Y,
            757, GRASS_Y - 55,
            810, GRASS_Y - 112,
            855, GRASS_Y - 40,
            900, GRASS_Y);
        mtR2.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0, mtPeakBack), new Stop(1, mtBaseBack)));

        pane.getChildren().addAll(mtL1, mtL2, mtR1, mtR2);

        // (snow caps removed)

        // Foreground mountain layer — smaller, darker, closer, also jagged
        Color fgPeak = isNight ? Color.web("#1a2a3a") : Color.web("#4a6a7a");
        Color fgBase = isNight ? Color.web("#0a1520") : Color.web("#2a3a4a");

        Polygon fgL1 = new Polygon(
            0,   GRASS_Y,
            80,  GRASS_Y - 38,
            160, GRASS_Y - 70,
            235, GRASS_Y - 50,
            310, GRASS_Y);
        fgL1.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0, fgPeak), new Stop(1, fgBase)));

        Polygon fgL2 = new Polygon(
            80,  GRASS_Y,
            155, GRASS_Y - 50,
            240, GRASS_Y - 90,
            330, GRASS_Y - 55,
            420, GRASS_Y);
        fgL2.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0, fgPeak), new Stop(1, fgBase)));

        Polygon fgR1 = new Polygon(
            480, GRASS_Y,
            565, GRASS_Y - 52,
            650, GRASS_Y - 88,
            715, GRASS_Y - 60,
            780, GRASS_Y);
        fgR1.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0, fgPeak), new Stop(1, fgBase)));

        Polygon fgR2 = new Polygon(
            650, GRASS_Y,
            720, GRASS_Y - 45,
            795, GRASS_Y - 72,
            850, GRASS_Y - 42,
            900, GRASS_Y);
        fgR2.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
            new Stop(0, fgPeak), new Stop(1, fgBase)));

        pane.getChildren().addAll(fgL1, fgL2, fgR1, fgR2);


        return pane;
    }

    // ====================================================
    // HOUSE — all JavaFX shapes, no images
    // ====================================================
    private Pane buildHouse(boolean isNight) {
        Pane pane = new Pane();
        pane.setPrefSize(W, H);
        pane.setPickOnBounds(false);

        Color wall    = Color.web("#E8DCC8");
        Color roofCol = isNight ? Color.web("#3a2a1a") : Color.web("#8B4513");
        Color winCol  = isNight ? Color.web("#ffe066") : Color.web("#a8d8f0");
        Color trim    = Color.web("#D4C4A8");
        Color stroke  = Color.web("#C0B0A0");

        // --- Chimney (drawn first so roof overlaps it) ---
        // Chimney top is above the roof peak (y=313), pokes out visually
        rect(pane, 455, 310, 20, 90, Color.web("#A89888"), stroke, 1);
        rect(pane, 451, 304, 30, 9,  Color.web("#B8A898"), stroke, 1);

        // --- Main body: x=335 y=388 w=185 h=107 (bottom=GRASS_Y=495) ---
        rect(pane, 335, 388, 185, 107, wall, stroke, 1);

        // --- Roof: overhangs 15px each side, peak at x=427, y=313 ---
        Polygon roof = poly(320, 388,  540, 388,  427, 313);
        roof.setFill(roofCol);
        roof.setStroke(Color.web("#5a3010")); roof.setStrokeWidth(1.5);
        pane.getChildren().add(roof);

        // --- Garage body: x=520 y=420 w=98 h=75 (bottom=GRASS_Y=495) ---
        rect(pane, 520, 420, 98, 75, Color.web("#DCCFB8"), stroke, 1);

        // --- Garage roof: peak at x=569, y=385 ---
        Polygon garageRoof = poly(510, 420,  628, 420,  569, 385);
        garageRoof.setFill(roofCol);
        garageRoof.setStroke(Color.web("#5a3010")); garageRoof.setStrokeWidth(1);
        pane.getChildren().add(garageRoof);

        // --- Garage door with 3 horizontal panel dividers ---
        rect(pane, 530, 433, 78, 62, Color.web("#C0B0A0"), Color.web("#8a7a6a"), 1);
        for (int li = 1; li <= 3; li++) {
            double lineY = 433 + li * (62.0 / 4.0);
            line(pane, 530, lineY, 608, lineY, Color.web("#8a7a6a"), 1);
        }

        // --- Front door: x=406 y=437 w=42 h=58 (bottom=GRASS_Y=495) ---
        rect(pane, 406, 437, 42, 58, Color.web("#6B3A2A"), Color.web("#4a2a1a"), 1);
        // Rounded arch on top
        Arc arch = new Arc(427, 437, 21, 15, 0, 180);
        arch.setType(ArcType.CHORD);
        arch.setFill(Color.web("#6B3A2A"));
        arch.setStroke(Color.web("#4a2a1a")); arch.setStrokeWidth(1);
        pane.getChildren().add(arch);
        // Door knob
        pane.getChildren().add(new Circle(443, 466, 3, Color.web("#D4AF37")));
        // Step
        rect(pane, 400, 491, 54, 4, trim, stroke, 1);

        // --- Left window (4 panes): x=344 y=405 w=54 h=42 ---
        rect(pane, 344, 405, 54, 42, winCol, stroke, 1.5);
        line(pane, 371, 405, 371, 447, stroke, 1.5); // vertical divider
        line(pane, 344, 426, 398, 426, stroke, 1.5); // horizontal divider
        rect(pane, 340, 446, 62, 5, trim, stroke, 0); // ledge

        // --- Right window (mirroring left, right side of door): x=456 y=405 w=54 h=42 ---
        rect(pane, 456, 405, 54, 42, winCol, stroke, 1.5);
        line(pane, 483, 405, 483, 447, stroke, 1.5); // vertical divider
        line(pane, 456, 426, 510, 426, stroke, 1.5); // horizontal divider
        rect(pane, 452, 446, 62, 5, trim, stroke, 0); // ledge

        return pane;
    }

    // ====================================================
    // UI OVERLAY — left panel, center labels, top-right info, right buttons
    // ====================================================
    /** Returns the time label for row i relative to the current hour. */
    private static String hourLabel(int i) {
        if (i == 0) return "Now";
        int h = (LocalDateTime.now().getHour() + i) % 24;
        if (h == 0)       return "12 AM";
        else if (h < 12)  return h + " AM";
        else if (h == 12) return "12 PM";
        else              return (h - 12) + " PM";
    }

    private static String weatherEmoji(String forecast) {
        if (forecast == null) return "🌡";
        String f = forecast.toLowerCase();
        if (f.contains("thunderstorm") || f.contains("thunder"))  return "⛈";
        if (f.contains("blizzard"))                                return "🌨";
        if (f.contains("snow") || f.contains("snowy"))            return "❄";
        if (f.contains("sleet") || f.contains("freezing rain"))   return "🌨";
        if (f.contains("rain") || f.contains("rainy") || f.contains("shower")) return "🌧";
        if (f.contains("drizzle"))                                 return "🌦";
        if (f.contains("fog") || f.contains("mist"))              return "🌫";
        if (f.contains("wind") || f.contains("breezy") || f.contains("blustery")) return "💨";
        if (f.contains("mostly cloudy") || f.contains("overcast")) return "☁";
        if (f.contains("partly cloudy") || f.contains("partly sunny")) return "⛅";
        if (f.contains("mostly clear") || f.contains("mostly sunny")) return "🌤";
        if (f.contains("sunny") || f.contains("clear"))           return "☀";
        if (f.contains("cloud"))                                   return "🌥";
        return "🌡";
    }

    private Pane buildUI(ArrayList<Period> forecast, StackPane rootRef) {
        Pane pane = new Pane();
        pane.setPrefSize(W, H);
        pane.setPickOnBounds(false);

        // ---- Left panel: frosted glass with HOURLY FORECAST header ----
        final double PANEL_W = 215;
        final double ROW_W   = PANEL_W - 12; // inner row width

        // Header label
        Label headerLbl = new Label("HOURLY FORECAST");
        headerLbl.setFont(Font.font("Arial", FontWeight.BOLD, 9));
        headerLbl.setTextFill(Color.web("#aac4e0"));
        headerLbl.setMaxWidth(ROW_W);
        headerLbl.setPadding(new Insets(0, 0, 4, 0));

        // Thin separator under header
        Region headerSep = new Region();
        headerSep.setPrefHeight(1);
        headerSep.setPrefWidth(ROW_W);
        headerSep.setStyle("-fx-background-color: rgba(255,255,255,0.15);");

        int numRows = Math.min(10, forecast.size());
        final int[] selectedIdx = {0};
        List<HBox> rowList = new ArrayList<>();
        VBox rowsBox = new VBox(0);

        // Keep refs to time labels so the clock ticker can update them each minute
        Label[] timeLabels = new Label[numRows];

        for (int i = 0; i < numRows; i++) {
            Period p      = forecast.get(i);
            final int idx = i;

            Label timeLbl = new Label(hourLabel(i));
            timeLbl.setFont(Font.font("Arial", 10));
            timeLbl.setTextFill(Color.web("#ddeeff"));
            timeLbl.setPrefWidth(72);
            timeLbl.setMinWidth(72);
            timeLabels[i] = timeLbl;

            // --- weather icon (col 2) — emoji based on shortForecast ---
            Label iconLbl = new Label(weatherEmoji(p.shortForecast));
            iconLbl.setFont(Font.font("Arial", 13));
            StackPane iconCell = new StackPane(iconLbl);
            iconCell.setPrefWidth(24);
            iconCell.setMinWidth(24);
            iconCell.setAlignment(Pos.CENTER);

            // --- precip % (col 3) ---
            int precip = (p.probabilityOfPrecipitation != null) ? p.probabilityOfPrecipitation.value : 0;
            Label precipLbl = new Label(precip + "%");
            precipLbl.setFont(Font.font("Arial", 9));
            precipLbl.setTextFill(Color.web("#7ab8f5"));
            precipLbl.setOpacity(0.85);
            precipLbl.setPrefWidth(28);
            precipLbl.setMinWidth(28);
            precipLbl.setAlignment(Pos.CENTER_RIGHT);

            // --- temp (col 4) ---
            Label tempLbl = new Label(p.temperature + "°F");
            tempLbl.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            tempLbl.setTextFill(Color.WHITE);
            tempLbl.setPrefWidth(36);
            tempLbl.setAlignment(Pos.CENTER_RIGHT);

            HBox row = new HBox(0, timeLbl, iconCell, precipLbl, tempLbl);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(5, 6, 5, 6));
            row.setPrefWidth(ROW_W);

            // Selected style: solid purple fill, brighter text
            // Unselected: transparent
            boolean isSelected = (i == 0);
            applyRowStyle(row, timeLbl, precipLbl, tempLbl, isSelected);

            row.setOnMouseClicked(e -> {
                selectedIdx[0] = idx;
                for (int j = 0; j < rowList.size(); j++) {
                    HBox r = rowList.get(j);
                    Label tl  = (Label) r.getChildren().get(0);
                    Label pl  = (Label) r.getChildren().get(2);
                    Label tpL = (Label) r.getChildren().get(3);
                    applyRowStyle(r, tl, pl, tpL, j == idx);
                }
                // Update center temp
                bigTempLbl.setText(p.temperature + "°F");
                // Update top-right description
                topDescLbl.setText(p.shortForecast != null ? p.shortForecast : "");
                // Recalculate H/L from full forecast list
                int hi2 = Integer.MIN_VALUE, lo2 = Integer.MAX_VALUE;
                for (Period fp : forecast) {
                    if (fp.temperature > hi2) hi2 = fp.temperature;
                    if (fp.temperature < lo2) lo2 = fp.temperature;
                }
                topHiLoLbl.setText("H: " + hi2 + "°   L: " + lo2 + "°");
                // Switch weather effects
                switchWeatherEffect(getWeatherState(p));
            });
            row.setOnMouseEntered(e -> {
                if (selectedIdx[0] != idx)
                    row.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 6;");
            });
            row.setOnMouseExited(e -> {
                if (selectedIdx[0] != idx)
                    row.setStyle("-fx-background-color: transparent;");
            });

            rowList.add(row);
            rowsBox.getChildren().add(row);
        }

        // Clock ticker — fires every 60 s, refreshes time labels from current system clock
        if (clockTimeline != null) clockTimeline.stop();
        clockTimeline = new Timeline(new KeyFrame(Duration.minutes(1), e -> {
            for (int i = 0; i < timeLabels.length; i++)
                timeLabels[i].setText(hourLabel(i));
        }));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();

        // Assemble inner content: header + sep + rows
        VBox panelContent = new VBox(0, headerLbl, headerSep, rowsBox);
        panelContent.setPadding(new Insets(10, 6, 10, 6));
        panelContent.setMaxWidth(PANEL_W);

        // Frosted glass background
        Region panelBg = new Region();
        panelBg.setStyle(
            "-fx-background-color: rgba(6,14,42,0.62); -fx-background-radius: 14;" +
            "-fx-border-color: rgba(255,255,255,0.14); -fx-border-radius: 14; -fx-border-width: 1;"
        );
        panelBg.setEffect(new GaussianBlur(14));

        StackPane leftPanel = new StackPane(panelBg, panelContent);
        leftPanel.setAlignment(Pos.TOP_LEFT);
        leftPanel.setMaxWidth(PANEL_W);
        leftPanel.setLayoutX(20);
        leftPanel.setLayoutY(45);   // start near top, panel fills most of screen height
        pane.getChildren().add(leftPanel);

        // ---- Top-center: temperature + city (above the house) ----
        Period current = forecast.get(0);
        bigTempLbl = new Label(current.temperature + "°F");
        bigTempLbl.setFont(Font.font("Arial", FontWeight.LIGHT, 52));
        bigTempLbl.setTextFill(Color.WHITE);
        Label cityLbl = new Label("Chicago");
        cityLbl.setFont(Font.font("Arial", FontWeight.NORMAL, 17));
        cityLbl.setTextFill(Color.web("#b8d4ee"));

        VBox centerBox = new VBox(1, bigTempLbl, cityLbl);
        centerBox.setAlignment(Pos.TOP_CENTER);
        centerBox.setLayoutX(0);
        centerBox.setLayoutY(18);
        centerBox.setPrefWidth(W);
        pane.getChildren().add(centerBox);

        // ---- Top-right: short forecast description + H/L ----
        int hi = Integer.MIN_VALUE, lo = Integer.MAX_VALUE;
        for (Period fp : forecast) {
            if (fp.temperature > hi) hi = fp.temperature;
            if (fp.temperature < lo) lo = fp.temperature;
        }

        topDescLbl = new Label(current.shortForecast != null ? current.shortForecast : "");
        topDescLbl.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        topDescLbl.setTextFill(Color.WHITE);
        topDescLbl.setWrapText(true);
        topDescLbl.setMaxWidth(145);
        topDescLbl.setTextAlignment(TextAlignment.RIGHT);

        topHiLoLbl = new Label("H: " + hi + "°   L: " + lo + "°");
        topHiLoLbl.setFont(Font.font("Arial", 11));
        topHiLoLbl.setTextFill(Color.web("#b8d4ee"));

        VBox topRight = new VBox(4, topDescLbl, topHiLoLbl);
        topRight.setAlignment(Pos.TOP_RIGHT);
        topRight.setLayoutX(W - 160);
        topRight.setLayoutY(14);
        pane.getChildren().add(topRight);

        // ---- Right-edge buttons — rotated 90°, pinned as vertical tabs ----
        // After setRotate(-90): button's pre-rotation width becomes visual height,
        // pre-rotation height becomes visual width.
        // To pin visual right edge flush with the screen edge:
        //   visual right edge = layoutX + prefWidth/2 + prefHeight/2  (after -90 rotation)
        //   => layoutX = W - prefWidth/2 - prefHeight/2 - 2  (2px margin)
        final double BTN_W = 118; // pre-rotation width → visual height
        final double BTN_H = 28;  // pre-rotation height → visual width (tab thickness)
        final double btnLayoutX = W - BTN_W / 2.0 - BTN_H / 2.0 - 2;

        Button laterBtn = new Button("Later this week");
        styleTabButton(laterBtn, BTN_W, BTN_H);
        laterBtn.setRotate(-90);
        laterBtn.setLayoutX(btnLayoutX);
        // Visual center pinned to 1/3 of screen height
        laterBtn.setLayoutY(H / 3.0 - BTN_H / 2.0);
        laterBtn.setOnAction(e -> showScene2());

        Button changeLocBtn = new Button("Change Location");
        styleTabButton(changeLocBtn, BTN_W, BTN_H);
        changeLocBtn.setRotate(-90);
        changeLocBtn.setLayoutX(btnLayoutX);
        // Visual center pinned to 2/3 of screen height — wide separation
        changeLocBtn.setLayoutY(2.0 * H / 3.0 - BTN_H / 2.0);
        changeLocBtn.setOnAction(e -> showChangeLocationModal(rootRef));

        pane.getChildren().addAll(laterBtn, changeLocBtn);

        return pane;
    }

    /** Apply selected/unselected style to a row and update label text fills accordingly. */
    private void applyRowStyle(HBox row, Label timeLbl, Label precipLbl, Label tempLbl, boolean selected) {
        if (selected) {
            row.setStyle("-fx-background-color: #4a3a7a; -fx-background-radius: 6;");
            timeLbl.setTextFill(Color.WHITE);
            precipLbl.setTextFill(Color.web("#c8e0ff"));
            tempLbl.setTextFill(Color.WHITE);
        } else {
            row.setStyle("-fx-background-color: transparent;");
            timeLbl.setTextFill(Color.web("#ddeeff"));
            precipLbl.setTextFill(Color.web("#7ab8f5"));
            tempLbl.setTextFill(Color.WHITE);
        }
    }

    // ====================================================
    // CHANGE LOCATION MODAL
    // ====================================================
    private void showChangeLocationModal(StackPane root) {
        Rectangle backdrop = new Rectangle(W, H, Color.web("#000000", 0.52));

        Label title = new Label("Change Location");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        title.setTextFill(Color.WHITE);

        TextField cityField = new TextField();
        cityField.setPromptText("City name (e.g. Chicago)");
        styleModalField(cityField);

        Label orLbl = new Label("— or use NWS grid coordinates —");
        orLbl.setFont(Font.font("Arial", 10));
        orLbl.setTextFill(Color.web("#90aac8"));

        TextField xField = new TextField();
        xField.setPromptText("Grid X"); xField.setPrefWidth(108);
        styleModalField(xField);

        TextField yField = new TextField();
        yField.setPromptText("Grid Y"); yField.setPrefWidth(108);
        styleModalField(yField);

        HBox coordRow = new HBox(10, xField, yField);
        coordRow.setAlignment(Pos.CENTER_LEFT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.14); -fx-background-radius: 8;" +
            "-fx-text-fill: white; -fx-font-family: Arial; -fx-padding: 6 16 6 16; -fx-cursor: hand;"
        );
        Button saveBtn = new Button("Save");
        saveBtn.setStyle(
            "-fx-background-color: #6a4fa3; -fx-background-radius: 8;" +
            "-fx-text-fill: white; -fx-font-family: Arial; -fx-font-weight: bold;" +
            "-fx-padding: 6 16 6 16; -fx-cursor: hand;"
        );

        HBox btnRow = new HBox(10, cancelBtn, saveBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(12, title, cityField, orLbl, coordRow, btnRow);
        card.setPadding(new Insets(22, 24, 22, 24));
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        card.setPrefHeight(260);
        card.setMaxHeight(260);
        card.setStyle(
            "-fx-background-color: rgba(12,20,58,0.94); -fx-background-radius: 14;" +
            "-fx-border-color: rgba(255,255,255,0.22); -fx-border-radius: 14; -fx-border-width: 1;"
        );

        StackPane modal = new StackPane(backdrop, card);
        modal.setAlignment(Pos.CENTER);
        StackPane.setAlignment(card, Pos.CENTER);
        root.getChildren().add(modal);

        cancelBtn.setOnAction(ev -> root.getChildren().remove(modal));
        backdrop.setOnMouseClicked(ev -> root.getChildren().remove(modal));
        saveBtn.setOnAction(ev -> root.getChildren().remove(modal)); // placeholder for wiring
    }

    // ====================================================
    // SKY GRADIENT
    // ====================================================
    private LinearGradient buildSkyGradient(int hour, int minute, boolean isNight) {
        double t = hour + minute / 60.0;
        if (isNight) {
            return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#03030f")),
                new Stop(1, Color.web("#0d1b4b")));
        } else if (t < 6.8 || t > 17.2) {
            // Sunrise / sunset — orange horizon
            return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0,  Color.web("#1a1050")),
                new Stop(0.38, Color.web("#e8632a")),
                new Stop(0.72, Color.web("#f4a346")),
                new Stop(1.0,  Color.web("#ffd57a")));
        } else {
            // Full daytime blue sky
            return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0d47a1")),
                new Stop(1, Color.web("#64B5F6")));
        }
    }

    // ====================================================
    // HELPERS
    // ====================================================
    private double getMoonOpacity(int hour) {
        if (hour >= 0 && hour < 6) return 0.65 + (6 - hour) / 6.0 * 0.35;
        return 0.65 + (hour - 18) / 6.0 * 0.35;
    }

    private void styleTabButton(Button btn, double prefW, double prefH) {
        String base =
            "-fx-background-color: #6a4fa3; -fx-background-radius: 6;" +
            "-fx-text-fill: white; -fx-font-family: Arial; -fx-font-size: 11px;" +
            "-fx-font-weight: bold; -fx-cursor: hand;";
        String hover =
            "-fx-background-color: #8060c0; -fx-background-radius: 6;" +
            "-fx-text-fill: white; -fx-font-family: Arial; -fx-font-size: 11px;" +
            "-fx-font-weight: bold; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setPrefWidth(prefW);
        btn.setPrefHeight(prefH);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }

    private void styleModalField(TextField f) {
        f.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white;" +
            "-fx-prompt-text-fill: rgba(255,255,255,0.45); -fx-background-radius: 6;" +
            "-fx-border-color: rgba(255,255,255,0.28); -fx-border-radius: 6;"
        );
    }

    /** Shortcut for Polygon(x1,y1, x2,y2, x3,y3) */
    private Polygon poly(double x1, double y1, double x2, double y2, double x3, double y3) {
        return new Polygon(x1, y1, x2, y2, x3, y3);
    }

    /** Shortcut to add a filled Rectangle to a pane */
    private void rect(Pane p, double x, double y, double w, double h,
                      Color fill, Color stroke, double sw) {
        Rectangle r = new Rectangle(x, y, w, h);
        r.setFill(fill);
        if (sw > 0) { r.setStroke(stroke); r.setStrokeWidth(sw); }
        p.getChildren().add(r);
    }

    /** Shortcut to add a Line to a pane */
    private void line(Pane p, double x1, double y1, double x2, double y2,
                      Color stroke, double sw) {
        Line l = new Line(x1, y1, x2, y2);
        l.setStroke(stroke); l.setStrokeWidth(sw);
        p.getChildren().add(l);
    }

    // ====================================================
    // WEATHER STATE & EFFECTS
    // ====================================================

    private WeatherState getWeatherState(Period p) {
        String f = (p.shortForecast != null) ? p.shortForecast : "";
        int precip = (p.probabilityOfPrecipitation != null) ? p.probabilityOfPrecipitation.value : 0;
        if (f.contains("Snow") || f.contains("Flurr") || f.contains("Blizzard")) return WeatherState.SNOW;
        if (f.contains("Rain") || f.contains("Shower") || f.contains("Thunder") || f.contains("Drizzle")) return WeatherState.RAIN;
        if (precip >= 50) return (p.temperature <= 32) ? WeatherState.SNOW : WeatherState.RAIN;
        if (f.contains("Cloud") || f.contains("Overcast")) return WeatherState.CLOUDY;
        return WeatherState.CLEAR;
    }

    /** Cancel any in-flight fade/animation and crossfade to a new weather effect. */
    private void switchWeatherEffect(WeatherState state) {
        if (currentFadeAnim != null) { currentFadeAnim.stop(); currentFadeAnim = null; }
        if (weatherTimeline != null) { weatherTimeline.stop(); weatherTimeline = null; }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), effectsPane);
        fadeOut.setToValue(0);
        currentFadeAnim = fadeOut;
        fadeOut.setOnFinished(ev -> {
            effectsPane.getChildren().clear();
            populateWeatherEffect(state);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(600), effectsPane);
            fadeIn.setToValue(1);
            currentFadeAnim = fadeIn;
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void populateWeatherEffect(WeatherState state) {
        switch (state) {
            case RAIN:   buildRainEffect(effectsPane);  break;
            case SNOW:   buildSnowEffect(effectsPane);  break;
            case CLOUDY: buildCloudEffect(effectsPane); break;
            default:     break; // CLEAR — leave pane empty
        }
    }

    private void buildRainEffect(Pane pane) {
        final int N = 90;
        Line[] drops = new Line[N];
        double[] dropY = new double[N];
        double[] dropX = new double[N];
        double[] speed = new double[N];
        double[] len   = new double[N];
        Random rng = new Random();
        for (int i = 0; i < N; i++) {
            dropX[i] = rng.nextDouble() * W;
            dropY[i] = rng.nextDouble() * H;
            speed[i] = 5 + rng.nextDouble() * 5;
            len[i]   = 9 + rng.nextDouble() * 9;
            drops[i] = new Line(dropX[i], dropY[i], dropX[i] + 1.5, dropY[i] + len[i]);
            drops[i].setStroke(Color.web("#90c8f0", 0.42));
            drops[i].setStrokeWidth(1);
            pane.getChildren().add(drops[i]);
        }
        weatherTimeline = new Timeline(new KeyFrame(Duration.millis(16), e -> {
            for (int i = 0; i < N; i++) {
                dropY[i] += speed[i];
                if (dropY[i] > H + 20) { dropY[i] = -20; dropX[i] = new Random().nextDouble() * W; }
                drops[i].setStartX(dropX[i]); drops[i].setStartY(dropY[i]);
                drops[i].setEndX(dropX[i] + 1.5); drops[i].setEndY(dropY[i] + len[i]);
            }
        }));
        weatherTimeline.setCycleCount(Animation.INDEFINITE);
        weatherTimeline.play();
    }

    private void buildSnowEffect(Pane pane) {
        final int N = 65;
        Circle[] flakes = new Circle[N];
        double[] fx = new double[N], fy = new double[N];
        double[] speed = new double[N], drift = new double[N], phase = new double[N];
        Random rng = new Random();
        for (int i = 0; i < N; i++) {
            fx[i] = rng.nextDouble() * W;
            fy[i] = rng.nextDouble() * H;
            speed[i] = 0.7 + rng.nextDouble() * 1.1;
            drift[i] = 0.3 + rng.nextDouble() * 0.5;
            phase[i] = rng.nextDouble() * Math.PI * 2;
            double r = 1.8 + rng.nextDouble() * 2.5;
            flakes[i] = new Circle(fx[i], fy[i], r, Color.web("#e8f4ff", 0.85));
            pane.getChildren().add(flakes[i]);
        }
        final long[] frame = {0};
        weatherTimeline = new Timeline(new KeyFrame(Duration.millis(33), e -> {
            frame[0]++;
            for (int i = 0; i < N; i++) {
                fy[i] += speed[i];
                fx[i] += Math.sin(phase[i] + frame[0] * 0.04) * drift[i];
                if (fy[i] > H + 10) { fy[i] = -10; fx[i] = new Random().nextDouble() * W; }
                flakes[i].setCenterX(fx[i]);
                flakes[i].setCenterY(fy[i]);
            }
        }));
        weatherTimeline.setCycleCount(Animation.INDEFINITE);
        weatherTimeline.play();
    }

    private void buildCloudEffect(Pane pane) {
        Rectangle overlay = new Rectangle(0, 0, W, H);
        overlay.setFill(Color.web("#404860", 0.28));
        pane.getChildren().add(overlay);
        double[][] clouds = {
            {130, 75,  95, 32}, {310, 52, 115, 36}, {510, 68, 105, 33},
            {695, 58,  88, 28}, {840, 88,  78, 26}
        };
        for (double[] c : clouds) {
            Ellipse e = new Ellipse(c[0], c[1], c[2], c[3]);
            e.setFill(Color.web("#c8d0e0", 0.20));
            pane.getChildren().add(e);
        }
    }
}
