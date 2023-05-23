package software.coley.treemap.squaring;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * A class to generate a squarified treemap layout from a set of values.
 *
 * @param <T>
 * 		Content type associated with inputs.
 *
 * @author agatheblues - Reference implementation of squarify algorithm
 * @author Matt Coley - Generic support, modernizations and UI integrations
 */
public class Squarify<T> {
	private final List<Rectangle<T>> rectangles;

	/**
	 * @param values
	 * 		Values to represent in a squarified treemap.
	 * @param sizeFunction
	 * 		Function to convert {@code T} values to sizes.
	 * @param x
	 * 		Start X offset into canvas to begin filling into.
	 * @param y
	 * 		Start Y offset into canvas to begin filling into.
	 * @param width
	 * 		Width of the space in the canvas to fill.
	 * @param height
	 * 		Height of the space in the canvas to fill.
	 */
	public Squarify(@Nonnull List<T> values, @Nonnull ToDoubleFunction<T> sizeFunction,
					double x, double y, double width, double height) {
		SizeInfoProcessor<T> processor = new SizeInfoProcessor<>(values, sizeFunction, width, height);
		rectangles = squarify(processor.getSizeInfos(), x, y, width, height);
	}

	/**
	 * Compute the list of rectangles that fill up the canvas, in proportion to the values.
	 * Optimized to have rectangles with a ratio closest to 1 to have squares.
	 *
	 * @param values
	 * 		The list of values, normalized to the canvas size, to squarify.
	 * @param x
	 * 		Current x coordinate of leftover canvas.
	 * 		Values below {@code x} have already been filled with rectangles.
	 * @param y
	 * 		Current y coordinate of leftover canvas.
	 * 		Values below {@code y} have already been filled with rectangles.
	 * @param width
	 * 		Current width remaining in the canvas.
	 * @param height
	 * 		Current height remaining in the canvas.
	 *
	 * @return Squarified rectangles of the passes values.
	 */
	@Nonnull
	private List<Rectangle<T>> squarify(@Nonnull List<SizeInfo<T>> values, double x, double y, double width, double height) {
		// Nothing to squarify, yield empty list.
		if (values.size() == 0)
			return Collections.emptyList();

		// End of recursion chain.
		if (values.size() == 1) {
			SizeInfo<T> sizeInfo = values.get(0);
			return Collections.singletonList(makeRect(sizeInfo.getValue(), x, y, width, height));
		}

		// Move forward until the ratio changes between the i'th and i+1'th value.
		// This determines where our layout row/column divisions occur.
		int i = 1;
		while ((i < values.size()) && (worstRatio(values.subList(0, i), x, y, width, height) >= worstRatio(values.subList(0, i + 1), x, y, width, height)))
			i++;

		List<SizeInfo<T>> current = values.subList(0, i);
		List<SizeInfo<T>> remaining = values.subList(i, values.size());

		// Compute how much space is left in the canvas.
		Rectangle<T> currentLeftover = remainingSpace(current, x, y, width, height);

		// Fill the remaining space with rectangles for the remaining values.
		List<Rectangle<T>> rectangles = new ArrayList<>(values.size());
		rectangles.addAll(layout(current, x, y, width, height));
		rectangles.addAll(squarify(remaining, currentLeftover.x(), currentLeftover.y(), currentLeftover.width(), currentLeftover.height()));
		return rectangles;
	}

	/**
	 * @return Result of squarification operation.
	 */
	@Nonnull
	public List<Rectangle<T>> getRectangles() {
		return rectangles;
	}

	/**
	 * @param values
	 * 		Values to sum the normalized size of.
	 *
	 * @return Normalized sum of values.
	 */
	private double sumNormalizedSizes(@Nonnull List<SizeInfo<T>> values) {
		double result = 0;
		for (SizeInfo<?> info : values)
			result += info.getNormalizedSize();
		return result;
	}

