package de.vrihi.swing.spanningtable;

import javax.swing.table.TableModel;
import java.util.Objects;
import java.util.stream.IntStream;

public class GroupingRowSpanModel implements RowSpanModel
{
	private final TableModel tableModel;

	public GroupingRowSpanModel(TableModel tableModel)
	{
		this.tableModel = Objects.requireNonNull(tableModel);
	}

	@Override
	public RowSpan getSpan(int row, int column)
	{
		Object val = tableModel.getValueAt(row, column);

		int minRow = IntStream.iterate(row - 1, operand -> operand - 1)
				.filter(value -> value < 0 || tableModel.getValueAt(value, column) != val)
				.findFirst()
				.getAsInt() + 1;

		int maxRow = IntStream.iterate(row + 1, operand -> operand + 1)
				.filter(value -> value >= tableModel.getRowCount() || tableModel.getValueAt(value, column) != val)
				.findFirst()
				.getAsInt() - 1;

		return new RowSpan(minRow, maxRow);
	}
}
