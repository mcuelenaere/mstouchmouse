/**
 * Simple visualizer for MS Touch Mouse raw touch data
 * 
 * @author Maurus Cuelenaere
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class Main extends JPanel {
	private static class DataParser {
		private int[][] data;
		private int lastTime;
		private int x, y;
		private boolean advanceFlag;

		public DataParser() {
			data = new int[15][13];
			reset();
		}

		/*
		 *    0 1 2 3 4 5 6 7 8 9 A B C D E F
		 *  0       * * * * * * * * * *
		 *  1     * * * * * * * * * * * *
		 *  2   * * * * * * * * * * * * * *
		 *  3   * * * * * * * * * * * * * *
		 *  4 * * * * * * * * * * * * * * * *
		 *  5 * * * * * * * * * * * * * * * *
		 *  6 * * * * * * * * * * * * * * * *
		 *  7 * * * * * * * * * * * * * * * *
		 *  8 * * * * * * * * * * * * * * * *
		 *  9 * * * * * * * * * * * * * * * *
		 *  A * * * * * * * * * * * * * * * *
		 *  B * * * * * * * * * * * * * * * *
		 *  C * * * * * * * * * * * * * * * *
		 *  D * * * * * * * * * * * * * * * *
		 */
		public void advance() {
		    x++;

		    int max, nextMin;
		    switch (y) {
		        case 0:
		            max = 11;
		            nextMin = 2;
		        break;
		        case 1:
		            max = 12;
		            nextMin = 1;
		        break;
		        case 2:
		            max = 13;
		            nextMin = 1;
		        break;
		        case 3:
		            max = 13;
		            nextMin = 0;
		        break;
		        default:
		            max = 14;
		            nextMin = 0;
		        break;
		    }

		    if (x > max) {
		        y++;
		        x = nextMin;
		    }
		}

		private void parseNibble(int b) {
		    if (advanceFlag) {
		        advanceFlag = false;
		        for (int i = -3; i < b; i++)
		            advance();
		    } else {
		        if (b == 0xF) {
		            advanceFlag = true;
		        } else {
		        	if (y < 13)
		        		data[x][y] = (0xFF * b) / 0xF;
		            advance();
		        }
		    }
		}

		public void parseData(int buffer[]) {
			if (buffer[0] != 0x27 || buffer[5] != 0x51)
				return;

			int dataLength = buffer[1];
			int curTime = buffer[6];

			if (lastTime != curTime) {
				reset();
				lastTime = curTime;
			}

		    for (int i=7; i < 7 + dataLength - 1; i++) {
		    	parseNibble(buffer[i] & 0xF);
		    	parseNibble(buffer[i] >> 4);
		    }
		}

		private void reset() {
			for (int[] line : data)
				Arrays.fill(line, 0);
			x = 3;
			y = 0;
			advanceFlag = false;
		}

		public int[][] getData() {
			return data;
		}
	}

	private DataParser parser = new DataParser();

	public Main(final File hidraw)
	{
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		String[][] tableData = new String[32][2];
		for (int i=0; i < 32; i++) {
			tableData[i][0] = String.valueOf(i);
			tableData[i][1] = "0x00";
		}

		tableData[0][0] = "hid_id";
		tableData[1][0] = "length";
		tableData[2][0] = tableData[3][0] = tableData[4][0] = "unknown";
		tableData[5][0] = "footer";
		tableData[6][0] = "timestamp";

		final JTable table = new JTable(tableData, new String[] {"description", "values"});
		Box b = Box.createVerticalBox();
		b.setBorder(BorderFactory.createTitledBorder("Raw data"));
		b.add(table.getTableHeader());
		b.add(table);

		Visualizer v = new Visualizer();
		v.setBorder(BorderFactory.createTitledBorder("Visualizer"));

		add(v);
		add(b);

		System.out.println(hidraw);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					int buf[] = new int[32];
					DataInputStream r = new DataInputStream(new BufferedInputStream(new FileInputStream(hidraw)));
					while (true) {
						/* See ../../FORMAT */
						int bit = r.readUnsignedByte();
						if (bit == 0x27) {
							buf[0] = 0x27;
							for (int i=1; i < buf.length; i++)
								buf[i] = r.readUnsignedByte();

							for (int i = 0; i < buf.length; i++)
								table.setValueAt(String.format("0x%02x", buf[i]), i, 1);

							parser.parseData(buf);
							repaint();
						} else if (bit == 0x21) {
							int b1 = r.readUnsignedByte(), b2 = r.readUnsignedByte();
							System.out.println(String.format("finger %s (%02x, %02x)", (b2 & (1 << 1)) != 0 ? "pressed" : "gone", b1, b2));
						} else {
							System.out.println("Unknown bit " + bit);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private class Visualizer extends JComponent {
		public Visualizer() {
			setPreferredSize(new Dimension(400, 400));
		}

		@Override
		protected void paintComponent(Graphics g) {
			Insets insets = getInsets();
			int totalWidth = getWidth() - insets.left - insets.right, totalHeight = getHeight() - insets.top - insets.bottom;
			int left = insets.left, top = insets.top;

			final int[][] data = parser.getData();
			int pixWidth = totalWidth / data.length, pixHeight = totalHeight / data[0].length;
			for (int x=0; x < data.length; x++) {
				for (int y=0; y < data[0].length; y++) {
					g.setColor(new Color(data[x][y], 0, 0));
					g.fillRect(left + x * pixWidth, top + y * pixHeight, pixWidth, pixHeight);
				}
			}
		}
	}

	public static void main(final String args[]) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JFrame frame = new JFrame("Visualizer");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.add(new Main(new File(args[0])));
				frame.pack();
				frame.setVisible(true);
			}
		});
	}
}