package Namingserver.namingserver;

import Namingserver.namingserver.controller.ServerController;
import Namingserver.namingserver.controller.communication.FailureListener;
import Namingserver.namingserver.controller.communication.MulticastReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NamingserverApplication implements CommandLineRunner {
// tracer
	@Autowired
	private ServerController controller;

	public static void main(String[] args) {
		SpringApplication.run(NamingserverApplication.class, args);
	}

	@Override
	public void run(String... args) {
		new Thread(new MulticastReceiver(controller)).start();
		new Thread(new FailureListener(controller)).start();

	}

}
