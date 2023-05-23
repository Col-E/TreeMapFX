package software.coley.treemap;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import software.coley.treemap.squaring.Rectangle;
import software.coley.treemap.squaring.Squarify;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * Tree map pane to render weighted values in different sized rectangles.
 *
 * @param <T>
 * 		Type of content.
 *
 * @author Matt Coley
 */
public class TreeMapPane<T> extends Pane {
	protected final ObservableList<T> valueList = FXCollections.observableArrayList();
	protected final Map<T, Node> valueToNode = new IdentityHashMap<>();
	protected final ObjectProperty<ToDoubleFunction<T>> sizeFunctionProperty;
	protected final ObjectProperty<Function<T, Node>> nodeFunctionProperty;
	protected final AtomicBoolean layoutLock = new AtomicBoolean();

	/**
	 * New tree-map pane.
	 *
	 * @param sizeFunction
	 * 		Size computation function for {@code T} values.
	 * @param nodeFunction
	 * 		Node providing function for {@code T} values.
	 */
	public TreeMapPane(@Nonnull ToDoubleFunction<T> sizeFunction,
					   @Nonnull Function<T, Node> nodeFunction) {
		this.sizeFunctionProperty = new SimpleObjectProperty<>(sizeFunction);
		this.nodeFunctionProperty = new SimpleObjectProperty<>(nodeFunction);

		// Disabled to prevent pixel snapping flickering.
		setSnapToPixel(false);

		// Add listeners to create or remove nodes on/from the pane.
		setupChildrenUpdates();

		// Clear data when properties change
		setupPropertyListeners();
	}

	/**
	 * @return Property representing the current {@code T} to {@link Node} mapping function.
	 */
	@Nonnull
	public ObjectProperty<Function<T, Node>> nodeFunctionProperty() {
		return nodeFunctionProperty;
	}

	/**
	 * @return Property representing the current {@code T} to {@code double} size mapping function.
	 */
	@Nonnull
	public ObjectProperty<ToDoubleFunction<T>> sizeFunctionProperty() {
		return sizeFunctionProperty;
	}

	/**
	 * Configures a change listener on the backing value list that adds/removed children to/from the pane.
	 */
	protected void setupChildrenUpdates() {
		valueList.addListener((ListChangeListener<T>) change -> {
			// Extract added & removed children from the changes
			List<Node> addedChildren = null;
			List<Node> removedChildren = null;
			Function<T, Node> nodeFunction = nodeFunctionProperty.get();
			while (change.next()) {
				for (T value : change.getAddedSubList()) {
					Node node = nodeFunction.apply(value);
					if (node == null)
						throw new IllegalStateException("Node function must not provide null values, " +
								"null provided for: " + value);
					valueToNode.put(value, node);
					if (addedChildren == null)
						addedChildren = new ArrayList<>();
					addedChildren.add(node);
				}
				for (T value : change.getRemoved()) {
					Node node = valueToNode.remove(value);
					if (node == null)
						throw new IllegalStateException("No associated node for the value: " + value);
					if (removedChildren == null)
						removedChildren = new ArrayList<>();
					removedChildren.add(node);
				}
			}

			// Update children
			ObservableList<Node> children = getChildren();
			if (removedChildren != null) children.removeAll(removedChildren);
			if (addedChildren != null) children.addAll(addedChildren);
		});
	}

	/**
	 * Configures change listeners on properties to update appropriate internals and children layouts.
	 */
	protected void setupPropertyListeners() {
		// Handle replacing the node cache when the node function is replaced.
		nodeFunctionProperty.addListener((ob, old, cur) -> {
			// Lock layouts until we've updated the value-to-node map.
			// We don't want a layout to occur while a value has no associated node.
			layoutLock.set(true);

			// Create new children and update the value-to-node map.
			valueToNode.clear();
			for (T value : valueList)
				valueToNode.put(value, cur.apply(value));

			// We can unlock layouts now.
			layoutLock.set(false);

			// Schedule update to children list, replace old nodes with newly made ones.
			Platform.runLater(() -> {
				ObservableList<Node> children = getChildren();
				children.clear();
				children.addAll(valueToNode.values());
			});

			// Request layout to display the new children.
			requestLayout();
		});

		// Request a layout refresh when the size function changes.
		sizeFunctionProperty.addListener((ob, old, cur) -> {
			requestLayout();
		});
	}

