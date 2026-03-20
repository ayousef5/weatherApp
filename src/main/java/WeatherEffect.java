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

// template method pattern — abstract class defines the skeleton, subclasses fill in the steps
public abstract class WeatherEffect {

    // shared pane that all subclasses add their nodes to
    protected Pane pane;
    // shared timeline that all subclasses assign in setupTimeline
    protected Timeline timeline;

    // constructor stores the pane so subclasses can reach it without arguments
    WeatherEffect(Pane pane) {
        this.pane = pane;
    }

    // template method — defines the fixed algorithm that every subclass follows
    // subclasses cannot override this; the order of steps is locked here
    final void build() {
        // step 1: abstract step — subclass populates the pane with nodes
        createNodes();
        // step 2: abstract step — subclass wires up the timeline
        setupTimeline();
        // steps 3 and 4 are invariant — the superclass owns them
        timeline.setCycleCount(Animation.INDEFINITE); // loop forever
        timeline.play(); // start animation after both steps are done
    }

    // abstract step 1 — each subclass must define what nodes its effect needs
    abstract void createNodes();

    // abstract step 2 — each subclass must define how its nodes animate over time
    abstract void setupTimeline();

    // shared cleanup available to all subclasses — superclass provides a safe default
    void stop() {
        // null check protects subclasses that build() was never called on
        if (timeline != null) timeline.stop();
    }
}

// concrete subclass — implements the abstract steps for a rain effect
class RainEffect extends WeatherEffect {

    // number of drops in the effect
    private static final int N = 90;
    // node array — one Line per drop, managed across both abstract steps
    private Line[] drops = new Line[N];
    // per-drop state arrays shared between createNodes and the timeline closure
    private double[] dropY = new double[N];
    private double[] dropX = new double[N];
    private double[] speed = new double[N];
    private double[] len   = new double[N];

    // passes the shared pane up to the superclass constructor
    RainEffect(Pane pane) { super(pane); }

    // implements abstract step 1 — creates all rain drop nodes and places them on the pane
    @Override
    void createNodes() {
        Random rng = new Random(); // random start positions so drops are spread across the screen
        for (int i = 0; i < N; i++) {
            // randomize position so drops don't all start at the same spot
            dropX[i] = rng.nextDouble() * JavaFX.W;
            dropY[i] = rng.nextDouble() * JavaFX.H;
            // randomize speed and length so the rain feels natural
            speed[i] = 5 + rng.nextDouble() * 5;
            len[i]   = 9 + rng.nextDouble() * 9;
            // angled drop — x+1.5 gives a slight diagonal to look like real rain
            drops[i] = new Line(dropX[i], dropY[i], dropX[i] + 1.5, dropY[i] + len[i]);
            // semi-transparent blue matches a rainy sky tone
            drops[i].setStroke(Color.web("#90c8f0", 0.42));
            drops[i].setStrokeWidth(1);
            // adding to pane is the responsibility of this abstract step
            pane.getChildren().add(drops[i]);
        }
    }

    // implements abstract step 2 — defines how drop positions update each frame
    @Override
    void setupTimeline() {
        // ~60fps keyframe — tight interval keeps rain motion smooth
        timeline = new Timeline(new KeyFrame(Duration.millis(16), e -> {
            for (int i = 0; i < N; i++) {
                // advance each drop downward by its speed
                dropY[i] += speed[i];
                // wrap off the bottom — resets to top with a new random x
                if (dropY[i] > JavaFX.H + 20) { dropY[i] = -20; dropX[i] = new Random().nextDouble() * JavaFX.W; }
                // update the Line node to match the new position
                drops[i].setStartX(dropX[i]); drops[i].setStartY(dropY[i]);
                drops[i].setEndX(dropX[i] + 1.5); drops[i].setEndY(dropY[i] + len[i]);
            }
        }));
    }
}

// concrete subclass — implements the abstract steps for a snow effect
class SnowEffect extends WeatherEffect {

