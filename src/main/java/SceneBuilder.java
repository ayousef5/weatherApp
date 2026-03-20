import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import java.util.Random;

// builds background/house; polygon+window refs for row click recolor
public class SceneBuilder {

    // mountain refs read by JavaFX after call
    public static Polygon[] backMtns  = new Polygon[2];
    public static Polygon[] frontMtns = new Polygon[6];

    // window refs read by JavaFX after call
    public static Rectangle leftWin;
    public static Rectangle rightWin;

    // sky, stars, mountains
    public static Pane buildBackground(int hour, int minute, boolean isNight) {
        Pane pane = new Pane();
        pane.setPrefSize(JavaFX.W, JavaFX.H);

        Rectangle sky = new Rectangle(0, 0, JavaFX.W, JavaFX.H);
        sky.setFill(buildSkyGradient(hour, minute, isNight));
        pane.getChildren().add(sky);

        if (isNight) { // fixed seed = same star layout each run
            Random rng = new Random(42);
            for (int i = 0; i < 60; i++) {
                double sx = rng.nextDouble() * JavaFX.W;
                double sy = rng.nextDouble() * (JavaFX.GRASS_Y - 30);
                double sr = 0.6 + rng.nextDouble() * 1.4;
                Circle star = new Circle(sx, sy, sr, Color.WHITE);
                star.setOpacity(0.45 + rng.nextDouble() * 0.55); // random opacity
                pane.getChildren().add(star);
            }
        }

        // day/night mountain colors
        Color backMtnColor  = isNight ? Color.web("#1e3a52") : Color.web("#4a6a8a");
        Color frontMtnColor = isNight ? Color.web("#0f2030") : Color.web("#3a5a6a");

        // back-left mountain
        Polygon mtL1 = new Polygon(
            0,   JavaFX.GRASS_Y,
            65,  JavaFX.GRASS_Y - 45,
            130, JavaFX.GRASS_Y - 118,
            202, JavaFX.GRASS_Y - 52,
            275, JavaFX.GRASS_Y);
        mtL1.setFill(backMtnColor);

        // front-left mountain
        Polygon mtL2 = new Polygon(
            55,  JavaFX.GRASS_Y,
            130, JavaFX.GRASS_Y - 68,
            205, JavaFX.GRASS_Y - 155,
            290, JavaFX.GRASS_Y - 75,
            375, JavaFX.GRASS_Y);
        mtL2.setFill(frontMtnColor);

        // front-right mountain
        Polygon mtR1 = new Polygon(
            525, JavaFX.GRASS_Y,
            600, JavaFX.GRASS_Y - 72,
            675, JavaFX.GRASS_Y - 152,
            757, JavaFX.GRASS_Y - 80,
            840, JavaFX.GRASS_Y);
        mtR1.setFill(frontMtnColor);

        // back-right mountain
        Polygon mtR2 = new Polygon(
            705, JavaFX.GRASS_Y,
            757, JavaFX.GRASS_Y - 55,
            810, JavaFX.GRASS_Y - 112,
            855, JavaFX.GRASS_Y - 40,
            900, JavaFX.GRASS_Y);
        mtR2.setFill(backMtnColor);

        backMtns[0] = mtL1; backMtns[1] = mtR2; // saved for click recolor
        pane.getChildren().addAll(mtL1, mtL2, mtR1, mtR2);

        // foreground, darker
        Polygon fgL1 = new Polygon(
            0,   JavaFX.GRASS_Y,
            80,  JavaFX.GRASS_Y - 38,
            160, JavaFX.GRASS_Y - 70,
            235, JavaFX.GRASS_Y - 50,
            310, JavaFX.GRASS_Y);
        fgL1.setFill(frontMtnColor);

        Polygon fgL2 = new Polygon(
            80,  JavaFX.GRASS_Y,
            155, JavaFX.GRASS_Y - 50,
            240, JavaFX.GRASS_Y - 90,
            330, JavaFX.GRASS_Y - 55,
            420, JavaFX.GRASS_Y);
        fgL2.setFill(frontMtnColor);

        Polygon fgR1 = new Polygon(
            480, JavaFX.GRASS_Y,
            565, JavaFX.GRASS_Y - 52,
            650, JavaFX.GRASS_Y - 88,
            715, JavaFX.GRASS_Y - 60,
            780, JavaFX.GRASS_Y);
        fgR1.setFill(frontMtnColor);

        Polygon fgR2 = new Polygon(
            650, JavaFX.GRASS_Y,
            720, JavaFX.GRASS_Y - 45,
            795, JavaFX.GRASS_Y - 72,
            850, JavaFX.GRASS_Y - 42,
            900, JavaFX.GRASS_Y);
        fgR2.setFill(frontMtnColor);

        // saved for click recolor
        frontMtns[0] = mtL2; frontMtns[1] = mtR1;
        frontMtns[2] = fgL1; frontMtns[3] = fgL2; frontMtns[4] = fgR1; frontMtns[5] = fgR2;
        pane.getChildren().addAll(fgL1, fgL2, fgR1, fgR2);

        return pane;
    }

