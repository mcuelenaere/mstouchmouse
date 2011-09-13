/**
 * Simple visualizer for MS Touch Mouse raw touch data
 * 
 * @author Maurus Cuelenaere
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class Main extends JPanel {
	private AtomicReference<int[]> data;

	private static File discoverHidRaw()
	{
		File base = new File("/sys/bus/hid/devices");
		File hids[] = base.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.matches("[0-9A-Z]+:045E:0773.[0-9A-Z]+");
			}
		});

		File hid = hids[hids.length-1];
		String hidraw = new File(hid, "hidraw/").list()[0];
		return new File("/dev", hidraw);
	}

	public Main()
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
		b.add(table.getTableHeader());
		b.add(table);

		add(new Vis());
		add(b);

		final File hidraw = discoverHidRaw();
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
								table.getModel().setValueAt(String.format("0x%02x", buf[i]), i, buf[1] < 0x10 ? 2 : 1);

							data.set(Arrays.copyOfRange(buf, 6, 32));
							repaint();
						} else if (bit == 0x21) {
							r.skip(1);
							System.out.println("finger " + (r.readUnsignedByte() == 0x12 ? "down" : "up"));
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
			final int data[] = Main.this.data.get();
			final int part = getHeight() / data.length;
			boolean flag = false;
			for (int i=6; i < data.length; i++) {
				int k = i - 6;

				if (data[i] != 0xff && !flag) {
					int tmp = data[i+1] | (data[i] << 8);
					int loc = tmp * getWidth() / (1 << 16);

					g.setColor(Color.BLACK);
					g.fillRect(0, part * k, getWidth(), part * 2);
					g.setColor(new Color(0, 0, 0xff));
					g.drawLine(loc, part * k, loc, part * (k+2));

					i++;
					flag = true;
				} else {
					g.setColor(new Color(0xff - data[i], 0, 0));
					g.fillRect(0, part * k, getWidth(), part);
				}
			}
		}
	}

	public static void main(String args[]) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JFrame frame = new JFrame("Visualizer");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.add(new Main());
				frame.pack();
				frame.setVisible(true);
			}
		});
	}
}