    // fewer flakes than rain — snow is lighter and slower
    private static final int N = 65;
    // node array — one Circle per flake
    private Circle[] flakes = new Circle[N];
    // per-flake position arrays shared between the two abstract steps
    private double[] fx = new double[N], fy = new double[N];
    // speed controls fall rate; drift and phase drive the horizontal sway
    private double[] speed = new double[N], drift = new double[N], phase = new double[N];

    // passes the shared pane up to the superclass constructor
    SnowEffect(Pane pane) { super(pane); }

    // implements abstract step 1 — creates all snowflake nodes and places them on the pane
    @Override
    void createNodes() {
        Random rng = new Random(); // random positions and sizes
        for (int i = 0; i < N; i++) {
            // spread flakes across the full canvas at startup
            fx[i] = rng.nextDouble() * JavaFX.W;
            fy[i] = rng.nextDouble() * JavaFX.H;
            // slower fall speed than rain to differentiate the two effects visually
            speed[i] = 0.7 + rng.nextDouble() * 1.1;
            // small drift magnitude keeps sway subtle
            drift[i] = 0.3 + rng.nextDouble() * 0.5;
            // unique phase per flake so they don't all sway in sync
            phase[i] = rng.nextDouble() * Math.PI * 2; // offset sway phases
            // vary radius slightly for a more natural look
            double r = 1.8 + rng.nextDouble() * 2.5;
            // soft white flake with high opacity
            flakes[i] = new Circle(fx[i], fy[i], r, Color.web("#e8f4ff", 0.85));
            // adding to pane is the responsibility of this abstract step
            pane.getChildren().add(flakes[i]);
        }
    }

    // implements abstract step 2 — defines how flake positions update each frame
    @Override
    void setupTimeline() {
        // ~30fps — slower tick rate reflects snow's gentle pace
        final long[] frame = {0}; // frame counter used to advance the sine wave
        timeline = new Timeline(new KeyFrame(Duration.millis(33), e -> {
            frame[0]++; // increment so the sine argument advances each tick
            for (int i = 0; i < N; i++) {
                // fall straight down at per-flake speed
                fy[i] += speed[i];
                // horizontal sway driven by a sine wave — gives a drifting feel
                fx[i] += Math.sin(phase[i] + frame[0] * 0.04) * drift[i]; // sine drift
                // wrap off the bottom — resets to top with a new random x
                if (fy[i] > JavaFX.H + 10) { fy[i] = -10; fx[i] = new Random().nextDouble() * JavaFX.W; }
                // update Circle node to match new position
                flakes[i].setCenterX(fx[i]);
                flakes[i].setCenterY(fy[i]);
            }
        }));
    }
}

// concrete subclass — implements the abstract steps for a cloudy/overcast effect
class CloudEffect extends WeatherEffect {

    // passes the shared pane up to the superclass constructor
    CloudEffect(Pane pane) { super(pane); }

    // implements abstract step 1 — adds a dark overlay and static cloud ellipses
    @Override
    void createNodes() {
        // full-canvas overlay dims the background sky to simulate overcast light
        Rectangle overlay = new Rectangle(0, 0, JavaFX.W, JavaFX.H);
        // low opacity so the background still shows through
        overlay.setFill(Color.web("#404860", 0.28));
        pane.getChildren().add(overlay);
        double[][] clouds = { // cx, cy, rx, ry per cloud — five static clouds across the sky
            {130, 75,  95, 32}, {310, 52, 115, 36}, {510, 68, 105, 33},
            {695, 58,  88, 28}, {840, 88,  78, 26}
        };
        for (double[] c : clouds) {
            // each cloud is a semi-transparent ellipse placed at a fixed position
            Ellipse e = new Ellipse(c[0], c[1], c[2], c[3]);
            // light grey at low opacity blends with the dimmed sky
            e.setFill(Color.web("#c8d0e0", 0.20));
            pane.getChildren().add(e);
        }
    }

    // implements abstract step 2 — clouds are static so no animation is needed
    // the template method still requires a non-null timeline, so an empty one is assigned
    @Override
    void setupTimeline() {
        timeline = new Timeline(); // no animation needed — satisfies the template method contract
    }
}
