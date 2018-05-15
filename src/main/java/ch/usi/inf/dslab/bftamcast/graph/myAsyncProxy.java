/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.graph;

import java.util.Comparator;

import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.Extractor;
import ch.usi.inf.dslab.bftamcast.client.ConsoleClientDirect;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *
 */
public class myAsyncProxy extends AsynchServiceProxy {
	private ConsoleClientDirect c;

	/**
	 *
	 * @param processId
	 *            Replica id
	 */
	public myAsyncProxy(int processId) {
		super(processId, null);
	}

	/**
	 *
	 * @param processId
	 *            Replica id
	 * @param configHome
	 *            Configuration folder
	 */
	public myAsyncProxy(int processId, String configHome, ConsoleClientDirect c) {
		super(processId, configHome);
		this.c = c;
	}

	public myAsyncProxy(int processId, String configHome, Comparator<byte[]> replyComparator,
			Extractor replyExtractor) {
		super(processId, configHome, replyComparator, replyExtractor);
	}

	@Override
	public void replyReceived(TOMMessage reply) {
		if (c != null)
			c.handle(reply);
		// System.out.println("HAJKHSJHAKJHAKHAK");
		// System.out.println("HAJKHSJHAKJHAKHAK");
		// System.out.println("HAJKHSJHAKJHAKHAK");
		// System.out.println("HAJKHSJHAKJHAKHAK");
		// System.out.println("HAJKHSJHAKJHAKHAK");
		// System.out.println("HAJKHSJHAKJHAKHAK");
		// System.out.println("HAJKHSJHAKJHAKHAK");
		// System.out.println("HAJKHSJHAKJHAKHAK");
		// System.out.println("HAJKHSJHAKJHAKHAK");
		// System.out.println("HAJKHSJHAKJHAKHAK");
		// System.out.println("HAJKHSJHAKJHAKHAK");
		// System.out.println("HAJKHSJHAKJHAKHAK");
		// System.out.println("HAJKHSJHAKJHAKHAK");
		// System.out.println("HAJKHSJHAKJHAKHAK");
		// super.replyReceived(reply);
	}

}
