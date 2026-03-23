import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import java.util.Random;
// This is the template for any weather Effect
public abstract class WeatherEffect {

    protected Pane pane;
    protected Timeline timeline;

    WeatherEffect(Pane pane) {
        this.pane = pane;
    }

    // template method — defines the steps of making a weather effect
    final void build() {
        createNodes();          // step 1: let subclass create shapes
        setupTimeline();        // step 2: let subclass make them move
        timeline.setCycleCount(Animation.INDEFINITE); // loop forever
        timeline.play();        // start the movement
    }

    // abstract step 1 — subclass must make its own shapes
    abstract void createNodes();

    // abstract step 2 — subclass must define movement/animation
    abstract void setupTimeline();

    void stop() {
        // null check protects subclasses that build() was never called on
        if (timeline != null) timeline.stop();
    }
}

// Rain Effect — implements the abstract steps for a rain effect
class RainEffect extends WeatherEffect {

    private static final int N = 90;    // number of raindrops
    private Line[] drops = new Line[N];  // array to hold each raindrop
    private double[] dropY = new double[N]; // y position for each drop
    private double[] dropX = new double[N]; // x position for each drop
    private double[] speed = new double[N]; // speed for each drop
    private double[] len   = new double[N]; // length of each drop

    // passes the shared pane up to the superclass constructor
    RainEffect(Pane pane) { super(pane); }

    // implements abstract step 1 — creates all rain drop nodes and places them on the pane
    @Override
    void createNodes() {
        Random rng = new Random(); // create random number generator

        for (int i = 0; i < N; i++) {
            dropX[i] = rng.nextDouble() * JavaFX.W; // random x position
            dropY[i] = rng.nextDouble() * JavaFX.H; // random y position
            speed[i] = 5 + rng.nextDouble() * 5;    // random speed
            len[i]   = 9 + rng.nextDouble() * 9;    // random length

            // create a line for the raindrop
            drops[i] = new Line(dropX[i], dropY[i], dropX[i] + 1.5, dropY[i] + len[i]);
            drops[i].setStroke(Color.web("#90c8f0", 0.42)); // light blue color
            drops[i].setStrokeWidth(1);                      // line thickness

            pane.getChildren().add(drops[i]); // add drop to the pane
        }
    }

    // implements abstract step 2 — defines how drop positions update each frame
    @Override
    void setupTimeline() {
        // make the drops move every 16 milliseconds (~60 fps)
        timeline = new Timeline(new KeyFrame(Duration.millis(16), e -> {
            for (int i = 0; i < N; i++) {
                dropY[i] += speed[i]; // move drop down by speed
                if (dropY[i] > JavaFX.H + 20) { // if drop goes off screen
                    dropY[i] = -20;               // put it back at top
                    dropX[i] = new Random().nextDouble() * JavaFX.W; // new random x
                }
                // update the line position
                drops[i].setStartX(dropX[i]);
                drops[i].setStartY(dropY[i]);
                drops[i].setEndX(dropX[i] + 1.5);
                drops[i].setEndY(dropY[i] + len[i]);
            }
        }));
    }
}

// Snow Effect
class SnowEffect extends WeatherEffect {

    private static final int N = 65;        // number of snowflakes
    private Circle[] flakes = new Circle[N]; // array to hold each snowflake
    private double[] fx = new double[N];    // x position
    private double[] fy = new double[N];    // y position
    private double[] speed = new double[N]; // fall speed
    private double[] drift = new double[N]; // horizontal sway
    private double[] phase = new double[N]; // phase for sway

    SnowEffect(Pane pane) { super(pane); }

    @Override
    void createNodes() {
        Random rng = new Random(); // random number generator

        for (int i = 0; i < N; i++) {
            fx[i] = rng.nextDouble() * JavaFX.W;       // random x start
            fy[i] = rng.nextDouble() * JavaFX.H;       // random y start
            speed[i] = 0.7 + rng.nextDouble() * 1.1;   // slow fall speed
            drift[i] = 0.3 + rng.nextDouble() * 0.5;   // sideways sway
            phase[i] = rng.nextDouble() * Math.PI * 2; // start phase

            double r = 1.8 + rng.nextDouble() * 2.5;  // size of snowflake
            flakes[i] = new Circle(fx[i], fy[i], r, Color.web("#e8f4ff", 0.85)); // white snowflake

            pane.getChildren().add(flakes[i]); // add to pane
        }
    }

    @Override
    void setupTimeline() {
        final long[] frame = {0}; // frame counter

        timeline = new Timeline(new KeyFrame(Duration.millis(33), e -> { // update ~30 fps
            frame[0]++; // increase frame count
            for (int i = 0; i < N; i++) {
                fy[i] += speed[i]; // move down
                fx[i] += Math.sin(phase[i] + frame[0] * 0.04) * drift[i]; // sway sideways

                if (fy[i] > JavaFX.H + 10) { // if off screen
                    fy[i] = -10; // back to top
                    fx[i] = new Random().nextDouble() * JavaFX.W; // new x
                }

                flakes[i].setCenterX(fx[i]); // update x position
                flakes[i].setCenterY(fy[i]); // update y position
            }
        }));
    }
}

// ------------------- Cloud Effect -------------------
class CloudEffect extends WeatherEffect {

    CloudEffect(Pane pane) { super(pane); }

    @Override
    void createNodes() {
        // dark rectangle to make sky look cloudy
        Rectangle overlay = new Rectangle(0, 0, JavaFX.W, JavaFX.H);
        overlay.setFill(Color.web("#404860", 0.28)); // semi-transparent gray
        pane.getChildren().add(overlay);

        // make five clouds
        double[][] clouds = {
                {130, 75, 95, 32}, {310, 52, 115, 36}, {510, 68, 105, 33},
                {695, 58, 88, 28}, {840, 88, 78, 26}
        };

        for (double[] c : clouds) {
            Ellipse e = new Ellipse(c[0], c[1], c[2], c[3]); // make cloud shape
            e.setFill(Color.web("#c8d0e0", 0.20));          // light gray
            pane.getChildren().add(e);                       // add to pane
        }
    }

    @Override
    void setupTimeline() {
        timeline = new Timeline(); // no movement, but needed by template
    }
}
