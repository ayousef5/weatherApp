import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.*;
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

public class JavaFX extends Application {

    private Stage primaryStage;
    Scene todayScene;
    Scene forecastScene;

    static final double W = 900, H = 500;
    static final double GRASS_Y = 495;

    // top display labels updated on row selection
    private Label bigTempLbl;
    private Label topDescLbl;
    private Label topHiLoLbl;

    // weather animation pane and timelines
    private Pane effectsPane;
    private WeatherEffect activeEffect;
    private Animation currentFadeAnim;
    private Timeline clockTimeline;

    // scene 1 background and house refs for click-driven updates
    private Pane bgPane;
    private Pane housePn;
    private Rectangle leftWin;
    private Rectangle rightWin;
    private Polygon[] backMtns = new Polygon[2];
    private Polygon[] frontMtns = new Polygon[6];

    // os-appropriate font for rendering color emoji in labels
    private final String emojiFont = System.getProperty("os.name").toLowerCase().contains("mac")
        ? "Apple Color Emoji" : "Segoe UI Emoji";

    private enum WeatherState { CLEAR, CLOUDY, RAIN, SNOW }

    public static void main(String[] args) { launch(args); }

    // navigate to forecast scene
    public void showScene2() {
        primaryStage.setScene(forecastScene);
        primaryStage.setFullScreen(true);
    }

    private Label styledLabel(String text, double size, boolean bold, Color color) {
        Label l = new Label(text);
        l.setFont(bold ? Font.font(".SF NS Text", FontWeight.BOLD, size) : Font.font(".SF NS Text", size));
        l.setTextFill(color);
        l.setTextAlignment(TextAlignment.CENTER);
        l.setWrapText(true);
        l.setMaxWidth(200);
        return l;
    }

