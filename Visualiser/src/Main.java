/**
 * Simple visualizer for MS Touch Mouse raw touch data
 * 
 * @author Maurus Cuelenaer
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Main extends JPanel {
	private volatile int data[];

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
		data = new int[] {0xff, 0x5f, 0x00}; /* dummy data */

		final JLabel l = new JLabel();
		add(new Vis());
		add(l);
		
		final File hidraw = discoverHidRaw();
		System.out.println(hidraw);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					DataInputStream r = new DataInputStream(new BufferedInputStream(new FileInputStream(hidraw)));
					while (true) {
						/* See ../../FORMAT */
						int bit = r.readUnsignedByte();
						if (bit == 0x27) {
							int buf[] = new int[31];
							for (int i=0; i < buf.length; i++)
								buf[i] = r.readUnsignedByte();

							StringBuilder sb = new StringBuilder();
							sb.append("27 ");
							for (int i=0; i < buf.length; i++) {
								sb.append(String.format("%02x", buf[i]));
								sb.append(" ");
							}

							int tmp[] = new int[16];
							for (int i=0; i < 16; i++)
								tmp[i] = buf[6+i];
							data = tmp;

							l.setText(sb.toString());
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
			final int part = getHeight() / data.length;
			boolean flag = false;
			for (int i=0; i < data.length; i++) {
				if (data[i] != 0xff && !flag) {
					int tmp = data[i+1] | (data[i] << 8);
					int loc = tmp * getWidth() / (1 << 16);

					g.setColor(Color.BLACK);
					g.fillRect(0, part * i, getWidth(), part*2);
					g.setColor(new Color(0, 0, 0xff));
					g.drawLine(loc, part*i, loc, part*(i+2));

					i++;
					flag = true;
				} else {
					g.setColor(new Color(0xff - data[i], 0, 0));
					g.fillRect(0, part * i, getWidth(), part);
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
				frame.setSize(640, 480);
				frame.setVisible(true);
			}
		});
	}
}