	/**
	 * @param value
	 * 		Value to add.
	 *
	 * @return {@code true} when added.
	 */
	public boolean addChild(@Nonnull T value) {
		return valueList.add(value);
	}

	/**
	 * @param values
	 * 		Values to add.
	 *
	 * @return {@code true} when added.
	 */
	@SafeVarargs
	public final boolean addChildren(@Nonnull T... values) {
		return valueList.addAll(values);
	}

	/**
	 * @param values
	 * 		Values to add.
	 *
	 * @return {@code true} when added.
	 */
	public boolean addChildren(@Nonnull Collection<T> values) {
		return valueList.addAll(values);
	}

	/**
	 * @param value
	 * 		Value to remove.
	 *
	 * @return {@code true} when removed.
	 */
	public boolean removeChild(@Nonnull T value) {
		return valueList.remove(value);
	}

	/**
	 * @param values
	 * 		Values to remove.
	 *
	 * @return {@code true} when removed.
	 */
	@SafeVarargs
	public final boolean removeChildren(@Nonnull T... values) {
		return valueList.removeAll(values);
	}

	/**
	 * @param values
	 * 		Values to remove.
	 *
	 * @return {@code true} when removed.
	 */
	public boolean removeChildren(@Nonnull Collection<T> values) {
		return valueList.removeAll(values);
	}

	/**
	 * @param canvasX
	 * 		X coordinate of rectangle to fill.
	 * @param canvasY
	 * 		Y coordinate of rectangle to fill.
	 * @param canvasWidth
	 * 		Width of the rectangle to fill.
	 * @param canvasHeight
	 * 		Height of the rectangle to fill.
	 *
	 * @return Rectangles of the {@link #valueList values} filling the given space.
	 */
	@Nonnull
	protected List<Rectangle<T>> computeRectangles(double canvasX, double canvasY,
												   double canvasWidth, double canvasHeight) {
		ToDoubleFunction<T> sizeFunction = sizeFunctionProperty.get();
		return new Squarify<>(valueList, sizeFunction, canvasX, canvasY, canvasWidth, canvasHeight)
				.getRectangles();
	}

	@Override
	protected void layoutChildren() {
		// Skip if layout lock is set.
		if (layoutLock.get())
			return;

		// Get dimensions to pass to the squarify algorithm from the current pane dimensions, minus insets.
		Insets insets = getInsets();
		double canvasX = insets.getLeft();
		double canvasY = insets.getTop();
		double canvasWidth = getWidth() - canvasX - insets.getRight();
		double canvasHeight = getHeight() - canvasY - insets.getBottom();

		// Skip if width is bogus.
		if (canvasWidth <= 0 || canvasHeight <= 0)
			return;

		// Map the values to rectangles.
		List<Rectangle<T>> rectangles = computeRectangles(canvasX, canvasY, canvasWidth, canvasHeight);

		// Get the associated node for each rectangle's value and update its position/size.
		for (Rectangle<T> rectangle : rectangles) {
			T value = rectangle.data();
			Node node = valueToNode.get(value);
			if (node == null)
				throw new IllegalStateException("Value " + value + " had no mapping to a node");
			layoutChild(rectangle, node);
		}
	}

	/**
	 * @param rectangle
	 * 		Space to fill.
	 * @param node
	 * 		Node to assign position/size of to match the rectangle.
	 */
	protected void layoutChild(@Nonnull Rectangle<T> rectangle, @Nonnull Node node) {
		double x = rectangle.x();
		double y = rectangle.y();
		double w = rectangle.width();
		double h = rectangle.height();
		node.resize(w, h);
		positionInArea(node, x, y,
				w, h, 0,
				Insets.EMPTY,
				HPos.CENTER,
				VPos.CENTER, isSnapToPixel());
	}
}
