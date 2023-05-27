package software.coley.treemap;

import javafx.application.Application;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import software.coley.treemap.content.SimpleHierarchicalTreeContent;
import software.coley.treemap.content.TreeContent;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static javafx.collections.FXCollections.observableArrayList;

public class DemoHierarchicalContent extends Application {
	private static final int WIDTH = 300;
	private static final int HEIGHT = 200;

	@Override
	public void start(Stage stage) {
		Path src = Paths.get("src");

		// Create two tree-map panes, one with flat modeling and the other with hierarchical
		TreeMapPane<TreeContent> paneHier = TreeMapPane.forTreeContent();
		TreeMapPane<TreeContent> paneFlat = TreeMapPane.forTreeContent();
		paneHier.valueListProperty().addAll(hierarchyFromPath(src));
		paneFlat.valueListProperty().addAll(flatFromPath(src));

		// Create basic layout and show it
		SplitPane root = (new SplitPane(paneHier, paneFlat));
		root.setStyle("-fx-background-color: black");
		Scene scene = new Scene(root, WIDTH, HEIGHT);
		stage.setScene(scene);
		stage.show();
	}

	@Nonnull
	private List<TreeContent> flatFromPath(@Nonnull Path path) {
		List<TreeContent> values = new ArrayList<>();
		try {
			Files.walkFileTree(path, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					long size = attrs.size();
					Label label = createLabel(file.getFileName().toString(), file.getParent(), size);
					values.add(new TreeContent() {
						@Override
						public double getValueWeight() {
							return size;
						}

						@Nonnull
						@Override
						public Node getNode() {
							return label;
						}
					});
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		return values;
	}

	@Nonnull
	private List<TreeContent> hierarchyFromPath(@Nonnull Path path) {
		try {
			if (Files.isDirectory(path)) {
				ListProperty<TreeContent> children = new SimpleListProperty<>(observableArrayList(Files.list(path)
						.flatMap(p -> hierarchyFromPath(p).stream())
						.toList()));
				return Collections.singletonList(new SimpleHierarchicalTreeContent(children));
			} else {
				long size = Files.size(path);
				Label label = createLabel(path.getFileName().toString(), path.getParent(), size);
				return Collections.singletonList(new TreeContent() {
					@Override
					public double getValueWeight() {
						return size;
					}

					@Nonnull
					@Override
					public Node getNode() {
						return label;
					}
				});
			}
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Nonnull
	private Label createLabel(@Nonnull String fileName, @Nonnull Path fileDir, long size) {
		// Create a label with the file name and size.
		//  - Background color changes based on directory
		//  - Background opacity changes based on file size (bigger = more opaque)
		Label label = new Label(fileName + "\n" + humanReadableByteCountSI(size));
		double brightness = 0.9;
		double hue = new Random(-fileDir.hashCode()).nextGaussian() * 360;
		double saturation = 0.7;
		Color hsb = Color.hsb(hue, saturation, brightness);
		label.setStyle("-fx-background-color: " + toRGBCode(hsb) + "; " +
				"-fx-background-radius: 0; -fx-border-width: 0.5; -fx-border-color: black;");
		label.setAlignment(Pos.CENTER);
		return label;
	}

	@Nonnull
	private static String toRGBCode(@Nonnull Color color) {
		return String.format("#%02X%02X%02X",
				(int) (color.getRed() * 255),
				(int) (color.getGreen() * 255),
				(int) (color.getBlue() * 255));
	}

	@Nonnull
	private static String humanReadableByteCountSI(long bytes) {
		if (-1000 < bytes && bytes < 1000)
			return bytes + " B";
		CharacterIterator ci = new StringCharacterIterator("kMGTPE");
		while (bytes <= -999_950 || bytes >= 999_950) {
			bytes /= 1000;
			ci.next();
		}
		return String.format("%.1f %cB", bytes / 1000.0, ci.current());
	}
}
