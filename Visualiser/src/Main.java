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
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class Main extends JPanel {
	private AtomicReference<int[]> data;

	public Main(final File hidraw)
	{
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		data = new AtomicReference<int[]>(new int[] {0xff, 0xff, 0xff}); /* dummy data */

		String[][] tableData = new String[32][3];
		for (int i=0; i < 32; i++) {
			tableData[i][0] = String.valueOf(i);
			tableData[i][1] = "0x00";
			tableData[i][2] = "0x00";
		}

		tableData[0][0] = "hid_id";
		tableData[1][0] = "group_id/pressure";
		tableData[6][0] = "timestamp";

		final JTable table = new JTable(tableData, new String[] {"description", "values", "values2"});
		Box b = Box.createVerticalBox();
		b.setBorder(BorderFactory.createTitledBorder("Raw data"));
		b.add(table.getTableHeader());
		b.add(table);

		Vis v = new Vis();
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
								table.setValueAt(String.format("0x%02x", buf[i]), i, buf[1] < 0x0f ? 2 : 1);

							data.set(Arrays.copyOfRange(buf, 7, 32));
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

	private class Vis extends JComponent {
		public Vis()
		{
			setPreferredSize(new Dimension(400, 400));
		}

		@Override
		protected void paintComponent(Graphics g) {
			Insets insets = getInsets();
			int width = getWidth() - insets.left - insets.right, height = getHeight() - insets.top - insets.bottom;
			int x = insets.left, y = insets.top;
			final int data[] = Main.this.data.get();
			final int part = height / data.length;
			for (int i=0; i < data.length; i++) {
				g.setColor(new Color(0xff - data[i], 0, 0));
				g.fillRect(x, y + part * i, width, part);

				int loc = data[i] * width / (1 << 8);
				g.setColor(new Color(0, 0, 0xff));
				g.drawLine(x + loc, y + part * i, x + loc, y + part * (i+1));
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