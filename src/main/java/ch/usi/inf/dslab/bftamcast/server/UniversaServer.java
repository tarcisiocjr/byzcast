/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class UniversaServer extends DefaultRecoverable{
	private UniversalReplier replier;
	private int id, groupId, nextTS;
	
	
	
	
	public UniversaServer(int id, int group, String configPath) {
		this.id = id;
        this.groupId = group;
        this.nextTS = 0;
        replier = new UniversalReplier(group);

        try {
            Thread.sleep(this.groupId * 4000 + this.id * 1000);
        } catch (InterruptedException e) {
            System.err.println("Error starting server " + this.id);
            e.printStackTrace();
            System.exit(-1);
        }

        new ServiceReplica(this.id, configPath, replier, this, null, replier);
	}

	@Override
	public void installSnapshot(byte[] state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public byte[] getSnapshot() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[][] appExecuteBatch(byte[][] commands, MessageContext[] msgCtxs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
		// TODO Auto-generated method stub
		return null;
	}

}