    // wall, roof, door, windows
    public static Pane buildHouse(boolean isNight) {
        Pane pane = new Pane();
        pane.setPrefSize(JavaFX.W, JavaFX.H);
        pane.setPickOnBounds(false); // clicks pass through

        Color wall    = Color.web("#E8DCC8");
        Color roofCol = isNight ? Color.web("#3a2a1a") : Color.web("#8B4513"); // day/night colors
        Color winCol  = isNight ? Color.web("#ffe066") : Color.web("#a8d8f0");
        Color trim    = Color.web("#D4C4A8");
        Color stroke  = Color.web("#C0B0A0");

        rect(pane, 335, 388, 185, 107, wall, stroke, 1);

        Polygon roof = poly(320, 388, 540, 388, 427, 313);
        roof.setFill(roofCol);
        roof.setStroke(Color.web("#5a3010")); roof.setStrokeWidth(1.5);
        pane.getChildren().add(roof);

        rect(pane, 406, 437, 42, 58, Color.web("#6B3A2A"), Color.web("#4a2a1a"), 1);
        pane.getChildren().add(new Circle(443, 466, 3, Color.web("#D4AF37")));

        Rectangle lw = new Rectangle(344, 405, 54, 42); // saved for day/night swap
        lw.setFill(winCol); lw.setStroke(stroke); lw.setStrokeWidth(1.5);
        pane.getChildren().add(lw);
        leftWin = lw;
        line(pane, 371, 405, 371, 447, stroke, 1.5); // dividers and ledge
        line(pane, 344, 426, 398, 426, stroke, 1.5);
        rect(pane, 340, 446, 62, 5, trim, stroke, 0);

        Rectangle rw = new Rectangle(456, 405, 54, 42); // saved for day/night swap
        rw.setFill(winCol); rw.setStroke(stroke); rw.setStrokeWidth(1.5);
        pane.getChildren().add(rw);
        rightWin = rw;
        line(pane, 483, 405, 483, 447, stroke, 1.5); // dividers and ledge
        line(pane, 456, 426, 510, 426, stroke, 1.5);
        rect(pane, 452, 446, 62, 5, trim, stroke, 0);

        return pane;
    }

    // sky color by hour
    public static LinearGradient buildSkyGradient(int hour, int minute, boolean isNight) {
        double t = hour + minute / 60.0;
        if (isNight) {
            return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, // night sky
                new Stop(0, Color.web("#03030f")),
                new Stop(1, Color.web("#0d1b4b")));
        } else if (t < 6.8 || t > 17.2) {
            return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, // sunrise/sunset
                new Stop(0.0,  Color.web("#1a1050")),
                new Stop(0.38, Color.web("#e8632a")),
                new Stop(0.72, Color.web("#f4a346")),
                new Stop(1.0,  Color.web("#ffd57a")));
        } else {
            return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, // daytime
                new Stop(0, Color.web("#0d47a1")),
                new Stop(1, Color.web("#64B5F6")));
        }
    }

    // stroke skipped when sw=0
    private static void rect(Pane p, double x, double y, double w, double h,
                              Color fill, Color stroke, double sw) {
        Rectangle r = new Rectangle(x, y, w, h);
        r.setFill(fill);
        if (sw > 0) { r.setStroke(stroke); r.setStrokeWidth(sw); }
        p.getChildren().add(r);
    }

    // triangle polygon
    private static Polygon poly(double x1, double y1, double x2, double y2, double x3, double y3) {
        return new Polygon(x1, y1, x2, y2, x3, y3);
    }

    // line helper
    private static void line(Pane p, double x1, double y1, double x2, double y2,
                              Color stroke, double sw) {
        Line l = new Line(x1, y1, x2, y2);
        l.setStroke(stroke); l.setStrokeWidth(sw);
        p.getChildren().add(l);
    }
}
