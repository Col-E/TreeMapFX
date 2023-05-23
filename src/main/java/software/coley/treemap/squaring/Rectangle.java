package software.coley.treemap.squaring;

/**
 * Basic rectangle with associated data.
 *
 * @param data
 * 		Associated data.
 * 		<br>
 * 		While {@code null} may be passed for temporary usage by {@link Squarify},
 * 		it is never {@code null} in practice.
 * @param x
 * 		X coordinate.
 * @param y
 * 		Y coordinate.
 * @param width
 * 		Width.
 * @param height
 * 		Height.
 * @param <T>
 * 		Associated data type.
 *
 * @author Matt coley
 */
public record Rectangle<T>(T data, double x, double y, double width, double height) {
}