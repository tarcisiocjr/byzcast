package ch.usi.inf.dslab.byzcast.client;

import ch.usi.inf.dslab.byzcast.util.CLIParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 * @author Tarcisio Ceolin - tarcisio.ceolin@acad.pucrs.br
 */
public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    
    public static void main(String[] args) throws InterruptedException {

        CLIParser p = CLIParser.getClientParser(args);
        Random r = new Random();
        int totalTime = p.getDuration();
        int valueSize = p.getMsgSize();
        String[] msgDestination = p.getMsgDestination();
        boolean singleRequest = p.isSingleRequest();
        int clientCount = p.getClientCount();
        int id = p.getId() == 0 ? r.nextInt(Integer.MAX_VALUE) : p.getId();
        int perc = p.getGlobalPercent();
        int lcaId = p.getLcaGroupId();
        String lcaConfigPatch = p.getLcaConfig();
        ClientThread[] clients = new ClientThread[clientCount];
        Thread[] clientThreads = new Thread[clientCount];

        System.out.println(clientCount);

        for (int i = 0; i < clientCount; i++) {
            logger.info("Starting client " + (id + i));
            Thread.sleep(r.nextInt(600));
            clients[i] = new ClientThread(id + i, lcaId, lcaConfigPatch, true, totalTime, valueSize, perc, clientCount, msgDestination, singleRequest);
            clientThreads[i] = new Thread(clients[i]);
            clientThreads[i].start();
        }

        for (int i = 0; i < clientCount; i++) {
            clientThreads[i].join();
            logger.info("Client " + i + " finished execution");
        }
        System.exit(0);
    }

}
