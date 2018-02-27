/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.server;

import java.io.Serializable;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ReplicaContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.server.FIFOExecutable;
import bftsmart.tom.server.Replier;
import ch.usi.inf.dslab.bftamcast.graph.Tree;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 * Temporary class to start experimenting and understanding byzcast
 * 
 * - understand byzcast structure
 * - make server and clients non blocking (asynchronous)
 * - remove auxiliary groups and use target groups to build the overlay tree
 */
public class UniversalReplier implements Replier, FIFOExecutable, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Tree overlayTree;
	private int groupID;
	

	public UniversalReplier(int groupID, String treeConfig) {
		this.overlayTree = new Tree(treeConfig);
		this.groupID= groupID;
	}

	@Override
	public byte[] executeOrdered(byte[] command, MessageContext msgCtx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] executeUnordered(byte[] command, MessageContext msgCtx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] executeOrderedFIFO(byte[] command, MessageContext msgCtx, int clientId, int operationId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] executeUnorderedFIFO(byte[] command, MessageContext msgCtx, int clientId, int operationId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setReplicaContext(ReplicaContext rc) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void manageReply(TOMMessage request, MessageContext msgCtx) {
		// TODO Auto-generated method stub
		
	}

}
