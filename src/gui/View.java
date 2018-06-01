package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;

import model.GeoCar;
import model.GeoServer;
import model.GeoTrafficLightMaster;

import org.openstreetmap.gui.jmapviewer.JMapViewer;

import controller.newengine.SimulationEngine;

public class View extends JFrame {
	private static final long serialVersionUID = 1L;
	int N, M;
	JPanel map;
	ServerView serv;
	List<CarView> carsView;
	TreeMap<Long, GeoTrafficLightMaster> trafficLightView;

	private JTextArea timer = new JTextArea();
	private JTextArea comms = new JTextArea();
	public JPanel panel = new JPanel();
	JButton finishButton = new JButton("Finish");
	ChartView cht;

	public View(int N, int M, JMapViewer map, ServerView serv,
			List<CarView> carsView, TreeMap<Long, GeoTrafficLightMaster> trafficLightView) {
		this.N = N;
		this.M = M;
		this.map = map;
		map.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				System.out.println(e.getX() + " " + e.getY());
				System.out.println(map.getPosition(e.getX(), e.getY()));
				
			}
			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		this.serv = serv;
		this.carsView = carsView;
		this.trafficLightView = trafficLightView;
		initView();
	}

	public void setTimer(String time) {
		this.timer.setText(time);
	}

	public String getTimer() {
		return timer.getText();
	}

	public void setComms(String data) {
		this.comms.setText(data);
	}

	public void setMap(JPanel map) {
		this.map = map;
	}

	public String getComms() {
		return comms.getText();
	}

	void initView() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		panel.setLayout(new OverlayLayout(panel));
		panel.add(map);
		JPanel actionPanel = new JPanel();
		actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
		actionPanel.setBorder(BorderFactory.createTitledBorder("Control"));

		finishButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		finishButton.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				SimulationEngine.getInstance().stopSimulation();
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}
		});

		actionPanel.add(finishButton);
		actionPanel.add(timer);
		actionPanel.add(comms);

		Container contentPane = getContentPane();
		contentPane.add(panel, BorderLayout.CENTER);
		contentPane.add(actionPanel, BorderLayout.EAST);
		setSize(900, 900);
		setLocation(100, 100);
	}

	public void showView() {
		setVisible(true);
	}

	public void disableCarView(long id) {
		CarView carView;
		for (int i = 0; i < carsView.size(); i++) {
			carView = carsView.get(i);
			if (carView.getId() == id) {
				carView.disableDrawingCar();
				final CarView a = carView;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						a.updateCarView();
					}
				});
				break;
			}
		}
	}

	public void initLocationServer(ArrayList<GeoServer> new_servs) {
		serv.initLocationServer(new_servs);
	}

	public void addNewServer(GeoServer new_serv) {
		serv.addnewServerView(new_serv);
	}

	public void showChart(int tip, String title, ArrayList<GeoCar> data) {
		cht = new ChartView(panel, title, data, tip);
		repaint();
	}
}