	/**
	 * @param values
	 * 		Values to generate rectangles for.
	 * @param x
	 * 		Current x coordinate of leftover canvas.
	 * 		Values below {@code x} have already been filled with rectangles.
	 * @param y
	 * 		Current y coordinate of leftover canvas.
	 * 		Values below {@code y} have already been filled with rectangles.
	 * @param height
	 * 		Current height remaining in the canvas.
	 *
	 * @return Generated rectangles occupying a row <i>(All {@link Rectangle#width()} values will be the same)</i>.
	 *
	 * @see #layout(List, double, double, double, double) Calls this method when {@code width >= height}.
	 */
	@Nonnull
	private List<Rectangle<T>> layoutRow(@Nonnull List<SizeInfo<T>> values, double x, double y, double height) {
		double coveredArea = sumNormalizedSizes(values);
		double width = coveredArea / height;
		List<Rectangle<T>> rectangles = new ArrayList<>(values.size());
		for (SizeInfo<T> value : values) {
			rectangles.add(makeRect(value.getValue(), x, y, width, value.getNormalizedSize() / width));
			y += value.getNormalizedSize() / width;
		}
		return rectangles;
	}

	/**
	 * @param values
	 * 		Values to generate rectangles for.
	 * @param x
	 * 		Current x coordinate of leftover canvas.
	 * 		Values below {@code x} have already been filled with rectangles.
	 * @param y
	 * 		Current y coordinate of leftover canvas.
	 * 		Values below {@code y} have already been filled with rectangles.
	 * @param width
	 * 		Current width remaining in the canvas.
	 *
	 * @return Generated rectangles occupying a column <i>(All {@link Rectangle#height()} values will be the same)</i>.
	 *
	 * @see #layout(List, double, double, double, double) Calls this method when {@code width < height}.
	 */
	@Nonnull
	private List<Rectangle<T>> layoutColumn(@Nonnull List<SizeInfo<T>> values, double x, double y, double width) {
		double coveredArea = sumNormalizedSizes(values);
		double height = coveredArea / width;
		List<Rectangle<T>> rectangles = new ArrayList<>(values.size());
		for (SizeInfo<T> value : values) {
			rectangles.add(makeRect(value.getValue(), x, y, value.getNormalizedSize() / height, height));
			x += value.getNormalizedSize() / height;
		}
		return rectangles;
	}

	/**
	 * @param values
	 * 		Values that have had rectangles already generated.
	 * 		Used for computing the total current occupied space.
	 * @param x
	 * 		Current x coordinate of leftover canvas.
	 * 		Values below {@code x} have already been filled with rectangles.
	 * @param y
	 * 		Current y coordinate of leftover canvas.
	 * 		Values below {@code y} have already been filled with rectangles.
	 * @param width
	 * 		Current width remaining in the canvas.
	 * @param height
	 * 		Current height remaining in the canvas.
	 *
	 * @return Computed remaining area.
	 *
	 * @see #remainingSpace(List, double, double, double, double) Calls this method when {@code width < height}.
	 */
	@Nonnull
	@SuppressWarnings("UnnecessaryLocalVariable")
	private Rectangle<T> remainingRowSpace(@Nonnull List<SizeInfo<T>> values, double x, double y, double width, double height) {
		double coveredArea = sumNormalizedSizes(values);
		double coveredHeight = coveredArea / height;
		double leftoverX = x + coveredHeight;
		double leftoverY = y;
		double leftoverWidth = width - coveredHeight;
		double leftoverHeight = height;
		return makeRect(null, leftoverX, leftoverY, leftoverWidth, leftoverHeight);
	}

	/**
	 * @param values
	 * 		Values that have had rectangles already generated.
	 * 		Used for computing the total current occupied space.
	 * @param x
	 * 		Current x coordinate of leftover canvas.
	 * 		Values below {@code x} have already been filled with rectangles.
	 * @param y
	 * 		Current y coordinate of leftover canvas.
	 * 		Values below {@code y} have already been filled with rectangles.
	 * @param width
	 * 		Current width remaining in the canvas.
	 * @param height
	 * 		Current height remaining in the canvas.
	 *
	 * @return Computed remaining area.
	 *
	 * @see #remainingSpace(List, double, double, double, double) Calls this method when {@code width >= height}.
	 */
	@Nonnull
	@SuppressWarnings("UnnecessaryLocalVariable")
	private Rectangle<T> remainingColumnSpace(@Nonnull List<SizeInfo<T>> values, double x, double y, double width, double height) {
		double coveredArea = sumNormalizedSizes(values);
		double coveredWidth = coveredArea / width;
		double leftoverX = x;
		double leftoverY = y + coveredWidth;
		double leftoverWidth = width;
		double leftoverHeight = height - coveredWidth;
		return makeRect(null, leftoverX, leftoverY, leftoverWidth, leftoverHeight);
	}

