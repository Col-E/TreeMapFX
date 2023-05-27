package software.coley.treemap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App extends Application {
	private static final int WIDTH = 300;
	private static final int HEIGHT = 200;
	private static final Random r = new Random(1);

	@Override
	public void start(Stage stage) {
		// The tree-map pane can represent any 'T' value, so long as you provide two things:
		//  1. ToDoubleFunction<T> to compute the 'size' or 'weight' of values
		//  2. Function<T, Node> to create Node representations of 'T' values
		//
		// In this example we'll represent a list of strings (of integers)
		List<String> values = Stream.of(1, 1, 1, 1, 2, 2, 2, 2, 2, 3,
						3, 3, 3, 3, 3, 3, 3, 3, 3, 5, 5, 5, 7, 7, 14, 16, 30, 80)
				.map(String::valueOf)
				.collect(Collectors.toList());

		// The 'size' conversion is a simple 'parseInt' on the string.
		// The Node mapping creates a label that shows the number, and a random background color to differentiate boxes
		TreeMapPane<String> pane = new TreeMapPane<>(Integer::parseInt, text -> {
			Label label = new Label(text);
			label.setStyle("-fx-background-color: " + String.format("#%06x", r.nextInt(0xffffff + 1)) + "; " +
					"-fx-background-radius: 0; -fx-border-width: 0.5; -fx-border-color: black;");
			label.setAlignment(Pos.CENTER);
			return label;
		});

		// Add the values to the tree-map
		pane.valueListProperty().addAll(values);

		// Show that properties can be changed on the fly without issues
		demoPropertyChangesOnInterval(pane);

		// Create basic layout and show it
		BorderPane root = new BorderPane(pane);
		root.setStyle("-fx-background-color: black");
		Scene scene = new Scene(root, WIDTH, HEIGHT);
		stage.setScene(scene);
		stage.show();
	}

	private static void demoPropertyChangesOnInterval(TreeMapPane<String> pane) {
		ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
		int interval = 5;
		int seconds = interval;
		runScheduledFx(pool, seconds += interval, () -> {
			pane.nodeFactoryProperty().set(text -> {
				Label label = new Label(text);
				label.setStyle("-fx-background-color: " + String.format("#%06x", r.nextInt(0xffffff + 1)) + "; " +
						"-fx-background-radius: 20; -fx-border-width: 0.5; -fx-border-color: black;");
				label.setAlignment(Pos.CENTER);
				return label;
			});
		});
		runScheduledFx(pool, seconds += interval, () -> {
			pane.nodeFactoryProperty().set(text -> {
				Label label = new Label(text);
				label.setStyle("-fx-background-color: " + String.format("#%06x", r.nextInt(0xffffff + 1)) + "; " +
						"-fx-background-radius: 0; -fx-border-width: 3; -fx-border-color: black;");
				label.setAlignment(Pos.CENTER);
				return label;
			});
		});
		runScheduledFx(pool, seconds += interval, () -> {
			pane.sizeFunctionProperty().set(text -> Integer.parseInt(text) % 40);
		});
		runScheduledFx(pool, seconds += interval, () -> {
			pane.sizeFunctionProperty().set(text -> Integer.parseInt(text) % 15);
		});
	}

	private static void runScheduledFx(ScheduledExecutorService service, int i, Runnable r) {
		service.schedule(() -> {
			Platform.runLater(r);
		}, i, TimeUnit.SECONDS);
	}
}
