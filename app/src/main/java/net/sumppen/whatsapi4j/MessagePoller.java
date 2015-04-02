package net.sumppen.whatsapi4j;

import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagePoller extends Thread {
	private boolean running = true;
	private final WhatsApi wa;
	private final Logger log = LoggerFactory.getLogger(MessagePoller.class);

	public MessagePoller(WhatsApi wa) {
		this.wa = wa;
	}

	@Override
	public void run() {
		log.info("Message poller starting");
		while(isRunning())
		try {
			wa.pollMessages();
		} catch (SocketTimeoutException e) {
		} catch (Exception e) {
			log.error("Message poller caught exception: "+e.getMessage(), e);
		}
		log.info("Message poller finishing");
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}
}
