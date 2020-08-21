package de.vrihi.swing.spanningtable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RowSpanningTable extends JTable
{
	private RowSpanModel spanModel;

	public RowSpanningTable() {}

	public RowSpanningTable(TableModel dm, RowSpanModel spanModel)
	{
		super(dm);
		this.spanModel = Objects.requireNonNull(spanModel);
		setUI(new RowSpanningTableUI());
	}

	public RowSpanningTable(int numRows, int numColumns, RowSpanModel spanModel)
	{
		super(numRows, numColumns);
		this.spanModel = Objects.requireNonNull(spanModel);
		setUI(new RowSpanningTableUI());
	}

	public RowSpanningTable(Object[][] rowData, Object[] columnNames, RowSpanModel spanModel)
	{
		super(rowData, columnNames);
		this.spanModel = Objects.requireNonNull(spanModel);
		setUI(new RowSpanningTableUI());
	}

	public void setSpanModel(RowSpanModel spanModel)
	{
		this.spanModel = Objects.requireNonNull(spanModel);
		setUI(new RowSpanningTableUI());
		repaint();
	}

	public RowSpanModel getSpanModel()
	{
		return spanModel;
	}

	public RowSpan getViewRowSpan(int row, int column)
	{
		RowSpan span = spanModel.getSpan(convertRowIndexToModel(row), convertColumnIndexToModel(column));
		return convertRowSpanToView(span, row);
	}

	public RowSpan convertRowSpanToView(RowSpan span, int row)
	{
		List<Integer> spanList = span.rows()
				.mapToObj(this::convertRowIndexToView)
				.filter(r -> r >= 0)
				.collect(Collectors.toList());

		long fewerRows = IntStream.iterate(1, operand -> operand + 1)
				.map(offset -> row - offset)
				.takeWhile(spanList::contains)
				.count();
		long additionalRows = IntStream.iterate(1, operand -> operand + 1)
				.map(offset -> row + offset)
				.takeWhile(spanList::contains)
				.count();

		return new RowSpan((int) (row - fewerRows), (int) (row + additionalRows));
	}

	@Override
	public Rectangle getCellRect(int row, int column, boolean includeSpacing)
	{
		if (spanModel == null)
			return super.getCellRect(row, column, includeSpacing);

		RowSpan span = getViewRowSpan(row, column);

		var rec = new Rectangle(-1, -1);
		for (int r = span.minRow; r <= span.maxRow; r++)
		{
			rec.add(super.getCellRect(r, column, true));
		}

		if (!includeSpacing)
		{
			// Bound the margins by their associated dimensions to prevent returning bounds with negative dimensions
			int rm = Math.min(getRowMargin(), rec.height);
			int cm = Math.min(getColumnModel().getColumnMargin(), rec.width);
			rec.setBounds(rec.x + cm / 2, rec.y + rm / 2, rec.width - cm, rec.height - rm);
		}

		return rec;
	}

	public Rectangle getUnderlyingCellRect(int row, int col, boolean includeSpacing)
	{
		return super.getCellRect(row, col, includeSpacing);
	}

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
	{
		Object value = getValueAt(row, column);

		boolean isSelected = false;
		boolean hasFocus = false;

		// Only indicate the selection and focused cell if not printing
		if (!isPaintingForPrint()) {
			boolean rowIsLead;
			boolean colIsLead;

			if (spanModel != null)
			{
				RowSpan span = getViewRowSpan(row, column);

				isSelected = span
						.rows()
						.mapToObj(r -> isCellSelected(r, column))
						.reduce(false, Boolean::logicalOr);

				rowIsLead = span.rows().anyMatch(r -> r == selectionModel.getLeadSelectionIndex());
				colIsLead = columnModel.getSelectionModel().getLeadSelectionIndex() == column;
			} else {
				isSelected = isCellSelected(row, column);

				rowIsLead = selectionModel.getLeadSelectionIndex() == row;
				colIsLead = columnModel.getSelectionModel().getLeadSelectionIndex() == column;
			}

			hasFocus = (rowIsLead && colIsLead) && isFocusOwner();
		}

		return renderer.getTableCellRendererComponent(this, value,
				isSelected, hasFocus,
				row, column);
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		super.valueChanged(e);

		int firstIndex = Math.min(getRowCount() - 1, Math.max(e.getFirstIndex(), 0));
		int lastIndex = Math.min(getRowCount() - 1, Math.max(e.getLastIndex(), 0));

		var dirtyRegion = new Rectangle(-1, -1);
		for (int row = firstIndex; row <= lastIndex; row++)
		{
			for (int col = 0; col < getColumnCount(); col++)
			{
				dirtyRegion.add(getCellRect(row, col, false));
			}
		}

		repaint(dirtyRegion);
	}
}
