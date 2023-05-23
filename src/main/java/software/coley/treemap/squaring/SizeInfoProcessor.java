package software.coley.treemap.squaring;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Processes some list of input instances, providing normalized sizes for all inputs in the wrapper type {@link SizeInfo}.
 *
 * @param <T>
 * 		Data value type.
 *
 * @author Matt Coley
 */
public class SizeInfoProcessor<T> {
	private final List<SizeInfo<T>> infos = new ArrayList<>();
	private final double totalSize;
	private final double totalArea;

	/**
	 * Processes the given values, yielding {@link #getSizeInfos() size information wrappers}.
	 *
	 * @param values
	 * 		Input data to process.
	 * @param sizeFunction
	 * 		Function to convert {@code T} values to sizes.
	 * @param width
	 * 		Target canvas width.
	 * @param height
	 * 		Target canvas height.
	 */
	public SizeInfoProcessor(@Nonnull List<T> values, @Nonnull ToDoubleFunction<T> sizeFunction,
							 double width, double height) {
		// Map values to info wrappers, providing initial sizes and sorting with larger items appearing first.
		for (T value : values)
			infos.add(new SizeInfo<>(value, sizeFunction.applyAsDouble(value), this::getOccupiedSpaceRatio));
		infos.sort(Comparator.comparingDouble(SizeInfo::getSize));
		Collections.reverse(infos);

		// Compute total size occupied by the values, and the size of the canvas.
		// Used by the occupied space ratio, which is needed for normalization.
		totalSize = infos.stream().mapToDouble(SizeInfo::getSize).sum();
		totalArea = width * height;

		// Normalize the values sizes to canvas area.
		for (SizeInfo<T> datum : infos)
			datum.normalize();
	}

	/**
	 * @return Wrappers of the original input values with additional size information.
	 */
	@Nonnull
	public List<SizeInfo<T>> getSizeInfos() {
		return this.infos;
	}

	/**
	 * @return Ratio of area covered by the input values to the total size of the canvas.
	 */
	private double getOccupiedSpaceRatio() {
		return totalArea / totalSize;
	}
}