    private Label makeWrappingLabel(String text, double maxW) {
        Label l = new Label(text);
        l.setFont(Font.font(".SF NS Text", 11));
        l.setTextFill(Color.web("#c8d8f0"));
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

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        stage.setTitle("Weather App");

        LocalDateTime now = LocalDateTime.now();
        int hour   = now.getHour();
        int minute = now.getMinute();
        boolean isNight = (hour >= 19 || hour < 6);

        ArrayList<Period> forecast = WeatherAPI.getForecast("LOT", 77, 70);
        if (forecast == null || forecast.size() < 8)
            throw new RuntimeException("Forecast did not load properly");

        // screen dimensions for scaling
        Rectangle2D screen = Screen.getPrimary().getBounds();
        double SW = screen.getWidth(), SH = screen.getHeight();

        StackPane scene1Root = buildScene1(forecast, hour, minute, isNight);
        // scale scene 1 to fill screen
        scene1Root.getTransforms().add(
            new javafx.scene.transform.Scale(SW / W, SH / H, 0, 0));
        // group so scene uses real screen dimensions
        javafx.scene.Group fsRoot = new javafx.scene.Group(scene1Root);
        todayScene = new Scene(fsRoot, SW, SH);

        // scene 2 root
        Pane bg2 = buildBackground(hour, minute, isNight);

        Button backButton = new Button("← Back to today!");
        backButton.setStyle(
            "-fx-background-color: #7B5EA7; -fx-background-radius: 24; -fx-text-fill: white;" +
            "-fx-font-family: '.SF NS Text'; -fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-padding: 8 18 8 18; -fx-cursor: hand;"
        );

        Label cityLabel2 = styledLabel("Chicago", 28, true, Color.WHITE);
        BorderPane topBar = new BorderPane();
        topBar.setLeft(backButton);
        topBar.setCenter(cityLabel2);
        BorderPane.setAlignment(backButton, Pos.CENTER_LEFT);
        BorderPane.setAlignment(cityLabel2, Pos.CENTER);
        topBar.setPadding(new Insets(14, 20, 6, 16));

        String[] tips = fetchTips(4);
        int[] dayIdx   = {0, 2, 4, 6};
        int[] nightIdx = {1, 3, 5, 7};

        Color cardHeaderColor = isNight ? Color.WHITE : Color.BLACK;
        Color cardBodyColor   = isNight ? Color.web("#c8d8f0") : Color.BLACK;
        Color cardItalicColor = isNight ? Color.web("#a0b8d8") : Color.BLACK;

        HBox cardsRow = new HBox(10);
        cardsRow.setAlignment(Pos.TOP_CENTER);
        cardsRow.setPadding(new Insets(4, 10, 10, 10));

        for (int c = 0; c < 4; c++) {
            Period dayP   = forecast.get(dayIdx[c]);
            Period nightP = forecast.get(nightIdx[c]);
            final double CARD_W = 210;

            String dayName   = (c == 0) ? "Today" : dayP.name;
            Label cardHeader = styledLabel(dayName, 16, true, cardHeaderColor);
            Label cardTemp   = styledLabel(dayP.temperature + "°", 16, true, cardHeaderColor);
            HBox cardTitle   = new HBox(6, cardHeader, cardTemp);
            cardTitle.setAlignment(Pos.CENTER);

            Region headerSep = new Region();
            headerSep.setPrefHeight(2); headerSep.setPrefWidth(CARD_W);
            headerSep.setStyle("-fx-background-color: rgba(100,100,180,0.4);");

            Label dayIconLbl = new Label("\u2600 DAY:");
            dayIconLbl.setFont(Font.font(".SF NS Text", FontWeight.BOLD, 12));
            dayIconLbl.setTextFill(cardHeaderColor);
            Label dayTempLbl = makeWrappingLabel("Temp: " + dayP.temperature + "°F", CARD_W);
            dayTempLbl.setTextFill(cardBodyColor);
            Label dayWindLbl = makeWrappingLabel("Wind: " + dayP.windDirection + " " + dayP.windSpeed, CARD_W);
            dayWindLbl.setTextFill(cardBodyColor);
            Label dayPrecLbl = makeWrappingLabel("Precipitation: " + dayP.probabilityOfPrecipitation.value + "%", CARD_W);
            dayPrecLbl.setTextFill(cardBodyColor);
            Label dayDescLbl = makeWrappingLabel(dayP.shortForecast, CARD_W);
            dayDescLbl.setTextFill(cardItalicColor);
            dayDescLbl.setFont(Font.font(".SF NS Text", FontPosture.ITALIC, 11));
            VBox daySection = new VBox(3, dayIconLbl, dayTempLbl, dayWindLbl, dayPrecLbl, dayDescLbl);
            daySection.setAlignment(Pos.CENTER_LEFT);

            Region midSep = new Region();
            midSep.setPrefHeight(1); midSep.setPrefWidth(CARD_W);
            midSep.setStyle("-fx-background-color: rgba(100,100,180,0.25);");

            Label nightIconLbl = new Label("\u263D NIGHT:");
            nightIconLbl.setFont(Font.font(".SF NS Text", FontWeight.BOLD, 12));
            nightIconLbl.setTextFill(cardHeaderColor);
            Label nightTempLbl = makeWrappingLabel("Temp: " + nightP.temperature + "°F", CARD_W);
            nightTempLbl.setTextFill(cardBodyColor);
            Label nightWindLbl = makeWrappingLabel("Wind: " + nightP.windDirection + " " + nightP.windSpeed, CARD_W);
            nightWindLbl.setTextFill(cardBodyColor);
            Label nightPrecLbl = makeWrappingLabel("Precipitation: " + nightP.probabilityOfPrecipitation.value + "%", CARD_W);
            nightPrecLbl.setTextFill(cardBodyColor);
            Label nightDescLbl = makeWrappingLabel(nightP.shortForecast, CARD_W);
            nightDescLbl.setTextFill(cardItalicColor);
            nightDescLbl.setFont(Font.font(".SF NS Text", FontPosture.ITALIC, 11));
            VBox nightSection = new VBox(3, nightIconLbl, nightTempLbl, nightWindLbl, nightPrecLbl, nightDescLbl);
            nightSection.setAlignment(Pos.CENTER_LEFT);

            Region botSep = new Region();
            botSep.setPrefHeight(1); botSep.setPrefWidth(CARD_W);
            botSep.setStyle("-fx-background-color: rgba(100,100,180,0.25);");

            Label tipIconLbl = new Label("\u2605 Tip of the day");
            tipIconLbl.setFont(Font.font(".SF NS Text", FontWeight.BOLD, 12));
            tipIconLbl.setTextFill(cardHeaderColor);
            Label tipText = makeWrappingLabel("\"" + tips[c] + "\"", CARD_W);
            tipText.setTextFill(cardItalicColor);
            tipText.setFont(Font.font(".SF NS Text", FontPosture.ITALIC, 11));
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
                "-fx-background-color: rgba(200,210,235,0.85); -fx-background-radius: 18;" +
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
        // scale scene 2 to fill screen
        root2.getTransforms().add(
            new javafx.scene.transform.Scale(SW / W, SH / H, 0, 0));
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

    // scene 1 root
    private StackPane buildScene1(ArrayList<Period> forecast, int hour, int minute, boolean isNight) {
        StackPane root = new StackPane();

        bgPane  = buildBackground(hour, minute, isNight);
        housePn = buildHouse(isNight);

        effectsPane = new Pane();
        effectsPane.setPrefSize(W, H);
        effectsPane.setPickOnBounds(false);
        effectsPane.setMouseTransparent(true); // clicks pass through to UI

        Pane uiPane  = buildUI(forecast, root);

        root.getChildren().addAll(bgPane, housePn, effectsPane, uiPane);

        // start weather animation
        switchWeatherEffect(getWeatherState(forecast.get(0)));
        return root;
    }

    // sky, stars, sun/moon, and mountains
    private Pane buildBackground(int hour, int minute, boolean isNight) {
        Pane pane = new Pane();
        pane.setPrefSize(W, H);

        Rectangle sky = new Rectangle(0, 0, W, H);
        sky.setFill(buildSkyGradient(hour, minute, isNight));
        pane.getChildren().add(sky);

        // stars
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

        // mountains
        Color backMtnColor  = isNight ? Color.web("#1e3a52") : Color.web("#4a6a8a");
        Color frontMtnColor = isNight ? Color.web("#0f2030") : Color.web("#3a5a6a");

        // back-left mountain
        Polygon mtL1 = new Polygon(
            0,   GRASS_Y,
            65,  GRASS_Y - 45,
            130, GRASS_Y - 118,
            202, GRASS_Y - 52,
            275, GRASS_Y);
        mtL1.setFill(backMtnColor);

        // front-left mountain
        Polygon mtL2 = new Polygon(
            55,  GRASS_Y,
            130, GRASS_Y - 68,
            205, GRASS_Y - 155,
            290, GRASS_Y - 75,
            375, GRASS_Y);
        mtL2.setFill(frontMtnColor);

        // front-right mountain
        Polygon mtR1 = new Polygon(
            525, GRASS_Y,
            600, GRASS_Y - 72,
            675, GRASS_Y - 152,
            757, GRASS_Y - 80,
            840, GRASS_Y);
        mtR1.setFill(frontMtnColor);

        // back-right mountain
        Polygon mtR2 = new Polygon(
            705, GRASS_Y,
            757, GRASS_Y - 55,
            810, GRASS_Y - 112,
            855, GRASS_Y - 40,
            900, GRASS_Y);
        mtR2.setFill(backMtnColor);

        backMtns[0] = mtL1; backMtns[1] = mtR2;

        pane.getChildren().addAll(mtL1, mtL2, mtR1, mtR2);

        // foreground mountains
        Polygon fgL1 = new Polygon(
            0,   GRASS_Y,
            80,  GRASS_Y - 38,
            160, GRASS_Y - 70,
            235, GRASS_Y - 50,
            310, GRASS_Y);
        fgL1.setFill(frontMtnColor);

        Polygon fgL2 = new Polygon(
            80,  GRASS_Y,
            155, GRASS_Y - 50,
            240, GRASS_Y - 90,
            330, GRASS_Y - 55,
            420, GRASS_Y);
        fgL2.setFill(frontMtnColor);

        Polygon fgR1 = new Polygon(
            480, GRASS_Y,
            565, GRASS_Y - 52,
            650, GRASS_Y - 88,
            715, GRASS_Y - 60,
            780, GRASS_Y);
        fgR1.setFill(frontMtnColor);

        Polygon fgR2 = new Polygon(
            650, GRASS_Y,
            720, GRASS_Y - 45,
            795, GRASS_Y - 72,
            850, GRASS_Y - 42,
            900, GRASS_Y);
        fgR2.setFill(frontMtnColor);

        frontMtns[0] = mtL2; frontMtns[1] = mtR1;
        frontMtns[2] = fgL1; frontMtns[3] = fgL2; frontMtns[4] = fgR1; frontMtns[5] = fgR2;

        pane.getChildren().addAll(fgL1, fgL2, fgR1, fgR2);


        return pane;
    }

    // house drawn with basic shapes
    private Pane buildHouse(boolean isNight) {
        Pane pane = new Pane();
        pane.setPrefSize(W, H);
        pane.setPickOnBounds(false);

        Color wall    = Color.web("#E8DCC8");
        Color roofCol = isNight ? Color.web("#3a2a1a") : Color.web("#8B4513");
        Color winCol  = isNight ? Color.web("#ffe066") : Color.web("#a8d8f0");
        Color trim    = Color.web("#D4C4A8"); // window ledges
        Color stroke  = Color.web("#C0B0A0");

        // main house body
        rect(pane, 335, 388, 185, 107, wall, stroke, 1);

        // roof triangle
        Polygon roof = poly(320, 388,  540, 388,  427, 313);
        roof.setFill(roofCol);
        roof.setStroke(Color.web("#5a3010")); roof.setStrokeWidth(1.5);
        pane.getChildren().add(roof);

        // front door
        rect(pane, 406, 437, 42, 58, Color.web("#6B3A2A"), Color.web("#4a2a1a"), 1);
        // door knob
        pane.getChildren().add(new Circle(443, 466, 3, Color.web("#D4AF37")));

        // left window
        Rectangle lw = new Rectangle(344, 405, 54, 42);
        lw.setFill(winCol); lw.setStroke(stroke); lw.setStrokeWidth(1.5);
        pane.getChildren().add(lw);
        leftWin = lw;
        line(pane, 371, 405, 371, 447, stroke, 1.5); // vertical divider
        line(pane, 344, 426, 398, 426, stroke, 1.5); // horizontal divider
        rect(pane, 340, 446, 62, 5, trim, stroke, 0); // ledge

        // right window
        Rectangle rw = new Rectangle(456, 405, 54, 42);
        rw.setFill(winCol); rw.setStroke(stroke); rw.setStrokeWidth(1.5);
        pane.getChildren().add(rw);
        rightWin = rw;
        line(pane, 483, 405, 483, 447, stroke, 1.5); // vertical divider
        line(pane, 456, 426, 510, 426, stroke, 1.5); // horizontal divider
        rect(pane, 452, 446, 62, 5, trim, stroke, 0); // ledge

        return pane;
    }

    // time label for row i
    private static String hourLabel(int i) {
        if (i == 0) return "Now";
        int h = (LocalDateTime.now().getHour() + i) % 24;
        if (h == 0)       return "12 AM";
        else if (h < 12)  return h + " AM";
        else if (h == 12) return "12 PM";
        else              return (h - 12) + " PM";
    }

    private static String weatherEmoji(String forecast) {
        if (forecast == null) return "?";
        String f = forecast.toLowerCase();
        if (f.contains("thunderstorm") || f.contains("thunder"))           return "\u26A1"; // ⚡
        if (f.contains("blizzard") || f.contains("snow") || f.contains("snowy")) return "\u2744"; // ❄
        if (f.contains("sleet") || f.contains("freezing rain"))            return "\u2744"; // ❄
        if (f.contains("rain") || f.contains("rainy") || f.contains("shower")) return "\u2602"; // ☂
        if (f.contains("drizzle"))                                         return "\u2602"; // ☂
        if (f.contains("fog") || f.contains("mist"))                       return "\u2248"; // ≈
        if (f.contains("wind") || f.contains("breezy") || f.contains("blustery")) return "\u007E\u007E"; // ~~
        if (f.contains("mostly cloudy") || f.contains("overcast"))         return "\u2601"; // ☁
        if (f.contains("partly cloudy") || f.contains("partly sunny"))      return "\u2601"; // ☁
        if (f.contains("mostly clear") || f.contains("mostly sunny"))      return "\u2600"; // ☀
        if (f.contains("sunny") || f.contains("clear"))                    return "\u2600"; // ☀
        if (f.contains("cloud"))                                            return "\u2601"; // ☁
        return "\u25CB"; // ○
    }

    private Pane buildUI(ArrayList<Period> forecast, StackPane rootRef) {
        Pane pane = new Pane();
        pane.setPrefSize(W, H);
        pane.setPickOnBounds(false);

        // hourly forecast panel
        final double PANEL_W = 215;
        final double ROW_W   = PANEL_W - 12; // inner row width

        Label headerLbl = new Label("HOURLY FORECAST");
        headerLbl.setFont(Font.font(".SF NS Text", FontWeight.BOLD, 9));
        headerLbl.setTextFill(Color.web("#aac4e0"));
        headerLbl.setMaxWidth(ROW_W);
        headerLbl.setPadding(new Insets(0, 0, 4, 0));

        // header separator
        Region headerSep = new Region();
        headerSep.setPrefHeight(1);
        headerSep.setPrefWidth(ROW_W);
        headerSep.setStyle("-fx-background-color: rgba(255,255,255,0.15);");

        int numRows = Math.min(10, forecast.size());
        final int[] selectedIdx = {0};
        List<HBox> rowList = new ArrayList<>();
        VBox rowsBox = new VBox(0);

        // time label refs for clock refresh
        Label[] timeLabels = new Label[numRows];

        for (int i = 0; i < numRows; i++) {
            Period p      = forecast.get(i);
            final int idx = i;
            // adapter wraps the period so direct field access is avoided below
            PeriodAdapter adapter = new PeriodAdapter(p);

            Label timeLbl = new Label(hourLabel(i));
            timeLbl.setFont(Font.font(".SF NS Text", 10));
            timeLbl.setTextFill(Color.web("#ddeeff"));
            timeLbl.setPrefWidth(72);
            timeLbl.setMinWidth(72);
            timeLabels[i] = timeLbl;

            // weather icon — use adapter description so null is never passed
            Label iconLbl = new Label(weatherEmoji(adapter.getDescription()));
            iconLbl.setFont(Font.font(".SF NS Text", 18));
            iconLbl.setTextFill(Color.WHITE);
            StackPane iconCell = new StackPane(iconLbl);
            iconCell.setPrefWidth(30);
            iconCell.setMinWidth(30);
            iconCell.setAlignment(Pos.CENTER);

            // precipitation label — adapter handles the null check
            int precip = adapter.getPrecipitation();
            Label precipLbl = new Label(precip + "%");
            precipLbl.setFont(Font.font(".SF NS Text", 9));
            precipLbl.setTextFill(Color.web("#7ab8f5"));
            precipLbl.setOpacity(0.85);
            precipLbl.setPrefWidth(28);
            precipLbl.setMinWidth(28);
            precipLbl.setAlignment(Pos.CENTER_RIGHT);

            // temp label — adapter formats the value with unit
            Label tempLbl = new Label(adapter.getDisplayTemperature());
            tempLbl.setFont(Font.font(".SF NS Text", FontWeight.BOLD, 10));
            tempLbl.setTextFill(Color.WHITE);
            tempLbl.setPrefWidth(36);
            tempLbl.setAlignment(Pos.CENTER_RIGHT);

            HBox row = new HBox(0, timeLbl, iconCell, precipLbl, tempLbl);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(5, 6, 5, 6));
            row.setPrefWidth(ROW_W);

            // first row selected by default
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
                // adapter methods replace direct period field access
                bigTempLbl.setText(adapter.getDisplayTemperature());
                topDescLbl.setText(adapter.getDescription());
                int hi2 = Integer.MIN_VALUE, lo2 = Integer.MAX_VALUE;
                for (Period fp : forecast) {
                    if (fp.temperature > hi2) hi2 = fp.temperature;
                    if (fp.temperature < lo2) lo2 = fp.temperature;
                }
                topHiLoLbl.setText("H: " + hi2 + "°   L: " + lo2 + "°");
                switchWeatherEffect(getWeatherState(p));
                // isNightHour derives day/night from index + current clock
                boolean rowIsNight = PeriodAdapter.isNightHour(idx);
                int rowHour = (LocalDateTime.now().getHour() + idx) % 24;
                Rectangle skyR = (Rectangle) bgPane.getChildren().get(0);
                skyR.setFill(buildSkyGradient(rowHour, 0, rowIsNight));
                Color winColor = rowIsNight ? Color.web("#ffe066") : Color.web("#a8d8f0");
                leftWin.setFill(winColor);
                rightWin.setFill(winColor);
                Color bkMtn = rowIsNight ? Color.web("#1e3a52") : Color.web("#4a6a8a");
                Color ftMtn = rowIsNight ? Color.web("#0f2030") : Color.web("#3a5a6a");
                for (Polygon m : backMtns) m.setFill(bkMtn);
                for (Polygon m : frontMtns) m.setFill(ftMtn);
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

        // refresh time labels every minute
        if (clockTimeline != null) clockTimeline.stop();
        clockTimeline = new Timeline(new KeyFrame(Duration.minutes(1), e -> {
            for (int i = 0; i < timeLabels.length; i++)
                timeLabels[i].setText(hourLabel(i));
        }));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();

        VBox panelContent = new VBox(0, headerLbl, headerSep, rowsBox);
        panelContent.setPadding(new Insets(10, 6, 10, 6));
        panelContent.setMaxWidth(PANEL_W);

        // frosted panel background
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
        leftPanel.setLayoutY(45);
        pane.getChildren().add(leftPanel);

        // center temperature display
        Period current = forecast.get(0);
        bigTempLbl = new Label(current.temperature + "°F");
        bigTempLbl.setFont(Font.font(".SF NS Text", FontWeight.LIGHT, 52));
        bigTempLbl.setTextFill(Color.WHITE);
        Label cityLbl = new Label("Chicago");
        cityLbl.setFont(Font.font(".SF NS Text", FontWeight.NORMAL, 17));
        cityLbl.setTextFill(Color.web("#b8d4ee"));

        VBox centerBox = new VBox(1, bigTempLbl, cityLbl);
        centerBox.setAlignment(Pos.TOP_CENTER);
        centerBox.setLayoutX(0);
        centerBox.setLayoutY(18);
        centerBox.setPrefWidth(W);
        pane.getChildren().add(centerBox);

        // forecast info top-right
        int hi = Integer.MIN_VALUE, lo = Integer.MAX_VALUE;
        for (Period fp : forecast) {
            if (fp.temperature > hi) hi = fp.temperature;
            if (fp.temperature < lo) lo = fp.temperature;
        }

        topDescLbl = new Label(current.shortForecast != null ? current.shortForecast : "");
        topDescLbl.setFont(Font.font(".SF NS Text", FontWeight.BOLD, 13));
        topDescLbl.setTextFill(Color.WHITE);
        topDescLbl.setWrapText(true);
        topDescLbl.setMaxWidth(145);
        topDescLbl.setTextAlignment(TextAlignment.RIGHT);

        topHiLoLbl = new Label("H: " + hi + "°   L: " + lo + "°");
        topHiLoLbl.setFont(Font.font(".SF NS Text", 11));
        topHiLoLbl.setTextFill(Color.web("#b8d4ee"));

        VBox topRight = new VBox(4, topDescLbl, topHiLoLbl);
        topRight.setAlignment(Pos.TOP_RIGHT);
        topRight.setLayoutX(W - 160);
        topRight.setLayoutY(14);
        pane.getChildren().add(topRight);

        // sideways tab buttons
        final double BTN_W = 118;
        final double BTN_H = 28;
        final double btnLayoutX = W - BTN_W / 2.0 - BTN_H / 2.0 - 2;

        Button laterBtn = new Button("Later this week");
        styleTabButton(laterBtn, BTN_W, BTN_H);
        laterBtn.setRotate(-90);
        laterBtn.setLayoutX(btnLayoutX);
        laterBtn.setLayoutY(H / 3.0 - BTN_H / 2.0);
        laterBtn.setOnAction(e -> showScene2());

        Button changeLocBtn = new Button("Change Location");
        styleTabButton(changeLocBtn, BTN_W, BTN_H);
        changeLocBtn.setRotate(-90);
        changeLocBtn.setLayoutX(btnLayoutX);
        changeLocBtn.setLayoutY(2.0 * H / 3.0 - BTN_H / 2.0);
        changeLocBtn.setOnAction(e -> showChangeLocationModal(rootRef));

        pane.getChildren().addAll(laterBtn, changeLocBtn);

        return pane;
    }

    // selected row highlight
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

    // change location modal — geocodes city via Census API then loads NWS forecast
    private void showChangeLocationModal(StackPane root) {
        Rectangle backdrop = new Rectangle(W, H, Color.web("#000000", 0.52));

        Label title = new Label("Change Location");
        title.setFont(Font.font(".SF NS Text", FontWeight.BOLD, 16));
        title.setTextFill(Color.WHITE);

        TextField cityField = new TextField();
        cityField.setPromptText("City, State (e.g. Chicago, IL)");
        styleModalField(cityField);

        Label statusLbl = new Label();
        statusLbl.setFont(Font.font(".SF NS Text", 11));
        statusLbl.setTextFill(Color.web("#90c8ff"));
        statusLbl.setVisible(false);

        Label errorLbl = new Label();
        errorLbl.setTextFill(Color.web("#ff6b6b"));
        errorLbl.setFont(Font.font(".SF NS Text", 11));
        errorLbl.setVisible(false);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.14); -fx-background-radius: 8;" +
            "-fx-text-fill: white; -fx-font-family: '.SF NS Text'; -fx-padding: 6 16 6 16; -fx-cursor: hand;"
        );
        Button saveBtn = new Button("Save");
        saveBtn.setStyle(
            "-fx-background-color: #6a4fa3; -fx-background-radius: 8;" +
            "-fx-text-fill: white; -fx-font-family: '.SF NS Text'; -fx-font-weight: bold;" +
            "-fx-padding: 6 16 6 16; -fx-cursor: hand;"
        );

        HBox btnRow = new HBox(10, cancelBtn, saveBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(12, title, cityField, statusLbl, errorLbl, btnRow);
        card.setPadding(new Insets(22, 24, 22, 24));
        card.setPrefWidth(300);
        card.setMaxWidth(300);
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
        saveBtn.setOnAction(ev -> {
            String city = cityField.getText().trim();
            if (city.isEmpty()) {
                errorLbl.setText("Enter a city name");
                errorLbl.setVisible(true);
                return;
            }
            // show loading state
            errorLbl.setVisible(false);
            statusLbl.setText("Looking up location...");
            statusLbl.setVisible(true);
            saveBtn.setDisable(true);
            cancelBtn.setDisable(true);

            Thread t = new Thread(() -> {
                try {
                    // step 1: census geocode city → lat/lon
                    String encoded = java.net.URLEncoder.encode(city, "UTF-8");
                    String geocodeUrl = "https://geocoding.geo.census.gov/geocoder/locations/onelineaddress"
                        + "?address=" + encoded + "&benchmark=2020&format=json";
                    String geoJson = httpGet(geocodeUrl);
                    double lat = parseJsonDouble(geoJson, "\"y\":");
                    double lon = parseJsonDouble(geoJson, "\"x\":");
                    if (Double.isNaN(lat) || Double.isNaN(lon))
                        throw new Exception("Location not found");

                    // step 2: NWS points API → gridId, gridX, gridY
                    javafx.application.Platform.runLater(() -> statusLbl.setText("Finding NWS grid..."));
                    String pointsUrl = String.format("https://api.weather.gov/points/%.4f,%.4f", lat, lon);
                    String pointsJson = httpGet(pointsUrl);
                    String gridId = parseJsonString(pointsJson, "\"gridId\":");
                    int gridX = parseJsonInt(pointsJson, "\"gridX\":");
                    int gridY = parseJsonInt(pointsJson, "\"gridY\":");
                    if (gridId == null || gridId.isEmpty() || gridX < 0 || gridY < 0)
                        throw new Exception("NWS grid not found");

                    // step 3: load forecast
                    javafx.application.Platform.runLater(() -> statusLbl.setText("Loading forecast..."));
                    ArrayList<Period> newForecast = WeatherAPI.getForecast(gridId, gridX, gridY);
                    if (newForecast == null || newForecast.size() < 8)
                        throw new Exception("Forecast unavailable for this location");

                    final ArrayList<Period> fc = newForecast;
                    javafx.application.Platform.runLater(() -> {
                        LocalDateTime now2 = LocalDateTime.now();
                        int h = now2.getHour(), m = now2.getMinute();
                        boolean night = (h >= 19 || h < 6);
                        StackPane newScene1Root = buildScene1(fc, h, m, night);
                        Rectangle2D screen2 = Screen.getPrimary().getBounds();
                        double SW2 = screen2.getWidth(), SH2 = screen2.getHeight();
                        newScene1Root.getTransforms().add(
                            new javafx.scene.transform.Scale(SW2 / W, SH2 / H, 0, 0));
                        javafx.scene.Group newFsRoot = new javafx.scene.Group(newScene1Root);
                        todayScene = new Scene(newFsRoot, SW2, SH2);
                        root.getChildren().remove(modal);
                        primaryStage.setScene(todayScene);
                        primaryStage.setFullScreen(true);
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        statusLbl.setVisible(false);
                        errorLbl.setText(ex.getMessage() != null ? ex.getMessage() : "Failed to load forecast");
                        errorLbl.setVisible(true);
                        saveBtn.setDisable(false);
                        cancelBtn.setDisable(false);
                    });
                }
            });
            t.setDaemon(true);
            t.start();
        });
    }

    // simple HTTP GET — returns body as string
    private String httpGet(String url) throws Exception {
        java.net.HttpURLConnection conn =
            (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "WeatherApp/1.0");
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(6000);
        int code = conn.getResponseCode();
        if (code != 200) throw new Exception("HTTP " + code + " from " + url);
        java.util.Scanner sc = new java.util.Scanner(conn.getInputStream(), "UTF-8");
        String body = sc.useDelimiter("\\A").next();
        sc.close();
        conn.disconnect();
        return body;
    }

    // parse first string value after key (handles quoted strings)
    private String parseJsonString(String json, String key) {
        int ki = json.indexOf(key);
        if (ki < 0) return null;
        int colon = json.indexOf(":", ki + key.length() - 1);
        int q1 = json.indexOf("\"", colon + 1);
        if (q1 < 0) return null;
        int q2 = json.indexOf("\"", q1 + 1);
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }

    // parse first integer value after key
    private int parseJsonInt(String json, String key) {
        int ki = json.indexOf(key);
        if (ki < 0) return -1;
        int colon = json.indexOf(":", ki + key.length() - 1);
        int start = colon + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\n')) start++;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        if (end == start) return -1;
        return Integer.parseInt(json.substring(start, end));
    }

    // parse first double value after key
    private double parseJsonDouble(String json, String key) {
        int ki = json.indexOf(key);
        if (ki < 0) return Double.NaN;
        int colon = json.indexOf(":", ki + key.length() - 1);
        int start = colon + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\n')) start++;
        int end = start;
        if (end < json.length() && json.charAt(end) == '-') end++;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) end++;
        if (end == start) return Double.NaN;
        return Double.parseDouble(json.substring(start, end));
    }

