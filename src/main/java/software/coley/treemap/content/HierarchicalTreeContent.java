package software.coley.treemap.content;

import javafx.beans.property.ListProperty;
import software.coley.treemap.TreeMapPane;

import javax.annotation.Nonnull;

/**
 * Helper model type to represent hierarchical items to input into a {@link TreeMapPane}.
 *
 * @author Matt Coley
 */
public interface HierarchicalTreeContent extends TreeContent {
	/**
	 * @return Child tree content items.
	 */
	@Nonnull
	ListProperty<TreeContent> children();

	/**
	 * @return Visualization of all child values.
	 */
	@Nonnull
	@Override
	TreeMapPane<TreeContent> getNode();
}
