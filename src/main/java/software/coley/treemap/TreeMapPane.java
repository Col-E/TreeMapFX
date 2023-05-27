package software.coley.treemap;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import software.coley.treemap.squaring.Rectangle;
import software.coley.treemap.squaring.Squarify;

import javax.annotation.Nonnull;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/**
 * Tree map pane to render weighted values in different sized rectangles.
 * <p>
 * To add content to the pane, you need to provide two things:
 * <ol>
 *     <li>{@link #sizeFunctionProperty() A conversion of 'T' to double} for calculating 'T' items "value"</li>
 *     <li>{@link #nodeFactoryProperty() A conversion of 'T' to nodes} for visualizing 'T' items</li>
 * </ol>
 * The nodes supplied by the node factory are sized according to their "value" relative to all other items.
 * More valuable items are given larger nodes.
 * <p>
 * Should you wish to change the {@link #sizeFunctionProperty()} or {@link #nodeFactoryProperty()} you must do so
 * on {@link Platform#runLater(Runnable) the FX thread}.
 *
 * @param <T>
 * 		Type of content.
 *
 * @author Matt Coley
 */
public class TreeMapPane<T> extends Pane {
	// Managed properties
	protected final MapProperty<T, Node> valueToNode = new SimpleMapProperty<>();

	// Configurable properties
	protected final ListProperty<T> valueList = new SimpleListProperty<>(FXCollections.observableArrayList());
	protected final ObjectProperty<ToDoubleFunction<T>> sizeFunction;
	protected final ObjectProperty<Function<T, Node>> nodeFactory;

	/**
	 * New tree-map pane.
	 *
	 * @param sizeFunction
	 * 		Size computation function for {@code T} values.
	 * @param nodeFactory
	 * 		Node providing function for {@code T} values.
	 */
	public TreeMapPane(@Nonnull ToDoubleFunction<T> sizeFunction,
					   @Nonnull Function<T, Node> nodeFactory) {

		this.sizeFunction = new SimpleObjectProperty<>(sizeFunction);
		this.nodeFactory = new SimpleObjectProperty<>(nodeFactory);

		// Create a dummy binding that will fire off a change when any given property is changed.
		ObjectBinding<?> multiSourceBindings = Bindings.createObjectBinding(
				() -> this /* Dummy value must not be null */,
				getTreeMapProperties());
		setupChangeBindings(multiSourceBindings);

		// Disabled to prevent pixel snapping flickering.
		setSnapToPixel(false);
	}

	/**
	 * Configures UI updates through bindings.
	 *
	 * @param multiSourceBindings
	 * 		Binding that wraps properties of this pane.
	 * 		Any change in one property is forwarded to this wrapper property.
	 */
	protected void setupChangeBindings(@Nonnull ObjectBinding<?> multiSourceBindings) {
		// When any of the properties change, recompute the nodes displayed.
		valueToNode.bind(multiSourceBindings.map(unused -> {
			Function<T, Node> valueFunction = nodeFactory.get();
			Function<T, T> keyFunction = Function.identity();
			BinaryOperator<Node> mergeFunction = (a, b) -> {
				throw new IllegalStateException();
			};
			Map<T, Node> map = valueList.stream()
					.collect(Collectors.toMap(keyFunction,
							valueFunction,
							mergeFunction,
							IdentityHashMap::new));
			return FXCollections.observableMap(map);
		}));

		// Map updates should update the children present on the pane.
		valueToNode.addListener((MapChangeListener<T, Node>) change -> {
			ObservableList<Node> children = getChildren();
			if (change.wasAdded()) children.add(change.getValueAdded());
			if (change.wasRemoved()) children.remove(change.getValueRemoved());
		});
	}

	/**
	 * @return Array of properties to consider for triggering recalculation of the tree-map display.
	 */
	@Nonnull
	protected Observable[] getTreeMapProperties() {
		return new Observable[]{sizeFunction, nodeFactory, valueList};
	}

	/**
	 * @return Property representing the items to display as a tree-map.
	 */
	@Nonnull
	public ListProperty<T> valueListProperty() {
		return valueList;
	}

	/**
	 * @return Read-only managed map of value instances to their associated {@link Node} representations.
	 */
	@Nonnull
	public ObservableMap<T, Node> valueToNodeProperty() {
		return FXCollections.unmodifiableObservableMap(valueToNode);
	}

	/**
	 * @return Property representing the current {@code T} to {@link Node} mapping function.
	 */
	@Nonnull
	public ObjectProperty<Function<T, Node>> nodeFactoryProperty() {
		return nodeFactory;
	}

	/**
	 * @return Property representing the current {@code T} to {@code double} size mapping function.
	 */
	@Nonnull
	public ObjectProperty<ToDoubleFunction<T>> sizeFunctionProperty() {
		return sizeFunction;
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
		return new Squarify<>(valueListProperty().get(), sizeFunctionProperty().get(),
				canvasX, canvasY, canvasWidth, canvasHeight)
				.getRectangles();
	}

	@Override
	protected void layoutChildren() {
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
		Map<T, Node> valueToNode = valueToNodeProperty();
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