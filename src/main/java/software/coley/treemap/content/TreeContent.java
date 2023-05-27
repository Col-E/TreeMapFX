package software.coley.treemap.content;

import javafx.scene.Node;
import software.coley.treemap.TreeMapPane;

import javax.annotation.Nonnull;

/**
 * Helper model type to represent items to input into a {@link TreeMapPane}.
 * Primarily useful if you want to cache the {@link #getValueWeight() weight} and {@link #getNode() node} values.
 *
 * @author Matt Coley
 * @see SimpleHierarchicalTreeContent Subtype for representing hierarchical data.
 */
public interface TreeContent {
	/**
	 * @return Weight of 'this' value.
	 */
	double getValueWeight();

	/**
	 * @return Visualization of 'this' value.
	 */
	@Nonnull
	Node getNode();
}
