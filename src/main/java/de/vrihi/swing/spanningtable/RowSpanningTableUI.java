package de.vrihi.swing.spanningtable;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;

public class RowSpanningTableUI extends BasicTableUI
{
	@Override
	public void paint(Graphics g, JComponent c)
	{
		Rectangle clip = g.getClipBounds();
		Rectangle bounds = table.getBounds();

		// account for the fact that the graphics has already been translated
		// into the table's bounds
		bounds.x = bounds.y = 0;

		if (table.getRowCount() <= 0 || table.getColumnCount() <= 0 || !bounds.intersects(clip))
			return;

		Rectangle visibleBounds = clip.intersection(bounds);
		Point upperLeft = visibleBounds.getLocation();
		Point lowerRight = new Point(visibleBounds.x + visibleBounds.width - 1,
				visibleBounds.y + visibleBounds.height - 1);

		int minRow = table.rowAtPoint(upperLeft);
		int maxRow = table.rowAtPoint(lowerRight);

		// This should never happen (as long as our bounds intersect the clip,
		// which is why we bail above if that is the case).
		if (minRow == -1) {
			minRow = 0;
		}
		// If the table does not have enough rows to fill the view we'll get -1.
		// (We could also get -1 if our bounds don't intersect the clip,
		// which is why we bail above if that is the case).
		// Replace this with the index of the last row.
		if (maxRow == -1) {
			maxRow = table.getRowCount() - 1;
		}

		// For FIT_WIDTH, all columns should be printed irrespective of
		// how many columns are visible. So, we used clip which is already set to
		// total col width instead of visible region
		// Since JTable.PrintMode is not accessible
		// from here, we aet "Table.printMode" in TablePrintable#print and
		// access from here.
		Object printMode = table.getClientProperty("Table.printMode");
		if ((printMode == JTable.PrintMode.FIT_WIDTH)) {
			upperLeft = clip.getLocation();
			lowerRight = new Point(clip.x + clip.width - 1, clip.y + clip.height - 1);
		}
		boolean ltr = table.getComponentOrientation().isLeftToRight();
		int minCol = table.columnAtPoint(ltr ? upperLeft : lowerRight);
		int maxCol = table.columnAtPoint(ltr ? lowerRight : upperLeft);
		// This should never happen.
		if (minCol == -1) {
			minCol = 0;
		}
		// If the table does not have enough columns to fill the view we'll get -1.
		// Replace this with the index of the last column.
		if (maxCol == -1) {
			maxCol = table.getColumnCount()-1;
		}

		paintGrid(g, minRow, maxRow, minCol, maxCol);

		paintCells(g, minRow, maxRow, minCol, maxCol);
	}

	private void paintCells(Graphics g, int minRow, int maxRow, int minCol, int maxCol)
	{
		for (int col = minCol; col <= maxCol; col++)
		{
			for (int row = minRow; row <= maxRow; row++)
			{
				paintCell(row, col, g, table.getCellRect(row, col, true));
				RowSpan span = ((RowSpanningTable) table).getSpanModel().getSpan(row, col);
				row += span.maxRow - row;
			}
		}

		rendererPane.removeAll();
	}

	private void paintCell(int row, int column, Graphics g, Rectangle area)
	{
		int verticalMargin = table.getRowMargin();
		int horizontalMargin = table.getColumnModel().getColumnMargin();

		area.setBounds(
				area.x + horizontalMargin / 2,
				area.y + verticalMargin / 2,
				area.width - horizontalMargin,
				area.height - verticalMargin
		);

		if (table.isEditing() && table.getEditingRow() == row && table.getEditingColumn() == column)
		{
			Component component = table.getEditorComponent();
			component.setBounds(area);
			component.validate();
		} else {
			TableCellRenderer renderer = table.getCellRenderer(row, column);
			Component component = table.prepareRenderer(renderer, row, column);
			if (component.getParent() == null)
			{
				rendererPane.add(component);
			}
			rendererPane.paintComponent(g, component, table, area.x, area.y, area.width, area.height, true);
		}
	}

	private void paintGrid(Graphics g, int rMin, int rMax, int cMin, int cMax)
	{
		g.setColor(table.getGridColor());

		Rectangle minCell = table.getCellRect(rMin, cMin, true);
		Rectangle maxCell = table.getCellRect(rMax, cMax, true);
		Rectangle damagedArea = minCell.union( maxCell );

		if (table.getShowHorizontalLines()) {
			int y = damagedArea.y;
			RowSpanModel spanModel = ((RowSpanningTable) table).getSpanModel();

			for (int row = rMin; row <= rMax; row++) {
				y += table.getRowHeight(row);

				int x = damagedArea.x;
				for (int col = cMin; col <= cMax; col++)
				{
					int columnWidth = table.getColumnModel().getColumn(col).getWidth();
					if (spanModel.getSpan(row, col).maxRow <= row)
						drawHLine(g, x, x + columnWidth - 1, y - 1);

					x += columnWidth;
				}
			}
		}
		if (table.getShowVerticalLines()) {
			TableColumnModel cm = table.getColumnModel();
			int tableHeight = damagedArea.y + damagedArea.height;
			int x;
			if (table.getComponentOrientation().isLeftToRight()) {
				x = damagedArea.x;
				for (int column = cMin; column <= cMax; column++) {
					int w = cm.getColumn(column).getWidth();
					x += w;
					drawVLine(g, x - 1, 0, tableHeight - 1);
				}
			} else {
				x = damagedArea.x;
				for (int column = cMax; column >= cMin; column--) {
					int w = cm.getColumn(column).getWidth();
					x += w;
					drawVLine(g, x - 1, 0, tableHeight - 1);
				}
			}
		}
	}

	/**
	 * This method should be used for drawing a borders over a filled rectangle.
	 * Draws vertical line, using the current color, between the points {@code
	 * (x, y1)} and {@code (x, y2)} in graphics context's coordinate system.
	 * Note: it use {@code Graphics.fillRect()} internally.
	 *
	 * @param g  Graphics to draw the line to.
	 * @param x  the <i>x</i> coordinate.
	 * @param y1 the first point's <i>y</i> coordinate.
	 * @param y2 the second point's <i>y</i> coordinate.
	 */
	public static void drawVLine(Graphics g, int x, int y1, int y2) {
		if (y2 < y1) {
			final int temp = y2;
			y2 = y1;
			y1 = temp;
		}
		g.fillRect(x, y1, 1, y2 - y1 + 1);
	}

	/**
	 * This method should be used for drawing a borders over a filled rectangle.
	 * Draws horizontal line, using the current color, between the points {@code
	 * (x1, y)} and {@code (x2, y)} in graphics context's coordinate system.
	 * Note: it use {@code Graphics.fillRect()} internally.
	 *
	 * @param g  Graphics to draw the line to.
	 * @param x1 the first point's <i>x</i> coordinate.
	 * @param x2 the second point's <i>x</i> coordinate.
	 * @param y  the <i>y</i> coordinate.
	 */
	public static void drawHLine(Graphics g, int x1, int x2, int y) {
		if (x2 < x1) {
			final int temp = x2;
			x2 = x1;
			x1 = temp;
		}
		g.fillRect(x1, y, x2 - x1 + 1, 1);
	}
}
