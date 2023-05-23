package software.coley.treemap.squaring;

import javax.annotation.Nonnull;
import java.util.function.DoubleSupplier;

/**
 * Wrapper of the original value, along with its computed 'size' and a variant normalized to the size of the canvas.
 *
 * @author Matt Coley
 * @see SizeInfoProcessor Processes a {@code List<T>} into {@code List<DataPoint<T>>}.
 */
public class SizeInfo<T> {
	private final DoubleSupplier ratioProvider;
	private final double size;
	private double normalizedValue = -1;
	private final T value;

	/**
	 * Creates a new size info wrapper of a value and its size.
	 *
	 * @param value
	 * 		Wrapped data value.
	 * @param size
	 * 		Original size of the value.
	 * @param occupiedSpaceRatio
	 * 		Supplies the ratio of space occupied by all values
	 * 		<i>(Passed to the {@link SizeInfoProcessor})</i> to the total size of the canvas.
	 */
	public SizeInfo(@Nonnull T value, double size, @Nonnull DoubleSupplier occupiedSpaceRatio) {
		this.value = value;
		this.size = size;
		this.ratioProvider = occupiedSpaceRatio;
	}

	/**
	 * Normalizes the value size to the proportion of the canvas.
	 */
	public void normalize() {
		if (normalizedValue < 0)
			normalizedValue = size * ratioProvider.getAsDouble();
	}

	/**
	 * @return Original size of the {@link #getValue() wrapped value}.
	 */
	public double getSize() {
		return size;
	}

	/**
	 * @return Normalized size of the {@link #getValue() wrapped value}.
	 */
	public double getNormalizedSize() {
		return normalizedValue;
	}

	/**
	 * @return Wrapped data value.
	 */
	@Nonnull
	public T getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SizeInfo<?> sizeInfo = (SizeInfo<?>) o;
		if (Double.compare(sizeInfo.size, size) != 0) return false;
		return value.equals(sizeInfo.value);
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		temp = Double.doubleToLongBits(size);
		result = (int) (temp ^ (temp >>> 32));
		result = 31 * result + value.hashCode();
		return result;
	}
}
