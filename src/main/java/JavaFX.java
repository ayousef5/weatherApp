import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import weather.Period;
import weather.WeatherAPI;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class JavaFX extends Application {

	Scene todayScene;
	Scene forecastScene;

	public static void main(String[] args) {
		launch(args);
	}

	// -------------------------------------------------------
	// Helper: styled Label
	// -------------------------------------------------------
	private Label styledLabel(String text, double size,
							  boolean bold, Color color) {
		Label l = new Label(text);
		l.setFont(bold
				? Font.font("Segoe UI", FontWeight.BOLD, size)
				: Font.font("Segoe UI", size));
		l.setTextFill(color);
		l.setTextAlignment(TextAlignment.CENTER);
		l.setWrapText(true);
		l.setMaxWidth(200);
		return l;
	}

	// -------------------------------------------------------
	// Fetch tips from Advice Slip API (free, no key needed)
	// -------------------------------------------------------
	private String[] fetchTips(int count) {
		String[] tips = new String[count];
		String fallback = "Stay prepared for changing conditions.";

		for (int i = 0; i < count; i++) {
			try {
				java.net.URL url = new java.net.URL("https://api.adviceslip.com/advice");
				java.net.HttpURLConnection conn =
						(java.net.HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				conn.setRequestProperty("Cache-Control", "no-cache");
				conn.setConnectTimeout(4000);
				conn.setReadTimeout(4000);

				if (conn.getResponseCode() == 200) {
					java.util.Scanner sc =
							new java.util.Scanner(conn.getInputStream(), "UTF-8");
					String body = sc.useDelimiter("\\A").next();
					sc.close();

					int adviceKey = body.indexOf("\"advice\"");
					if (adviceKey != -1) {
						int colon  = body.indexOf(":", adviceKey);
						int openQ  = body.indexOf("\"", colon);
						int closeQ = body.indexOf("\"", openQ + 1);
						if (openQ != -1 && closeQ > openQ) {
							tips[i] = body.substring(openQ + 1, closeQ);
						} else {
							tips[i] = fallback;
						}
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
	public void start(Stage primaryStage) throws Exception {

		primaryStage.setTitle("Professional Weather App 🌤");

		// =====================================================
		// TIME / DAY-NIGHT
		// =====================================================
		LocalDateTime now = LocalDateTime.now();
		int hour = now.getHour();
		boolean isNight = (hour >= 18 || hour < 6);

		String scene1Background = isNight ? "/scene1Night.png" : "/scene1Day.png";
		String scene2Background = isNight ? "/scene2Night.png" : "/scene2Day.png";

		// =====================================================
		// WEATHER DATA
		// =====================================================
		ArrayList<Period> forecast = WeatherAPI.getForecast("LOT", 77, 70);
		if (forecast == null || forecast.size() < 7) {
			throw new RuntimeException("Forecast did not load properly");
		}

		// =====================================================
		// SCENE 1 — TODAY (background + button only)
		// =====================================================
		Image bgImg1 = new Image(
				getClass().getResource(scene1Background).toExternalForm());

		ImageView bg1Sharp = new ImageView(bgImg1);
		bg1Sharp.setFitWidth(960);
		bg1Sharp.setFitHeight(540);
		bg1Sharp.setPreserveRatio(false);

		Button laterButton = new Button("Later this week  →");
		laterButton.setStyle(
				"-fx-background-color: rgba(255,255,255,0.20);" +
						"-fx-background-radius: 24;" +
						"-fx-border-color: rgba(255,255,255,0.55);" +
						"-fx-border-radius: 24;" +
						"-fx-border-width: 1.5;" +
						"-fx-text-fill: white;" +
						"-fx-font-family: 'Segoe UI';" +
						"-fx-font-size: 14px;" +
						"-fx-font-weight: bold;" +
						"-fx-padding: 10 22 10 22;" +
						"-fx-cursor: hand;"
		);
		laterButton.setOnMouseEntered(e -> laterButton.setStyle(
				"-fx-background-color: rgba(255,255,255,0.35);" +
						"-fx-background-radius: 24;" +
						"-fx-border-color: white;" +
						"-fx-border-radius: 24;" +
						"-fx-border-width: 1.5;" +
						"-fx-text-fill: white;" +
						"-fx-font-family: 'Segoe UI';" +
						"-fx-font-size: 14px;" +
						"-fx-font-weight: bold;" +
						"-fx-padding: 10 22 10 22;" +
						"-fx-cursor: hand;"
		));
		laterButton.setOnMouseExited(e -> laterButton.setStyle(
				"-fx-background-color: rgba(255,255,255,0.20);" +
						"-fx-background-radius: 24;" +
						"-fx-border-color: rgba(255,255,255,0.55);" +
						"-fx-border-radius: 24;" +
						"-fx-border-width: 1.5;" +
						"-fx-text-fill: white;" +
						"-fx-font-family: 'Segoe UI';" +
						"-fx-font-size: 14px;" +
						"-fx-font-weight: bold;" +
						"-fx-padding: 10 22 10 22;" +
						"-fx-cursor: hand;"
		));

		HBox bottomRight = new HBox(laterButton);
		bottomRight.setAlignment(Pos.BOTTOM_RIGHT);
		bottomRight.setPadding(new Insets(0, 28, 22, 0));

		BorderPane scene1Layout = new BorderPane();
		scene1Layout.setBottom(bottomRight);

		StackPane root1 = new StackPane(bg1Sharp, scene1Layout);
		todayScene = new Scene(root1, 960, 540);

		// =====================================================
		// SCENE 2 — MULTI-DAY FORECAST
		// =====================================================
		Image bgImg2 = new Image(
				getClass().getResource(scene2Background).toExternalForm());

		ImageView bg2Sharp = new ImageView(bgImg2);
		bg2Sharp.setFitWidth(960);
		bg2Sharp.setFitHeight(540);
		bg2Sharp.setPreserveRatio(false);

		// --- Back button ---
		Button backButton = new Button("← Back to today!");
		backButton.setStyle(
				"-fx-background-color: #7B5EA7;" +
						"-fx-background-radius: 24;" +
						"-fx-text-fill: white;" +
						"-fx-font-family: 'Segoe UI';" +
						"-fx-font-size: 13px;" +
						"-fx-font-weight: bold;" +
						"-fx-padding: 8 18 8 18;" +
						"-fx-cursor: hand;"
		);

		// --- "Chicago" top-center ---
		Label cityLabel2 = styledLabel("Chicago", 28, true, Color.BLACK);

		BorderPane topBar = new BorderPane();
		topBar.setLeft(backButton);
		topBar.setCenter(cityLabel2);
		BorderPane.setAlignment(backButton, Pos.CENTER_LEFT);
		BorderPane.setAlignment(cityLabel2, Pos.CENTER);
		topBar.setPadding(new Insets(14, 20, 6, 16));

		// =====================================================
		// FETCH LIVE TIPS FROM ADVICE SLIP API
		// =====================================================
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

			// Header
			String dayName   = (c == 0) ? "Today" : dayP.name;
			Label cardHeader = styledLabel(dayName, 16, true, Color.BLACK);
			Label cardTemp   = styledLabel(dayP.temperature + "°", 16, true, Color.BLACK);
			HBox cardTitle   = new HBox(6, cardHeader, cardTemp);
			cardTitle.setAlignment(Pos.CENTER);

			Region headerSep = new Region();
			headerSep.setPrefHeight(2);
			headerSep.setPrefWidth(CARD_W);
			headerSep.setStyle("-fx-background-color: rgba(100,100,180,0.4);");

			// DAY section
			Label dayIcon = new Label("☀ DAY:");
			dayIcon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
			dayIcon.setTextFill(Color.rgb(50, 50, 50));

			Label dayTemp  = makeWrappingLabel("Temp: " + dayP.temperature + "°F", CARD_W);
			Label dayWind  = makeWrappingLabel("Wind: " + dayP.windDirection + " " + dayP.windSpeed, CARD_W);
			Label dayPrec  = makeWrappingLabel("Precipitation: " + dayP.probabilityOfPrecipitation.value + "%", CARD_W);
			Label dayDesc  = makeWrappingLabel(dayP.shortForecast, CARD_W);
			dayDesc.setTextFill(Color.rgb(70, 70, 70));
			dayDesc.setFont(Font.font("Segoe UI", javafx.scene.text.FontPosture.ITALIC, 11));

			VBox daySection = new VBox(3, dayIcon, dayTemp, dayWind, dayPrec, dayDesc);
			daySection.setAlignment(Pos.CENTER_LEFT);

			Region midSep = new Region();
			midSep.setPrefHeight(1);
			midSep.setPrefWidth(CARD_W);
			midSep.setStyle("-fx-background-color: rgba(100,100,180,0.25);");

			// NIGHT section
			Label nightIcon = new Label("🌙 NIGHT:");
			nightIcon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
			nightIcon.setTextFill(Color.rgb(50, 50, 50));

			Label nightTemp = makeWrappingLabel("Temp: " + nightP.temperature + "°F", CARD_W);
			Label nightWind = makeWrappingLabel("Wind: " + nightP.windDirection + " " + nightP.windSpeed, CARD_W);
			Label nightPrec = makeWrappingLabel("Precipitation: " + nightP.probabilityOfPrecipitation.value + "%", CARD_W);
			Label nightDesc = makeWrappingLabel(nightP.shortForecast, CARD_W);
			nightDesc.setTextFill(Color.rgb(70, 70, 70));
			nightDesc.setFont(Font.font("Segoe UI", javafx.scene.text.FontPosture.ITALIC, 11));

			VBox nightSection = new VBox(3, nightIcon, nightTemp, nightWind, nightPrec, nightDesc);
			nightSection.setAlignment(Pos.CENTER_LEFT);

			Region botSep = new Region();
			botSep.setPrefHeight(1);
			botSep.setPrefWidth(CARD_W);
			botSep.setStyle("-fx-background-color: rgba(100,100,180,0.25);");

			// TIP section
			Label tipIcon = new Label("💡 Tip of the day");
			tipIcon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
			tipIcon.setTextFill(Color.rgb(50, 50, 50));

			Label tipText = makeWrappingLabel("\"" + tips[c] + "\"", CARD_W);
			tipText.setTextFill(Color.rgb(70, 70, 70));
			tipText.setFont(Font.font("Segoe UI", javafx.scene.text.FontPosture.ITALIC, 11));
			tipText.setTextAlignment(TextAlignment.CENTER);
			tipText.setAlignment(Pos.CENTER);

			VBox tipSection = new VBox(4, tipIcon, tipText);
			tipSection.setAlignment(Pos.CENTER);

			// Assemble card content
			VBox cardContent = new VBox(8,
					cardTitle, headerSep,
					daySection, midSep,
					nightSection, botSep,
					tipSection);
			cardContent.setAlignment(Pos.TOP_LEFT);
			cardContent.setPadding(new Insets(10, 10, 10, 10));
			cardContent.setPrefWidth(CARD_W);
			cardContent.setMaxWidth(CARD_W);

			Region cardBg = new Region();
			cardBg.setStyle(
					"-fx-background-color: rgba(200,210,235,0.55);" +
							"-fx-background-radius: 18;" +
							"-fx-border-color: rgba(255,255,255,0.6);" +
							"-fx-border-radius: 18;" +
							"-fx-border-width: 1.2;"
			);
			cardBg.setEffect(new GaussianBlur(18));

			StackPane card = new StackPane(cardBg, cardContent);
			card.setMaxWidth(CARD_W + 20);
			card.setAlignment(Pos.TOP_LEFT);

			javafx.scene.shape.Rectangle cardClip = new javafx.scene.shape.Rectangle();
			cardClip.setArcWidth(36);
			cardClip.setArcHeight(36);
			card.setClip(cardClip);
			card.layoutBoundsProperty().addListener((obs, o, n) -> {
				cardClip.setWidth(n.getWidth());
				cardClip.setHeight(n.getHeight());
			});

			cardsRow.getChildren().add(card);
		}

		VBox cardsWrapper = new VBox(cardsRow);
		cardsWrapper.setAlignment(Pos.TOP_CENTER);

		VBox scene2Layout = new VBox(0, topBar, cardsWrapper);
		scene2Layout.setAlignment(Pos.TOP_CENTER);

		StackPane root2Stack = new StackPane(bg2Sharp, scene2Layout);
		forecastScene = new Scene(root2Stack, 960, 540);

		// =====================================================
		// BUTTON ACTIONS
		// =====================================================
		laterButton.setOnAction(e -> primaryStage.setScene(forecastScene));
		backButton.setOnAction(e -> primaryStage.setScene(todayScene));

		primaryStage.setScene(todayScene);
		primaryStage.show();
	}

	// -------------------------------------------------------
	// Helper: wrapping label with consistent style
	// -------------------------------------------------------
	private Label makeWrappingLabel(String text, double maxW) {
		Label l = new Label(text);
		l.setFont(Font.font("Segoe UI", 11));
		l.setTextFill(Color.rgb(50, 50, 50));
		l.setWrapText(true);
		l.setMaxWidth(maxW);
		l.setTextAlignment(TextAlignment.LEFT);
		return l;
	}
}