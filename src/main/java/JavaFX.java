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
// Our main weather app class
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

    // weather animation pane and active effect
    private Pane effectsPane;
    private WeatherEffect activeEffect;
    private Animation currentFadeAnim;
    private Timeline clockTimeline;

    // scene 1 background and house refs for click-driven color updates
    private Pane bgPane;
    private Pane housePn;
    private Rectangle leftWin;
    private Rectangle rightWin;
    private Polygon[] backMtns  = new Polygon[2];
    private Polygon[] frontMtns = new Polygon[6];

    private enum WeatherState { CLEAR, CLOUDY, RAIN, SNOW }

    public static void main(String[] args) { launch(args); }

    // switches the primary stage to the forecast scene
    public void showScene2() {
        primaryStage.setScene(forecastScene);
        primaryStage.setFullScreen(true);
    }

    // helper method that creates a fancy text label.
    private Label styledLabel(String text, double size, boolean bold, Color color) {
        Label l = new Label(text);
        l.setFont(bold ? Font.font("SansSerif", FontWeight.BOLD, size) : Font.font("SansSerif", size));
        l.setTextFill(color);
        l.setTextAlignment(TextAlignment.CENTER);
        l.setWrapText(true);
        l.setMaxWidth(200);
        return l;
    }

    //  helper that makes a small text label that wraps onto multiple lines
    private Label makeWrappingLabel(String text, double maxW) {
        Label l = new Label(text);
        l.setFont(Font.font("SansSerif", 11));
        l.setTextFill(Color.web("#c8d8f0"));
        l.setWrapText(true);
        l.setMaxWidth(maxW);
        l.setTextAlignment(TextAlignment.LEFT);
        return l;
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        stage.setTitle("Weather App");
        // Find out what time it is right now.
        LocalDateTime now = LocalDateTime.now();
        int hour   = now.getHour();
        int minute = now.getMinute();
        boolean isNight = (hour >= 19 || hour < 6);

        // load forecast data — crash early if the api returns nothing useful
        ArrayList<Period> forecast = WeatherAPI.getForecast("LOT", 77, 70);
        if (forecast == null || forecast.size() < 8)
            throw new RuntimeException("Forecast did not load properly");

        // screen dimensions used for fullscreen scaling transforms
        Rectangle2D screen = Screen.getPrimary().getBounds();
        double SW = screen.getWidth(), SH = screen.getHeight();

        StackPane scene1Root = buildScene1(forecast, hour, minute, isNight);
        // Stretch screen 1 to fill the monitor by scaling it up.
        scene1Root.getTransforms().add(
            new javafx.scene.transform.Scale(SW / W, SH / H, 0, 0));
        javafx.scene.Group fsRoot = new javafx.scene.Group(scene1Root);
        todayScene = new Scene(fsRoot, SW, SH);

        // Build the same sky-and-mountains background for screen 2.
        Pane bg2 = SceneBuilder.buildBackground(hour, minute, isNight);

        Button backButton = new Button("\u2190 Back");
        backButton.setStyle(
            "-fx-background-color: #7B5EA7; -fx-background-radius: 24; -fx-text-fill: white;" +
            "-fx-font-family: 'SansSerif'; -fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-padding: 8 18 8 18; -fx-cursor: hand;"
        );

        Label cityLabel2 = styledLabel("Chicago", 28, true, Color.WHITE);
        StackPane topBar = new StackPane();
        StackPane.setAlignment(backButton, Pos.CENTER_LEFT);
        StackPane.setAlignment(cityLabel2, Pos.CENTER);
        topBar.getChildren().addAll(cityLabel2, backButton);
        topBar.setPadding(new Insets(14, 20, 6, 16));

        // fetch one tip per card from the advice api
        String[] tips = WeatherTips.fetchTips(4);
        int[] dayIdx   = {0, 2, 4, 6};
        int[] nightIdx = {1, 3, 5, 7};

        // Choose text colors so they are easy to read whether it is day or night outside.
        Color cardHeaderColor = isNight ? Color.WHITE : Color.BLACK;
        Color cardBodyColor   = isNight ? Color.web("#c8d8f0") : Color.BLACK;
        Color cardItalicColor = isNight ? Color.web("#e6f0ff") : Color.web("#333333");

        HBox cardsRow = new HBox(10);
        cardsRow.setAlignment(Pos.TOP_CENTER);
        cardsRow.setPadding(new Insets(4, 10, 10, 10));
        // Loop 4 times to build 4 forecast cards
        for (int c = 0; c < 4; c++) {
            Period dayP   = forecast.get(dayIdx[c]);
            Period nightP = forecast.get(nightIdx[c]);
            final double CARD_W = 210;

            // first card says "Today", rest use the period name
            String dayName   = (c == 0) ? "Today" : dayP.name;
            Label cardHeader = styledLabel(dayName, 16, true, cardHeaderColor);
            Label cardTemp   = styledLabel(dayP.temperature + "\u00B0", 16, true, cardHeaderColor);
            HBox cardTitle   = new HBox(6, cardHeader, cardTemp);
            cardTitle.setAlignment(Pos.CENTER);

            Region headerSep = new Region();
            headerSep.setPrefHeight(2); headerSep.setPrefWidth(CARD_W);
            headerSep.setStyle("-fx-background-color: rgba(100,100,180,0.4);");

            // day section header and detail rows
            Label dayIconLbl = new Label("\u2600 DAY:");
            dayIconLbl.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
            dayIconLbl.setTextFill(cardHeaderColor);
            Label dayTempLbl = makeWrappingLabel("Temp: " + dayP.temperature + "\u00B0F", CARD_W);
            dayTempLbl.setTextFill(cardBodyColor);
            Label dayWindLbl = makeWrappingLabel("Wind: " + dayP.windDirection + " " + dayP.windSpeed, CARD_W);
            dayWindLbl.setTextFill(cardBodyColor);
            Label dayPrecLbl = makeWrappingLabel("Precipitation: " + dayP.probabilityOfPrecipitation.value + "%", CARD_W);
            dayPrecLbl.setTextFill(cardBodyColor);
            Label dayDescLbl = makeWrappingLabel(dayP.shortForecast, CARD_W);
            dayDescLbl.setTextFill(cardItalicColor);
            dayDescLbl.setFont(Font.font("SansSerif", FontPosture.ITALIC, 11));
            VBox daySection = new VBox(3, dayIconLbl, dayTempLbl, dayWindLbl, dayPrecLbl, dayDescLbl);
            daySection.setAlignment(Pos.CENTER_LEFT);

            Region midSep = new Region();
            midSep.setPrefHeight(1); midSep.setPrefWidth(CARD_W);
            midSep.setStyle("-fx-background-color: rgba(100,100,180,0.25);");

            // night section header and detail rows
            Label nightIconLbl = new Label("\u263D NIGHT:");
            nightIconLbl.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
            nightIconLbl.setTextFill(cardHeaderColor);
            Label nightTempLbl = makeWrappingLabel("Temp: " + nightP.temperature + "\u00B0F", CARD_W);
            nightTempLbl.setTextFill(cardBodyColor);
            Label nightWindLbl = makeWrappingLabel("Wind: " + nightP.windDirection + " " + nightP.windSpeed, CARD_W);
            nightWindLbl.setTextFill(cardBodyColor);
            Label nightPrecLbl = makeWrappingLabel("Precipitation: " + nightP.probabilityOfPrecipitation.value + "%", CARD_W);
            nightPrecLbl.setTextFill(cardBodyColor);
            Label nightDescLbl = makeWrappingLabel(nightP.shortForecast, CARD_W);
            nightDescLbl.setTextFill(cardItalicColor);
            nightDescLbl.setFont(Font.font("SansSerif", FontPosture.ITALIC, 11));
            VBox nightSection = new VBox(3, nightIconLbl, nightTempLbl, nightWindLbl, nightPrecLbl, nightDescLbl);
            nightSection.setAlignment(Pos.CENTER_LEFT);

            Region botSep = new Region();
            botSep.setPrefHeight(1); botSep.setPrefWidth(CARD_W);
            botSep.setStyle("-fx-background-color: rgba(100,100,180,0.25);");

            // tip section — one random advice slip per card
            Label tipIconLbl = new Label("\u2605 Tip of the day");
            tipIconLbl.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
            tipIconLbl.setTextFill(cardHeaderColor);
            Label tipText = makeWrappingLabel("\"" + tips[c] + "\"", CARD_W);
            tipText.setTextFill(cardItalicColor);
            tipText.setFont(Font.font("SansSerif", FontPosture.ITALIC, 11));
            tipText.setTextAlignment(TextAlignment.CENTER);
            tipText.setAlignment(Pos.CENTER);
            VBox tipSection = new VBox(4, tipIconLbl, tipText);
            tipSection.setAlignment(Pos.CENTER);

            VBox cardContent = new VBox(8, cardTitle, headerSep, daySection, midSep, nightSection, botSep, tipSection);
            cardContent.setAlignment(Pos.TOP_LEFT);
            cardContent.setPadding(new Insets(10));
            cardContent.setPrefWidth(CARD_W); cardContent.setMaxWidth(CARD_W);

            // frosted card background with rounded clip
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
            // keep clip rectangle in sync with card size
            card.layoutBoundsProperty().addListener((obs, o, n) -> {
                cardClip.setWidth(n.getWidth()); cardClip.setHeight(n.getHeight());
            });
            cardsRow.getChildren().add(card);
        }
     // Wrap the row of cards in a VBox so it can be centered vertically in the window.
        VBox cardsWrapper = new VBox(cardsRow);
        cardsWrapper.setAlignment(Pos.CENTER);
        VBox scene2Layout = new VBox(0, topBar, cardsWrapper);
        scene2Layout.setAlignment(Pos.CENTER);
        scene2Layout.setPrefHeight(H);
        VBox.setVgrow(cardsWrapper, Priority.ALWAYS);
        StackPane root2 = new StackPane(bg2, scene2Layout);
        // Stretch screen 2 to fill the monitor
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

    // This method builds all the stuff you see on screen 1: background, house, effects, ui
    private StackPane buildScene1(ArrayList<Period> forecast, int hour, int minute, boolean isNight) {
        StackPane root = new StackPane();

        // Build the sky-and-mountains background and remember the mountain shapes
        bgPane = SceneBuilder.buildBackground(hour, minute, isNight);
        backMtns  = SceneBuilder.backMtns;
        frontMtns = SceneBuilder.frontMtns;

        // Build the house and remember the window rectangles
        housePn = SceneBuilder.buildHouse(isNight);
        leftWin  = SceneBuilder.leftWin;
        rightWin = SceneBuilder.rightWin;

        effectsPane = new Pane();
        effectsPane.setPrefSize(W, H);
        effectsPane.setPickOnBounds(false);
        // mouse events pass through the effects layer to the ui below
        effectsPane.setMouseTransparent(true);

        Pane uiPane = buildUI(forecast, root);
        root.getChildren().addAll(bgPane, housePn, effectsPane, uiPane);

        // kick off the initial weather animation
        switchWeatherEffect(getWeatherState(forecast.get(0)));
        return root;
    }

    // Returns a label for a row in the hourly list.
    private static String hourLabel(int i) {
        if (i == 0) return "Now";
        int h = (LocalDateTime.now().getHour() + i) % 24;
        if (h == 0)       return "12 AM";
        else if (h < 12)  return h + " AM";
        else if (h == 12) return "12 PM";
        else              return (h - 12) + " PM";
    }

    // Looks at a weather description like "Rain Showers" and returns a matching symbol.
    private static String weatherEmoji(String forecast) {
        if (forecast == null) return "?";
        String f = forecast.toLowerCase();
        if (f.contains("thunderstorm") || f.contains("thunder"))                return "\u26A1"; // ⚡
        if (f.contains("blizzard") || f.contains("snow") || f.contains("snowy")) return "\u2744"; // ❄
        if (f.contains("sleet") || f.contains("freezing rain"))                 return "\u2744"; // ❄
        if (f.contains("rain") || f.contains("rainy") || f.contains("shower"))  return "\u2602"; // ☂
        if (f.contains("drizzle"))                                               return "\u2602"; // ☂
        if (f.contains("fog") || f.contains("mist"))                            return "\u2248"; // ≈
        if (f.contains("wind") || f.contains("breezy") || f.contains("blustery")) return "\u007E\u007E"; // ~~
        if (f.contains("mostly cloudy") || f.contains("overcast"))              return "\u2601"; // ☁
        if (f.contains("partly cloudy") || f.contains("partly sunny"))          return "\u2601"; // ☁
        if (f.contains("mostly clear") || f.contains("mostly sunny"))           return "\u2600"; // ☀
        if (f.contains("sunny") || f.contains("clear"))                         return "\u2600"; // ☀
        if (f.contains("cloud"))                                                 return "\u2601"; // ☁
        return "\u25CB"; // ○
    }

    // builds the entire User Interface that you see on screen 1
    // The left panel with hourly rows
    // the big temp number in the midle
    // The description and hi/lo labels on the top right
    // The sideways "Later this week" button on the right edge
    private Pane buildUI(ArrayList<Period> forecast, StackPane root) {
        Pane pane = new Pane();
        pane.setPrefSize(W, H);
        pane.setPickOnBounds(false);

        final double PANEL_W = 255;
        // inner row width leaves room for the panel padding
        final double ROW_W   = PANEL_W - 12;
         // hourly forecast title
        Label headerLbl = new Label("HOURLY FORECAST");
        headerLbl.setFont(Font.font("SansSerif", FontWeight.BOLD, 9));
        headerLbl.setTextFill(Color.WHITE);
        headerLbl.setMaxWidth(ROW_W);
        headerLbl.setPrefWidth(ROW_W);
        headerLbl.setAlignment(Pos.CENTER);
        headerLbl.setTextAlignment(TextAlignment.CENTER);
        headerLbl.setPadding(new Insets(0, 0, 4, 0));
          // visual divider line
        Region headerSep = new Region();
        headerSep.setPrefHeight(1);
        headerSep.setPrefWidth(ROW_W);
        headerSep.setStyle("-fx-background-color: rgba(255,255,255,0.15);");

        int numRows = Math.min(10, forecast.size());
        final int[] selectedIdx = {0};
        List<HBox> rowList = new ArrayList<>();
        VBox rowsBox = new VBox(0);

        // timeLabels stores the time text for each row so the clock timer can update them
        Label[] timeLabels = new Label[numRows];
        // builds each hourly row
        for (int i = 0; i < numRows; i++) {
            Period p      = forecast.get(i);
            final int idx = i;
            // PeriodAdapter wraps the Period object and handles missing/null data safely
            PeriodAdapter adapter = new PeriodAdapter(p);

            Label timeLbl = new Label(hourLabel(i));
            timeLbl.setFont(Font.font("SansSerif", 10));
            timeLbl.setTextFill(Color.web("#ddeeff"));
            timeLbl.setPrefWidth(90);
            timeLbl.setMinWidth(90);
            timeLbl.setPadding(new Insets(0, 0, 0, 10));
            timeLabels[i] = timeLbl;

            // Weather symbol
            Label iconLbl = new Label(weatherEmoji(adapter.getDescription()));
            iconLbl.setFont(Font.font("SansSerif", 18));
            iconLbl.setTextFill(Color.WHITE);
            StackPane iconCell = new StackPane(iconLbl);
            iconCell.setPrefWidth(35);
            iconCell.setMinWidth(35);
            iconCell.setAlignment(Pos.CENTER);

            // Precipitation percentage
            int precip = adapter.getPrecipitation();
            Label precipLbl = new Label(precip + "%");
            precipLbl.setFont(Font.font("SansSerif", 9));
            precipLbl.setTextFill(Color.web("#7ab8f5"));
            precipLbl.setOpacity(0.85);
            precipLbl.setPrefWidth(50);
            precipLbl.setMinWidth(50);
            precipLbl.setAlignment(Pos.CENTER_RIGHT);

            // Temperature label (e.g. "68°F")
            Label tempLbl = new Label(adapter.getDisplayTemperature());
            tempLbl.setFont(Font.font("SansSerif", FontWeight.BOLD, 10));
            tempLbl.setTextFill(Color.WHITE);
            tempLbl.setPrefWidth(50);
            tempLbl.setAlignment(Pos.CENTER_RIGHT);
            // Put all four pieces (time, icon, precip, temp) side by side in 1 horizontal row
            HBox row = new HBox(0, timeLbl, iconCell, precipLbl, tempLbl);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(5, 6, 5, 6));
            row.setPrefWidth(ROW_W);

            // row 0 starts selected
            boolean isSelected = (i == 0);
            applyRowStyle(row, timeLbl, precipLbl, tempLbl, isSelected);

            // What happens when the user CLICKS a row:
            row.setOnMouseClicked(e -> {
                selectedIdx[0] = idx;
                // Loop through all rows and update their highlight color.
                // Only the clicked row gets the purple highlight
                for (int j = 0; j < rowList.size(); j++) {
                    HBox r   = rowList.get(j);
                    Label tl  = (Label) r.getChildren().get(0);
                    Label pl  = (Label) r.getChildren().get(2);
                    Label tpL = (Label) r.getChildren().get(3);
                    applyRowStyle(r, tl, pl, tpL, j == idx);
                }
                // Update the big temperature, description, and hi/lo labels at the top.
                bigTempLbl.setText(adapter.getDisplayTemperature());
                topDescLbl.setText(adapter.getDescription());
                // Recalculate hi/lo by scanning all forecast periods
                int hi2 = Integer.MIN_VALUE, lo2 = Integer.MAX_VALUE;
                for (Period fp : forecast) {
                    if (fp.temperature > hi2) hi2 = fp.temperature;
                    if (fp.temperature < lo2) lo2 = fp.temperature;
                }
                topHiLoLbl.setText("H: " + hi2 + "\u00B0   L: " + lo2 + "\u00B0");
                switchWeatherEffect(getWeatherState(p));
                // Figure out if this row's projected time is day or night
                boolean rowIsNight = PeriodAdapter.isNightHour(idx);
                int rowHour = (LocalDateTime.now().getHour() + idx) % 24;
                // Change the sky color to match the time of the selected row.
                Rectangle skyR = (Rectangle) bgPane.getChildren().get(0);
                skyR.setFill(SceneBuilder.buildSkyGradient(rowHour, 0, rowIsNight));
                Color winColor = rowIsNight ? Color.web("#ffe066") : Color.web("#a8d8f0");
                leftWin.setFill(winColor);
                rightWin.setFill(winColor);
                Color bkMtn = rowIsNight ? Color.web("#1e3a52") : Color.web("#4a6a8a");
                Color ftMtn = rowIsNight ? Color.web("#0f2030") : Color.web("#3a5a6a");
                for (Polygon m : backMtns)  m.setFill(bkMtn);
                for (Polygon m : frontMtns) m.setFill(ftMtn);
            });

            // What happens when the mouse HOVERS over a row but it is not selected:
            // give a little bit of shade on the row so the user knows they can click it
            row.setOnMouseEntered(e -> {
                if (selectedIdx[0] != idx)
                    row.setStyle("-fx-background-color: rgba(255,255,255,0.08);");
            });
            // remove hover higlight when the mouse leaves the row
            row.setOnMouseExited(e -> {
                if (selectedIdx[0] != idx)
                    row.setStyle("-fx-background-color: transparent;");
            });

            rowList.add(row);
            rowsBox.getChildren().add(row);
        }

        // clock timeline updates all time labels once per minute
        if (clockTimeline != null) clockTimeline.stop();
        clockTimeline = new Timeline(new KeyFrame(Duration.minutes(1), e -> {
            for (int i = 0; i < timeLabels.length; i++)
                timeLabels[i].setText(hourLabel(i));
        }));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();

        // labels above the data rows telling you what each column means
        Label colTimeHdr = new Label("TIME");
        colTimeHdr.setFont(Font.font("SansSerif", FontWeight.BOLD, 8));
        colTimeHdr.setTextFill(Color.WHITE);
        colTimeHdr.setPrefWidth(90);
        colTimeHdr.setMinWidth(90);
        colTimeHdr.setPadding(new Insets(0, 0, 0, 10));
        Label colSkyHdr = new Label("SKY");
        colSkyHdr.setFont(Font.font("SansSerif", 8));
        colSkyHdr.setTextFill(Color.web("#aac4e0"));
        colSkyHdr.setPrefWidth(35);
        colSkyHdr.setMinWidth(35);
        colSkyHdr.setAlignment(Pos.CENTER);
        Label colPrecipHdr = new Label("PRECIP");
        colPrecipHdr.setFont(Font.font("SansSerif", 8));
        colPrecipHdr.setTextFill(Color.web("#7ab8f5"));
        colPrecipHdr.setPrefWidth(50); colPrecipHdr.setMinWidth(50);
        colPrecipHdr.setAlignment(Pos.CENTER_RIGHT);
        Label colTempHdr = new Label("TEMP");
        colTempHdr.setFont(Font.font("SansSerif", FontWeight.BOLD, 8));
        colTempHdr.setTextFill(Color.WHITE);
        colTempHdr.setPrefWidth(50); colTempHdr.setMinWidth(50);
        colTempHdr.setAlignment(Pos.CENTER_RIGHT);
        HBox colHeaders = new HBox(0, colTimeHdr, colSkyHdr, colPrecipHdr, colTempHdr);
        colHeaders.setAlignment(Pos.CENTER_LEFT);
        colHeaders.setPadding(new Insets(2, 6, 2, 6));

        VBox panelContent = new VBox(0, headerLbl, headerSep, colHeaders, rowsBox);
        panelContent.setPadding(new Insets(10, 6, 10, 6));
        panelContent.setMaxWidth(PANEL_W);

        // solid-border panel background — no blur on the container
        Region panelBg = new Region();
        panelBg.setStyle(
            "-fx-background-color: rgba(6,14,42,0.62); -fx-background-radius: 14;" +
            "-fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 14; -fx-border-width: 1;"
        );

        StackPane leftPanel = new StackPane(panelBg, panelContent);
        leftPanel.setAlignment(Pos.TOP_LEFT);
        leftPanel.setMaxWidth(PANEL_W);
        leftPanel.setLayoutX(20);
        leftPanel.setLayoutY(45);
        pane.getChildren().add(leftPanel);

        // center temperature and city label
        Period current = forecast.get(0);
        bigTempLbl = new Label(current.temperature + "\u00B0F");
        bigTempLbl.setFont(Font.font("SansSerif", FontWeight.LIGHT, 52));
        bigTempLbl.setTextFill(Color.WHITE);
        Label cityLbl = new Label("Chicago");
        cityLbl.setFont(Font.font("SansSerif", FontWeight.NORMAL, 17));
        cityLbl.setTextFill(Color.web("#b8d4ee"));

        VBox centerBox = new VBox(1, bigTempLbl, cityLbl);
        centerBox.setAlignment(Pos.TOP_CENTER);
        centerBox.setLayoutX(0);
        centerBox.setLayoutY(18);
        centerBox.setPrefWidth(W);
        pane.getChildren().add(centerBox);

        // scan all periods for daily high and low
        int hi = Integer.MIN_VALUE, lo = Integer.MAX_VALUE;
        for (Period fp : forecast) {
            if (fp.temperature > hi) hi = fp.temperature;
            if (fp.temperature < lo) lo = fp.temperature;
        }

        // top-right: description and hi/lo
        topDescLbl = new Label(current.shortForecast != null ? current.shortForecast : "");
        topDescLbl.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));
        topDescLbl.setTextFill(Color.WHITE);
        topDescLbl.setWrapText(true);
        topDescLbl.setMaxWidth(145);
        topDescLbl.setTextAlignment(TextAlignment.RIGHT);

        topHiLoLbl = new Label("H: " + hi + "\u00B0   L: " + lo + "\u00B0");
        topHiLoLbl.setFont(Font.font("SansSerif", 11));
        topHiLoLbl.setTextFill(Color.web("#b8d4ee"));

        VBox topRight = new VBox(4, topDescLbl, topHiLoLbl);
        topRight.setAlignment(Pos.TOP_RIGHT);
        topRight.setLayoutX(W - 160);
        topRight.setLayoutY(14);
        pane.getChildren().add(topRight);

        // sideways "Later this week" tab on the right edge
        final double BTN_W = 118;
        final double BTN_H = 28;
        final double btnLayoutX = W - BTN_W / 2.0 - BTN_H / 2.0 - 2;

        Button laterBtn = new Button("Later this week");
        styleTabButton(laterBtn, BTN_W, BTN_H);
        laterBtn.setRotate(-90);
        laterBtn.setLayoutX(btnLayoutX);
        laterBtn.setLayoutY(H / 3.0 - BTN_H / 2.0);
        laterBtn.setOnAction(e -> showScene2());

        pane.getChildren().add(laterBtn);
        return pane;
    }

    // applies selected or unselected visual state to a row and its labels
    private void applyRowStyle(HBox row, Label timeLbl, Label precipLbl, Label tempLbl, boolean selected) {
        if (selected) {
            row.setStyle("-fx-background-color: #4a3a7a;");
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

    // Makes the "Later this week" tab button look purple normally
    private void styleTabButton(Button btn, double prefW, double prefH) {
        String base =
            "-fx-background-color: #6a4fa3; -fx-background-radius: 6;" +
            "-fx-text-fill: white; -fx-font-family: 'SansSerif'; -fx-font-size: 11px;" +
            "-fx-font-weight: bold; -fx-cursor: hand;";
        String hover =
            "-fx-background-color: #8060c0; -fx-background-radius: 6;" +
            "-fx-text-fill: white; -fx-font-family: 'SansSerif'; -fx-font-size: 11px;" +
            "-fx-font-weight: bold; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setPrefWidth(prefW);
        btn.setPrefHeight(prefH);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }

    // maps a forecast period to a weather animation state
    private WeatherState getWeatherState(Period p) {
        String f = (p.shortForecast != null) ? p.shortForecast : "";
        int precip = (p.probabilityOfPrecipitation != null) ? p.probabilityOfPrecipitation.value : 0;
        if (f.contains("Snow") || f.contains("Flurr") || f.contains("Blizzard")) return WeatherState.SNOW;
        if (f.contains("Rain") || f.contains("Shower") || f.contains("Thunder") || f.contains("Drizzle")) return WeatherState.RAIN;
        // high precip with freezing temp implies snow, otherwise rain
        if (precip >= 50) return (p.temperature <= 32) ? WeatherState.SNOW : WeatherState.RAIN;
        if (f.contains("Cloud") || f.contains("Overcast")) return WeatherState.CLOUDY;
        return WeatherState.CLEAR;
    }

    // fades out the current effect, clears the pane, then fades in the new one
    private void switchWeatherEffect(WeatherState state) {
        if (currentFadeAnim != null) { currentFadeAnim.stop(); currentFadeAnim = null; }
        // stop the old effect before clearing nodes
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

    // RAIN  -> creates falling raindrops.
    // SNOW  -> creates falling snowflakes.
    // CLOUDY -> creates drifting cloud shapes.
    // CLEAR  -> nothing to draw, so activeEffect stays null.
    private void populateWeatherEffect(WeatherState state) {
        switch (state) {
            case RAIN:   activeEffect = new RainEffect(effectsPane);   activeEffect.build(); break;
            case SNOW:   activeEffect = new SnowEffect(effectsPane);   activeEffect.build(); break;
            case CLOUDY: activeEffect = new CloudEffect(effectsPane);  activeEffect.build(); break;
            // CLEAR: nothing to draw
            default:     activeEffect = null; break;
        }
    }
}
