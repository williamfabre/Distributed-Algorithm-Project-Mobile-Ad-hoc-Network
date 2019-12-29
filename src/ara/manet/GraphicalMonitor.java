package ara.manet;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ara.manet.communication.Emitter;
import ara.manet.detection.NeighborProtocol;
import ara.manet.positioning.PositionProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */
public class GraphicalMonitor extends JPanel implements Control {

	private static final long serialVersionUID = -4639751772079773440L;

	private static final String PAR_POSITIONPID = "positionprotocol";
	private static final String PAR_NEIGHBORPID = "neighborprotocol";
	private static final String PAR_EMITTER = "emitter";
	private static final String PAR_TIMESLOW = "time_slow";
	private static final String PAR_MONITORABLEPID = "monitorableprotocol";

	// a modifier en fonction des gouts et des couleurs
	private static final Color[] color_state = { Color.GREEN, // etat 0
			Color.RED, // etat 1
			Color.pink, // etat 2
			Color.CYAN, // etat 3
			Color.MAGENTA, // etat 4
			Color.yellow, // etat 5
			Color.orange, // etat 6
			Color.BLACK // etat 7
	};

	private final int position_pid;
	private final int neighbor_pid;
	private final int emitter_pid;
	private final int monitorable_pid;

	private double time_slow;

	private final Dimension dimension_frame;

	private final Dimension dimension_terrain;
	private JFrame frame = null;

	private static final Monitorable defaultmonitorable = new Monitorable() {
		@Override
		public Object clone() {
			Monitorable res = null;
			try {
				res = (Monitorable) super.clone();
			} catch (CloneNotSupportedException e) {
			}
			return res;
		}
	};

	private boolean stop = false;

	public GraphicalMonitor(String prefix) {
		neighbor_pid = Configuration.getPid(prefix + "." + PAR_NEIGHBORPID, -1);
		position_pid = Configuration.getPid(prefix + "." + PAR_POSITIONPID);

		emitter_pid = Configuration.getPid(prefix + "." + PAR_EMITTER, -1);
		monitorable_pid = Configuration.getPid(prefix + "." + PAR_MONITORABLEPID, -1);

		time_slow = Configuration.getDouble(prefix + "." + PAR_TIMESLOW);

		Node n = Network.get(0);
		PositionProtocol pos = (PositionProtocol) n.getProtocol(position_pid);

		Dimension dim_screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		dim_screen = new Dimension((int) (dim_screen.getWidth() * 0.9), (int) (dim_screen.getHeight() * 0.9));
		dimension_terrain = new Dimension((int) pos.getMaxX(), (int) pos.getMaxY());

		int width = dimension_terrain.getWidth() > dim_screen.getWidth() ? (int) dim_screen.getWidth()
				: (int) dimension_terrain.getWidth();
		int height = dimension_terrain.getHeight() > dim_screen.getHeight() ? (int) dim_screen.getHeight()
				: (int) dimension_terrain.getHeight();

		dimension_frame = new Dimension(width, height);

	}

	private JLabel date = new JLabel();

