package software.coley.treemap.content;

import javafx.beans.property.ListProperty;
import javafx.beans.value.ObservableValue;
import software.coley.treemap.TreeMapPane;

import javax.annotation.Nonnull;

/**
 * Simple implementation of {@link HierarchicalTreeContent}.
 *
 * @author Matt Coley
 */
public class SimpleHierarchicalTreeContent implements HierarchicalTreeContent {
	private final ListProperty<TreeContent> children;
	private final ObservableValue<Double> weight;
	private final ObservableValue<TreeMapPane<TreeContent>> node;

	/**
	 * @param children
	 * 		List of child content items.
	 */
	public SimpleHierarchicalTreeContent(@Nonnull ListProperty<TreeContent> children) {
		this.children = children;

		// Simple children --> valueWeight() property mapping.
		weight = children.map(list ->
				list.stream().mapToDouble(TreeContent::getValueWeight).sum());

		// Reuse the same tree-map pane, but rebind contents when the child list updates.
		TreeMapPane<TreeContent> treeMapPane = TreeMapPane.forTreeContent();
		node = children().map(list -> {
			treeMapPane.valueListProperty().bindContent(list);
			return treeMapPane;
		});
	}

	@Nonnull
	@Override
	public ListProperty<TreeContent> children() {
		return children;
	}

	@Override
	public double getValueWeight() {
		return weight.getValue();
	}

	@Nonnull
	@Override
	public TreeMapPane<TreeContent> getNode() {
		return node.getValue();
	}
}
