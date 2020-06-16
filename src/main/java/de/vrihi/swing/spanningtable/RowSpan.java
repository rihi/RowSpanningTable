package de.vrihi.swing.spanningtable;

import java.util.stream.IntStream;

public class RowSpan
{
	public final int minRow;
	public final int maxRow;

	public RowSpan(int minRow, int maxRow)
	{
		this.minRow = minRow;
		this.maxRow = maxRow;
	}

	public IntStream rows()
	{
		return IntStream.rangeClosed(minRow, maxRow);
	}
}