	private void init() {
		frame = new JFrame();
		frame.setTitle("MANET SYSTEM");

		frame.setSize(dimension_frame);

		frame.setLocationRelativeTo(null);

		this.setBackground(Color.WHITE);
		this.setSize(frame.getSize());

		frame.getContentPane().add(this);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		JButton speed_up = new JButton("Accélerer simulation");
		speed_up.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GraphicalMonitor mon = GraphicalMonitor.this;
				synchronized (mon) {
					mon.time_slow /= 2;
				}
			}
		});

		JButton speed_down = new JButton("Ralentir simulation");
		speed_down.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GraphicalMonitor mon = GraphicalMonitor.this;
				synchronized (mon) {
					mon.time_slow *= 2;
				}
			}
		});

		JButton pause = new JButton("Pause");
		pause.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GraphicalMonitor mon = GraphicalMonitor.this;
				synchronized (mon) {
					mon.stop = !mon.stop;
					mon.notify();
				}
			}
		});

		this.add(speed_down);
		this.add(speed_up);
		this.add(pause);
		this.add(date);

	}

	@Override
	public void paintComponent(Graphics g) {

		date.setText("Date = " + CommonState.getTime());

		int size = 10;
		g.setColor(this.getBackground());
		g.fillRect(0, 0, this.getWidth(), this.getHeight());

		Monitorable monitorable = defaultmonitorable;

		for (int i = 0; i < Network.size(); i++) {
			Node n = Network.get(i);
			PositionProtocol pos = (PositionProtocol) n.getProtocol(position_pid);

			if (monitorable_pid != -1) {
				monitorable = (Monitorable) n.getProtocol(monitorable_pid);
			}

			int center_x = toGraphicX(pos.getCurrentPosition().getX());
			int center_y = toGraphicY(pos.getCurrentPosition().getY());

			int x_node = center_x - (size / 2);
			int y_node = center_y - (size / 2);

			if (emitter_pid != -1) {
				Emitter emitter = (Emitter) n.getProtocol(emitter_pid);

				g.setColor(Color.CYAN);

				int size_scope_x = toGraphicX(emitter.getScope());
				int size_scope_y = toGraphicY(emitter.getScope());
				int x_scope = center_x - size_scope_x;
				int y_scope = center_y - size_scope_y;
				g.drawOval(x_scope, y_scope, size_scope_x * 2, size_scope_y * 2);
			}

			g.setColor(Color.BLACK);
			int pas = 10;
			int j = 0;
			for (String info : monitorable.infos(n)) {
				g.drawString(info, x_node + size, y_node + (j * pas));
				j++;
			}

			if (neighbor_pid != -1) {
				NeighborProtocol neighb = (NeighborProtocol) n.getProtocol(neighbor_pid);
				Long[] neighbors = new Long[neighb.getNeighbors().size()];
				neighbors = neighb.getNeighbors().toArray(neighbors);
				for (Long id : neighbors) {
					Node neighbor = getNodefromId(id);
					PositionProtocol pos_neigh = (PositionProtocol) neighbor.getProtocol(position_pid);
					int center_x_neighbor = toGraphicX(pos_neigh.getCurrentPosition().getX());
					int center_y_neighbor = toGraphicY(pos_neigh.getCurrentPosition().getY());
					g.drawLine(center_x, center_y, center_x_neighbor, center_y_neighbor);
				}
			}

			int state = monitorable.getState(n);
			Color color = color_state[Math.min(state, color_state.length - 1)];

			g.setColor(color);
			g.fillOval(x_node, y_node, size, size);

		}
		g.setColor(Color.black);
		g.drawString("slow_factor = " + this.time_slow, this.getWidth() - 400, 10);
		if (stop) {
			g.setColor(Color.ORANGE);
			g.setFont(new Font("Arial", Font.BOLD, 39));
			g.drawString("PAUSE", 50, 50);
		}

	}

	private Node getNodefromId(long id) {
		for (int i = 0; i < Network.size(); i++) {
			Node n = Network.get(i);
			if (n.getID() == id) {
				return n;
			}
		}

		throw new RuntimeException("Unknwon Id :" + id);
	}

	private int toGraphicX(double x_terrain) {
		double res = (x_terrain * dimension_frame.getWidth()) / dimension_terrain.getWidth();
		return (int) res;
	}

	private int toGraphicY(double y_terrain) {
		double res = (y_terrain * dimension_frame.getHeight()) / dimension_terrain.getHeight();
		return (int) res;
	}

	@Override
	public boolean execute() {
		if (frame == null) {
			init();
		}
		try {
			int nb_milisec;
			int nb_nano;
			synchronized (this) {
				this.repaint();
				if (stop) {
					this.wait();
				}
				nb_milisec = (int) time_slow;
				double nb_milisec_double = (double) nb_milisec;
				nb_nano = (int) ((time_slow - nb_milisec_double) * 1000000.0);
			}
			Thread.sleep(nb_milisec, nb_nano);

		} catch (InterruptedException e) {
		}
		return false;
	}

}