	/**
	 * @param values
	 * 		Values that have had rectangles already generated.
	 * 		Used for computing the total current occupied space.
	 * @param x
	 * 		Current x coordinate of leftover canvas.
	 * 		Values below {@code x} have already been filled with rectangles.
	 * @param y
	 * 		Current y coordinate of leftover canvas.
	 * 		Values below {@code y} have already been filled with rectangles.
	 * @param width
	 * 		Current width remaining in the canvas.
	 * @param height
	 * 		Current height remaining in the canvas.
	 *
	 * @return Computed remaining area.
	 * <p>
	 * When the remaining width is greater than <i>(or equal to)</i> the remaining height,
	 * then the space will represent {@link #remainingRowSpace(List, double, double, double, double) a row}.
	 * Otherwise, it will represent {@link #remainingColumnSpace(List, double, double, double, double) a column}.
	 */
	@Nonnull
	private Rectangle<T> remainingSpace(@Nonnull List<SizeInfo<T>> values, double x, double y, double width, double height) {
		if (width >= height)
			return remainingRowSpace(values, x, y, width, height);
		return remainingColumnSpace(values, x, y, width, height);
	}

	/**
	 * @param values
	 * 		Values to generate rectangles for.
	 * @param x
	 * 		Current x coordinate of leftover canvas.
	 * 		Values below {@code x} have already been filled with rectangles.
	 * @param y
	 * 		Current y coordinate of leftover canvas.
	 * 		Values below {@code y} have already been filled with rectangles.
	 * @param width
	 * 		Current width remaining in the canvas.
	 * @param height
	 * 		Current height remaining in the canvas.
	 *
	 * @return Generated list of rectangles for the provided values,
	 * populating a row if the current width is greater than <i>(or equal to)</i> the current height,
	 * or a column otherwise.
	 */
	@Nonnull
	private List<Rectangle<T>> layout(@Nonnull List<SizeInfo<T>> values, double x, double y, double width, double height) {
		if (width >= height)
			return layoutRow(values, x, y, height);
		return layoutColumn(values, x, y, width);
	}

	/**
	 * @param value
	 * 		Value to wrap.
	 * @param x
	 * 		Rect x coordinate.
	 * @param y
	 * 		Rect y coordinate.
	 * @param width
	 * 		Rect width.
	 * @param height
	 * 		Rect height.
	 *
	 * @return New rect with the provided dimensions.
	 */
	@Nonnull
	protected Rectangle<T> makeRect(@Nullable T value, double x, double y, double width, double height) {
		return new Rectangle<>(value, x, y, width, height);
	}

	/**
	 * @param values
	 * 		Values to check.
	 * @param x
	 * 		Current x coordinate of leftover canvas.
	 * 		Values below {@code x} have already been filled with rectangles.
	 * @param y
	 * 		Current y coordinate of leftover canvas.
	 * 		Values below {@code y} have already been filled with rectangles.
	 * @param width
	 * 		Current width remaining in the canvas.
	 * @param height
	 * 		Current height remaining in the canvas.
	 *
	 * @return Worst ratio among the values <i>(farthest from 1)</i> to form a square.
	 */
	private double worstRatio(@Nonnull List<SizeInfo<T>> values, double x, double y, double width, double height) {
		List<Rectangle<T>> rectangles = layout(values, x, y, width, height);
		double max = 0;
		for (Rectangle<T> rectangle : rectangles) {
			double rectWidth = rectangle.width();
			double rectHeight = rectangle.height();
			double maxValue = Math.max(rectWidth / rectHeight, rectHeight / rectWidth);
			max = Math.max(maxValue, max);
		}
		return max;
	}
}
