package ch.usi.inf.dslab.byzcast.async;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import ch.usi.inf.dslab.byzcast.util.CLIParser;
import ch.usi.inf.dslab.byzcast.util.Colors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tarcisio Ceolin - tarcisio.ceolin@acad.pucrs.br
 */

public class Replica extends DefaultRecoverable {
	static Logger logger = LoggerFactory.getLogger(Replica.class);
	private int replicaId, groupId;
	private String rootConfigPath, groupConfig;

	private boolean executeFunction;
	private ReplicaReplier replier;

	Colors c = new Colors(); // cores para debug

	public Replica(int replicaId, int groupId, String rootConfigPath, boolean executeFunction) {
		this.replicaId = replicaId;
		this.groupId = groupId;
		this.rootConfigPath = rootConfigPath;
		this.executeFunction = executeFunction;
		this.groupConfig = this.rootConfigPath + "g" + this.groupId;


		logger.info("Group ID: " + this.groupId);
		logger.info("Replica ID: " + this.replicaId);
		logger.info("Config Home: " + this.groupConfig);

		replier = new ReplicaReplier(this.replicaId, this.groupId, this.rootConfigPath, this.executeFunction);

		try {
			Thread.sleep(groupId * 100 + this.replicaId * 200);
		} catch (InterruptedException e) {
			System.err.println("Error starting server " + this.replicaId);
			e.printStackTrace();
			System.exit(-1);
		}

		// Subindo serviço
		new ServiceReplica(this.replicaId, this.groupConfig, this, this,
				null, replier, null);
	}

	public static void main(String[] args) {
		CLIParser p = CLIParser.getReplicaParser(args);
		new Replica(p.getId(), p.getGroup(), p.getRootConfigsPath(), p.executeFunction());
	}

	/**
	 * Given a snapshot received from the state transfer protocol, install it
	 *
	 * @param state The serialized snapshot
	 */
	@Override
	public void installSnapshot(byte[] state) {
        /**
         * No Snapshots for the time being required.
         */
	}

	/**
	 * Returns a serialized snapshot of the application state
	 *
	 * @return A serialized snapshot of the application state
	 */
	@Override
	public byte[] getSnapshot() {
        /**
         * No Snapshots for the time being required.
         */
		return new byte[0];
	}

	/**
	 * Execute a batch of ordered requests
	 *
	 * @param commands      The batch of requests
	 * @param msgCtxs       The context associated to each request
	 * @param fromConsensus true if the request arrived from a consensus execution, false if it arrives from the state transfer protocol
	 * @return the respective replies for each request
	 */
	@Override
	public byte[][] appExecuteBatch(byte[][] commands, MessageContext[] msgCtxs, boolean fromConsensus) {
		// Como o bft-smart entrega um batch de TOMMessage(s),
		logger.debug(c.red() + "Chamou appExecuteBatch" + c.reset());
		replier.setBatchSize(commands.length);
		logger.debug("New batch of size " + commands.length);
		return commands;
	}

	/**
	 * Execute an unordered request
	 *
	 * @param command The unordered request
	 * @param msgCtx  The context associated to the request
	 * @return the reply for the request issued by the client
	 */
	@Override
	public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
		throw new UnsupportedOperationException(c.red() + "Implemented by ReplicaReplier" + c.reset());
	}
}