    // sky gradient by time of day
    private LinearGradient buildSkyGradient(int hour, int minute, boolean isNight) {
        double t = hour + minute / 60.0;
        if (isNight) {
            return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#03030f")),
                new Stop(1, Color.web("#0d1b4b")));
        } else if (t < 6.8 || t > 17.2) {
            // sunrise/sunset
            return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0,  Color.web("#1a1050")),
                new Stop(0.38, Color.web("#e8632a")),
                new Stop(0.72, Color.web("#f4a346")),
                new Stop(1.0,  Color.web("#ffd57a")));
        } else {
            // daytime
            return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0d47a1")),
                new Stop(1, Color.web("#64B5F6")));
        }
    }

    private void styleTabButton(Button btn, double prefW, double prefH) {
        String base =
            "-fx-background-color: #6a4fa3; -fx-background-radius: 6;" +
            "-fx-text-fill: white; -fx-font-family: '.SF NS Text'; -fx-font-size: 11px;" +
            "-fx-font-weight: bold; -fx-cursor: hand;";
        String hover =
            "-fx-background-color: #8060c0; -fx-background-radius: 6;" +
            "-fx-text-fill: white; -fx-font-family: '.SF NS Text'; -fx-font-size: 11px;" +
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

    // triangle polygon helper
    private Polygon poly(double x1, double y1, double x2, double y2, double x3, double y3) {
        return new Polygon(x1, y1, x2, y2, x3, y3);
    }

    // filled rectangle helper
    private void rect(Pane p, double x, double y, double w, double h,
                      Color fill, Color stroke, double sw) {
        Rectangle r = new Rectangle(x, y, w, h);
        r.setFill(fill);
        if (sw > 0) { r.setStroke(stroke); r.setStrokeWidth(sw); }
        p.getChildren().add(r);
    }

    // line helper
    private void line(Pane p, double x1, double y1, double x2, double y2,
                      Color stroke, double sw) {
        Line l = new Line(x1, y1, x2, y2);
        l.setStroke(stroke); l.setStrokeWidth(sw);
        p.getChildren().add(l);
    }

    // weather state from forecast text
    private WeatherState getWeatherState(Period p) {
        String f = (p.shortForecast != null) ? p.shortForecast : "";
        int precip = (p.probabilityOfPrecipitation != null) ? p.probabilityOfPrecipitation.value : 0;
        if (f.contains("Snow") || f.contains("Flurr") || f.contains("Blizzard")) return WeatherState.SNOW;
        if (f.contains("Rain") || f.contains("Shower") || f.contains("Thunder") || f.contains("Drizzle")) return WeatherState.RAIN;
        if (precip >= 50) return (p.temperature <= 32) ? WeatherState.SNOW : WeatherState.RAIN;
        if (f.contains("Cloud") || f.contains("Overcast")) return WeatherState.CLOUDY;
        return WeatherState.CLEAR;
    }

    // fade to new weather effect
    private void switchWeatherEffect(WeatherState state) {
        if (currentFadeAnim != null) { currentFadeAnim.stop(); currentFadeAnim = null; }
        // stop the currently running effect before starting the next one
        if (activeEffect != null) { activeEffect.stop(); activeEffect = null; }

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
        // instantiate the correct subclass and call the template method
        switch (state) {
            case RAIN:   activeEffect = new RainEffect(effectsPane);   activeEffect.build(); break;
            case SNOW:   activeEffect = new SnowEffect(effectsPane);   activeEffect.build(); break;
            case CLOUDY: activeEffect = new CloudEffect(effectsPane);  activeEffect.build(); break;
            default:     activeEffect = null; break; // CLEAR: nothing to draw
        }
    }

    // -------------------------------------------------------------------------
    // pattern: adapter — wraps Period and exposes clean display methods
    // -------------------------------------------------------------------------
    static class PeriodAdapter {
        // the period being adapted
        private Period period;

        // constructor stores the wrapped period
        PeriodAdapter(Period period) {
            this.period = period;
        }

        // display string for the temperature label
        String getDisplayTemperature() {
            // formats temperature with unit suffix
            return period.temperature + "°F";
        }

        // window color based on whether the period is daytime
        Color getWindowColor() {
            // yellow for night, light blue for day
            return period.isDaytime ? Color.web("#a8d8f0") : Color.web("#ffe066");
        }

        // short description, never null
        String getDescription() {
            // fall back to "No data" if the api returned null
            return (period.shortForecast != null) ? period.shortForecast : "No data";
        }

        // precipitation value, never null
        int getPrecipitation() {
            // return 0 if the api did not include precipitation data
            return (period.probabilityOfPrecipitation != null)
                ? period.probabilityOfPrecipitation.value : 0;
        }

        // row hour derived from index and current time
        static boolean isNightHour(int rowIndex) {
            // add the row offset to current hour and wrap around midnight
            int h = (java.time.LocalDateTime.now().getHour() + rowIndex) % 24;
            // night is defined as 7pm or later, or before 6am
            return h >= 19 || h < 6;
        }
    }

    // -------------------------------------------------------------------------
    // pattern: template method — defines the build steps, subclasses fill them in
    // -------------------------------------------------------------------------
    abstract static class WeatherEffect {
        // pane that nodes are added to
        protected Pane pane;
        // animation timeline controlled by the template
        protected Timeline timeline;

        // constructor receives the target pane
        WeatherEffect(Pane pane) {
            this.pane = pane;
        }

        // template method — order of steps is fixed here
        final void build() {
            // step 1: create and add all shape nodes
            createNodes();
            // step 2: configure the animation timeline
            setupTimeline();
            // run the animation forever
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.play();
        }

        // subclasses create and add their shape nodes to pane
        abstract void createNodes();

        // subclasses define the animation keyframe and assign to timeline
        abstract void setupTimeline();

        // stops the timeline if it is running
        void stop() {
            if (timeline != null) timeline.stop();
        }
    }

    // rain effect subclass — falling line drops
    static class RainEffect extends WeatherEffect {
        // drop count
        private static final int N = 90;
        // shape refs reused across frames
        private Line[] drops = new Line[N];
        // per-drop position, speed, and length
        private double[] dropY = new double[N];
        private double[] dropX = new double[N];
        private double[] speed = new double[N];
        private double[] len   = new double[N];

        // constructor passes pane to base class
        RainEffect(Pane pane) { super(pane); }

        @Override
        void createNodes() {
            // initialize each drop at a random position with random speed and length
            Random rng = new Random();
            for (int i = 0; i < N; i++) {
                dropX[i] = rng.nextDouble() * W;
                dropY[i] = rng.nextDouble() * H;
                speed[i] = 5 + rng.nextDouble() * 5;
                len[i]   = 9 + rng.nextDouble() * 9;
                // slight rightward angle on the end point simulates falling angle
                drops[i] = new Line(dropX[i], dropY[i], dropX[i] + 1.5, dropY[i] + len[i]);
                drops[i].setStroke(Color.web("#90c8f0", 0.42));
                drops[i].setStrokeWidth(1);
                pane.getChildren().add(drops[i]);
            }
        }

        @Override
        void setupTimeline() {
            // 16ms frame = ~60fps, move each drop down by its speed
            timeline = new Timeline(new KeyFrame(Duration.millis(16), e -> {
                for (int i = 0; i < N; i++) {
                    dropY[i] += speed[i];
                    // wrap to top when drop exits the bottom
                    if (dropY[i] > H + 20) { dropY[i] = -20; dropX[i] = new Random().nextDouble() * W; }
                    drops[i].setStartX(dropX[i]); drops[i].setStartY(dropY[i]);
                    drops[i].setEndX(dropX[i] + 1.5); drops[i].setEndY(dropY[i] + len[i]);
                }
            }));
        }
    }

    // snow effect subclass — drifting circle flakes
    static class SnowEffect extends WeatherEffect {
        // flake count
        private static final int N = 65;
        // shape refs reused across frames
        private Circle[] flakes = new Circle[N];
        // per-flake position and motion parameters
        private double[] fx = new double[N], fy = new double[N];
        private double[] speed = new double[N], drift = new double[N], phase = new double[N];

        // constructor passes pane to base class
        SnowEffect(Pane pane) { super(pane); }

        @Override
        void createNodes() {
            // scatter flakes randomly across the pane with varying sizes
            Random rng = new Random();
            for (int i = 0; i < N; i++) {
                fx[i] = rng.nextDouble() * W;
                fy[i] = rng.nextDouble() * H;
                speed[i] = 0.7 + rng.nextDouble() * 1.1;
                drift[i] = 0.3 + rng.nextDouble() * 0.5;
                // random phase so flakes don't all sway in sync
                phase[i] = rng.nextDouble() * Math.PI * 2;
                double r = 1.8 + rng.nextDouble() * 2.5;
                flakes[i] = new Circle(fx[i], fy[i], r, Color.web("#e8f4ff", 0.85));
                pane.getChildren().add(flakes[i]);
            }
        }

        @Override
        void setupTimeline() {
            // 33ms frame = ~30fps, move flakes down and sway them sideways
            final long[] frame = {0};
            timeline = new Timeline(new KeyFrame(Duration.millis(33), e -> {
                frame[0]++;
                for (int i = 0; i < N; i++) {
                    fy[i] += speed[i];
                    // sine-based lateral drift gives gentle swaying motion
                    fx[i] += Math.sin(phase[i] + frame[0] * 0.04) * drift[i];
                    // wrap to top when flake exits the bottom
                    if (fy[i] > H + 10) { fy[i] = -10; fx[i] = new Random().nextDouble() * W; }
                    flakes[i].setCenterX(fx[i]);
                    flakes[i].setCenterY(fy[i]);
                }
            }));
        }
    }

    // cloud effect subclass — static overlay and ellipse clouds
    static class CloudEffect extends WeatherEffect {
        // constructor passes pane to base class
        CloudEffect(Pane pane) { super(pane); }

        @Override
        void createNodes() {
            // semi-transparent dark overlay dims the sky
            Rectangle overlay = new Rectangle(0, 0, W, H);
            overlay.setFill(Color.web("#404860", 0.28));
            pane.getChildren().add(overlay);
            // each row is {cx, cy, rx, ry} for the ellipse
            double[][] clouds = {
                {130, 75,  95, 32}, {310, 52, 115, 36}, {510, 68, 105, 33},
                {695, 58,  88, 28}, {840, 88,  78, 26}
            };
            // add each cloud ellipse with a soft fill
            for (double[] c : clouds) {
                Ellipse e = new Ellipse(c[0], c[1], c[2], c[3]);
                e.setFill(Color.web("#c8d0e0", 0.20));
                pane.getChildren().add(e);
            }
        }

        @Override
        void setupTimeline() {
            // clouds are static — empty timeline satisfies the template contract
            timeline = new Timeline();
        }
    }
}
