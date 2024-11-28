package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    private static final String CONFIG_FILE = "config.yml";
    private final DrawNumber model;
    private final List<DrawNumberView> views;

    /**
     * @param views
     *              the views to attach
     */
    public DrawNumberApp(final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view : views) {
            view.setObserver(this);
            view.start();
        }

        final Configuration.Builder confBuild = new Configuration.Builder();
        try (final var in = new BufferedReader(
                new InputStreamReader(ClassLoader.getSystemResourceAsStream(CONFIG_FILE)))) {
            for (var line = in.readLine(); line != null; line = in.readLine()) {
                final String[] setting = line.split(":\s");
                // System.out.println(Arrays.toString(setting));
                final int value = Integer.valueOf(setting[1]);
                if (setting[0].equals(Configuration.Type.MIN.getName())) {
                    confBuild.setMin(value);
                } else if (setting[0].equals(Configuration.Type.MAX.getName())) {
                    confBuild.setMax(value);
                } else if (setting[0].equals(Configuration.Type.ATTEMPTS.getName())) {
                    confBuild.setAttempts(value);
                } else {
                    showError("No matching setting for "
                            + setting[0] + "in the configuration file");
                }
            }
        } catch (NumberFormatException | IOException e) {
            showError("An error occurrend: " + e.getMessage());
        }

        final Configuration conf = confBuild.build();
        if (conf.isConsistent()) {
            this.model = new DrawNumberImpl(conf);
        } else {
            showError("The selected configuration is not consistent. "
                    + "Starting the application with the default configuration...");
            this.model = new DrawNumberImpl(new Configuration.Builder().build());
        }
    }

    private void showError(final String error) {
        views.forEach(v -> v.displayError(error));
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view : views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view : views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * @param args
     *             ignored
     * @throws FileNotFoundException
     */
    public static void main(final String... args) throws FileNotFoundException {
        new DrawNumberApp(
                new DrawNumberViewImpl(),
                new DrawNumberViewImpl(),
                new PrintStreamView(System.out),
                new PrintStreamView("logger.txt"));
    }

}
