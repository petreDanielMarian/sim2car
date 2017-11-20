package controller.engine;

import gui.CarView;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import model.GeoCar;
import model.parameters.Globals;
import model.threadpool.ThreadPool;

public class Animation implements Runnable {

	
	
	private final Logger logger;
	private EngineSimulation engine;
	
	public Animation(EngineSimulation engine) {
		
		this.engine = engine;
		this.logger = Logger.getLogger(Animation.class.getName());
	}
	
	public void run() {
		// printHeader();
		long time = engine.getNow().getTime();
		int totalRuns = 0;
		int nr_dis = 0;
		while (engine.isRunning()) {
			time += Globals.timePeriod;
			totalRuns++;
			nr_dis = 0;
			
			String commsStr = "Communicating:\n";
			engine.getNow().setTime(time);
			if (Globals.showGUI)
				engine.getView().setTimer(new Date(time * 1000).toString());

			for (int i = 0; i < engine.getCars().size(); i++) {
				GeoCar c = engine.getCars().get(i);
				if (c.getActive() == 0) {
					nr_dis++;
					continue;
				}

//				engine.getCarsMobility().get(i).update(engine.getNow());
//				if (c.getNextPos() == null)
//					continue;
				commsStr += engine.getCars().get(i).runApplications();
				if (Globals.showGUI)
					engine.getCarsView().get(i).updateCarView();
			}

			if (Globals.showGUI)
				engine.getView().setComms(commsStr);

			if (Globals.showGUI) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException ex) {
					Logger.getLogger(EngineSimulation.class.getName())
							.log(Level.SEVERE, null, ex);
				}
			}

			if (totalRuns > 2 && nr_dis == engine.getCars().size())
				engine.stopSimulation();
			if (totalRuns % 1440 == 0) {
				// System.out.println( new Date(now.getTime() *
				// 1000).toString());
				engine.printClientsStatus(engine.getCars());
				engine.printServersStatus(engine.getServers());
			}
			if (totalRuns > 1440 * Globals.simulationDays) {
				engine.getMap().interactive = false;
			}

		}
		engine.printClientsStatus(engine.getCars());
		engine.printServersStatus(engine.getServers());
		try {
			if (engine.getServersLog() != null)
				engine.getServersLog().close();
			if (engine.getPeersLog() != null)
				engine.getPeersLog().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.log(Level.WARNING, e.getMessage());
		}

		if (Globals.showGUI) {
			for (int i = 0; i < engine.getCarsView().size(); i++) {
				CarView c = engine.getCarsView().get(i);
				engine.getView().disableCarView(c.getId());
			}

			engine.getMapJ().removeAll();
			engine.getView().panel.remove(engine.getMapJ());
			engine.getView().setMap(engine.getMap());
			engine.getView().panel.revalidate();

			engine.showChart();
			engine.getView().panel.revalidate();
			engine.getView().validate();
			engine.getView().panel.repaint();
			engine.getView().repaint();
		}
		System.out.println("Total " + totalRuns);
		
		/* uncomment when ThreadPool will be in production
		 * waitForThreadPoolProcessing();
		 */
	}

	private void waitForThreadPoolProcessing() {
		// TODO Auto-generated method stub
		while(!ThreadPool.getInstance().isEmpty()) {
			
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				logger.log(Level.WARNING, e.getMessage());
			}
		}
	}
}
