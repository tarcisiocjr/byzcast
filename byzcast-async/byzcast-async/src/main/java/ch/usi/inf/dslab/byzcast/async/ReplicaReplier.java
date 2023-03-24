package ch.usi.inf.dslab.byzcast.async;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.MessageContext;
import bftsmart.tom.ReplicaContext;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.server.FIFOExecutable;
import bftsmart.tom.server.Replier;
import ch.usi.inf.dslab.byzcast.kvs.Request;
import ch.usi.inf.dslab.byzcast.kvs.RequestStatus;
import ch.usi.inf.dslab.byzcast.kvs.RequestType;
import ch.usi.inf.dslab.byzcast.util.Colors;
import ch.usi.inf.dslab.byzcast.util.TreeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Tarcisio Ceolin - tarcisio.ceolin@acad.pucrs.br
 */

public class ReplicaReplier implements Replier, ReplyListener, FIFOExecutable {
	// Pre-popular tabela
	final char[] symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
	// ByzCast Stuff
	private final ReentrantLock instancesLock;
	private final ReentrantLock replyClientLock;
	private final ReentrantLock replicatedTOMMessagesLock;
	private final ReentrantLock replicatedRepliesLock;
	private final ReentrantLock executeLock;
	private final ReentrantLock batchLock;
	Logger logger = LoggerFactory.getLogger(ReplicaReplier.class);
	// BFT-SMaRt Stuff
	transient Lock replyLock;
	transient Condition contextSet;
	transient ReplicaContext rc;
	Colors c = new Colors(); // cores para debug
	private int replicaId, groupId, clientId, remaining, batchId;
	private String rootConfigPath, byzcastConfig;
	private boolean executeFunction;
	private List<Integer> groupsOnMyReach;
	private int[] routeToGroupsNotOnMyReach;
	private AsynchServiceProxy[] proxyToGroup;
	private Map<String, Request> instances;
	private Map<String, Vector<TOMMessage>> replicatedTOMMessages;
	private Map<String, Vector<Request>> replicatedReplies;
	private Map<Integer, Vector<String>> replySequence;
	private Map<String, Vector<TOMMessage>> replySequenceTM;
	private Map<Integer, Vector<Request>> requestsToBatch;
	private Map<Integer, byte[]> table;

	public ReplicaReplier(int replicaId, int groupId, String rootConfigPath, boolean executeFunction) {
		this.replicaId = replicaId;
		this.groupId = groupId;
		this.rootConfigPath = rootConfigPath;
		this.executeFunction = executeFunction;

		this.replyLock = new ReentrantLock(); // Used for replica context
		this.contextSet = replyLock.newCondition(); // BFT-SMaRt stuff

		// ByzCast locks
		this.replyClientLock = new ReentrantLock();
		this.instancesLock = new ReentrantLock(); // ByzCast instances
		this.replicatedTOMMessagesLock = new ReentrantLock(); // TM instances
		this.replicatedRepliesLock = new ReentrantLock(); // TM instances
		this.executeLock = new ReentrantLock(); // TM instances
		this.batchLock = new ReentrantLock();

		this.byzcastConfig = rootConfigPath;
		this.groupsOnMyReach = new ArrayList<>();
		this.instances = new TreeMap<>(); // Map used to control the instances
		this.table = new TreeMap<>(); // Map used to save the State
		this.replicatedTOMMessages = new TreeMap<>(); // Map used to save TOMMessages
		this.replicatedReplies = new TreeMap<>(); // Map used to control the Replies
		this.replySequence = new TreeMap<>(); // Map used to control the reply sequence to real client (sync communincation)
		this.replySequenceTM = new TreeMap<>(); // Map used to save the TM that are ready to be replied
		this.requestsToBatch = new TreeMap<>(); // Map used to save batches

		// Buscando grupos do próximo nível da árvore
		TreeConfiguration tc = new TreeConfiguration(this.groupId, this.byzcastConfig);
		this.groupsOnMyReach = tc.getGroupsOnMyReach(groupId);

		this.remaining = 0;
		this.batchId = 0;

		// Buscando grupos onde não estou conectado diretamente
		this.routeToGroupsNotOnMyReach = new int[tc.getByzcastTreeMembers().length];
		this.routeToGroupsNotOnMyReach = tc.getGroupsNotOnMyReach(groupId, groupsOnMyReach);
		logger.debug(c.yellow() + "groupsOnMyReach: " + groupsOnMyReach + " routeToGroupsNotOnMyReach: " + Arrays.toString(routeToGroupsNotOnMyReach) + c.reset());

		// Calculando o identificador utilizado para se conectar como cliente
		// em cada uma das réplicas no(s) grupo(s) que estou diretamente conectado.
		this.clientId = ((1000 * (this.groupId + 1)) + this.replicaId);

		// Conectando-se em cada uma das réplicas no(s) grupo(s)
		// que estou diretamente conectado.
		if (!groupsOnMyReach.isEmpty()) {
			// Criando um proxy para o(s) grupo(s) que estou conectado diretamente
			proxyToGroup = new AsynchServiceProxy[tc.getByzcastTreeMembers().length];
			// Conectando
			for (Integer groupToConnect : groupsOnMyReach) {
				logger.debug(c.yellow() + "Conectando: " + groupToConnect + c.reset());
				String configHomeOfGroupToConnect = this.rootConfigPath + "g" + groupToConnect;
				proxyToGroup[groupToConnect] = new AsynchServiceProxy(clientId, configHomeOfGroupToConnect);
			}
		}

		// popular tabela
		popularTable(100000);
	}

	public static void wait(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void manageReply(TOMMessage tm, MessageContext msgCtx) {
		logger.debug(c.red() + "Chamou manageReply " + tm.getOperationId() + c.reset());
		while (rc == null) {
			try {
				this.replyLock.lock();
				this.contextSet.await();
				this.replyLock.unlock();
			} catch (InterruptedException ex) {
				logger.info(ReplicaReplier.class.getName());
			}
		}

		remaining--;

		// Abrindo o(s) Request(s) de TOMMessage
		Request req = new Request();
		req.fromBytes(tm.getContent());

		try {
			if (req.getType() == RequestType.BATCH) {
				Request[] reqs = Request.ArrayfromBytes(req.getValue());
				for (Request r : reqs) {
					manageRequest(r, tm);
				}
			} else {
				manageRequest(req, tm);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void manageRequest(Request req, TOMMessage tm) {
		Request instance;
		this.instancesLock.lock();
		instance = instances.get(req.getUniqueId());
		this.instancesLock.unlock();

		// -------------------
		// Testa primeiro caso
		// -------------------
		if (instance == null) { // line 13: esta é a primeira vez que vejo essa instância?
			logger.debug("UNKNOWN INSTANCE (13): " + req.getUniqueId() + " getSender(): " + tm.getSender() + " INSTANCE STATUS: " + req.getStatus());

			// Se um cliente real me contatou é por esse grupo ser o LCA da
			// requisição. Com isso não é necessita formar quórum.
			if (req.getIsRealClient()) {
				logger.debug("STATUS DO REQUEST: " + req.getStatus() + " REALCLIENT: " + req.getIsRealClient());
				req.setIsRealClient(false);
				req.setStatus(RequestStatus.RCVD); // line 15 - Mark the instance as received (RCVD)
				replyClientLock.lock();
				saveReplySequence(tm.getSender(), req.getUniqueId());
				replyClientLock.unlock();
			} else {
				req.setStatus(RequestStatus.RCVQINC); // line 17 Mark the instance as RCVQINC
				logger.debug("STATUS DO REQUEST: " + req.getStatus());
			}
			instancesLock.lock();
			instances.put(req.getUniqueId(), req); // Save the instance...
			instancesLock.unlock();
			logger.debug("REQUEST SALVO EM INSTANCES: " + req.getUniqueId());
			replicatedTOMMessagesLock.lock();
			Vector<TOMMessage> tms = saveReplicatedTM(req.getUniqueId(), tm); // lines 15 and 17 : save P_orig to respond to
			replicatedTOMMessagesLock.unlock();
			logger.debug("TOMMESSAGE SALVO EM REPLICATEDREQUESTS: " + req.getUniqueId() + " TMSIZE:" + tms.size());

			if (req.getStatus() == RequestStatus.RCVD) { // if rcvd fwd and exec
				forwardAndOrExecute(req); // try the possible guards enabled
			}
			return;
		}
		// -----------------
		// Fim primeiro caso
		// -----------------

		logger.debug("---> KNOW INSTANCE: (19, 24, 27) " + req.getUniqueId() + " getSender(): " + tm.getSender());
		logger.debug("---> INCOMING REQUEST STATUS: " + req.getStatus() + " INSTANCE STATUS: " + instance.getStatus());

		if (remaining == 0) {
			batchLock.lock();
			batch();
			batchLock.unlock();
		}

		// ---------------------------------------------------------
		// Testa caso especial.
		// Precisa ficar acima do próximo caso por causa da recursão
		// ---------------------------------------------------------
		if (instance.getStatus() == RequestStatus.REPLIED) {
			logger.debug("STATUS DO REQUEST: " + req.getStatus());
			req.setGroup(groupId);
			byte[] response;
			response = req.toBytes();
			TOMMessage msg = tm;
			if (!replySequence.containsKey(msg.getSender())) {
				logger.debug(c.purple() + "---> (DELAYED REQ): " + req.getUniqueId() + " TO: " + msg.getSender() + " MY GROUP:" + groupId + c.reset());
				msg.reply.setContent(response);

				rc.getServerCommunicationSystem().send(new int[]{tm.getSender()}, msg.reply);
			}

			return;
		}
		// -----------------
		// Fim caso especial
		// -----------------

		// -------------------
		// Testa segundo caso
		// -------------------
		if (instance.getStatus() == RequestStatus.RCVQINC) {
			logger.debug("SEGUNDO CASO - STATUS DO REQUEST: " + req.getStatus());
			replicatedTOMMessagesLock.lock();
			Vector<TOMMessage> tms = saveReplicatedTM(req.getUniqueId(), tm); // line 20 : save one more
			replicatedTOMMessagesLock.unlock();
			if (tms.size() == (rc.getStaticConfiguration().getN() - rc.getStaticConfiguration().getF())) {
				// line 21 : quorum completed now
				logger.debug("---> QUORUM COMPLETE (21): " + req.getUniqueId());
				req.setStatus(RequestStatus.RCVD); // line 22
				logger.debug("---> Marked as RCVD (22): " + req.getUniqueId());
				instancesLock.lock();
				instances.replace(req.getUniqueId(), req); // line 22: Update the map with new status
				instancesLock.unlock();
				forwardAndOrExecute(req); // try the possible guards enabled
			} else {
				/* no quorum, just saved process to respond */
			}
			return;
		}
		// ----------------
		// Fim segundo caso
		// ----------------


		// ------------------------------------------
		// Testa terceiro caso
		// Requisição já foi encaminhada ou executada
		// ------------------------------------------
		if (req.getStatus() == RequestStatus.FORWARDED || req.getStatus() == RequestStatus.RCVD || req.getStatus() == RequestStatus.EXECUTED) {
			logger.debug("STATUS DO REQUEST: " + req.getStatus());
			replicatedTOMMessagesLock.lock();
			saveReplicatedTM(req.getUniqueId(), tm); // line 25 : save one more
			replicatedTOMMessagesLock.unlock();
		}
		// -----------------
		// Fim terceiro caso
		// -----------------
	}

	/**
	 * Sets the replica context
	 *
	 * @param rc The replica context
	 */
	@Override
	public void setReplicaContext(ReplicaContext rc) {
		this.replyLock.lock();
		this.rc = rc;
		this.contextSet.signalAll();
		this.replyLock.unlock();
	}

	private void forwardAndOrExecute(Request request) {
		boolean fwded = false;
		boolean execd = false;

		Request req = request;
		logger.debug("---> forwardAndOrExecute MY GROUP: " + groupId + " DESTINATION GROUP: " + Arrays.toString(req.getDestination()) + " UID: " + req.getUniqueId());

		try {
			// Se chegou até aqui é por que devemos que executar ou encaminhar.
			for (int dest : req.getDestination()) {
				logger.debug("---> forwardAndOrExecute " + dest);
				if (dest == groupId) {
					// Se o destino é o meu grupo, executamos.
					logger.debug(c.red() + "Sou o grupo de destino. Executando: " + req.getUniqueId() + " STATUS: " + req.getStatus() + c.reset());
					// Salva meu grupo como quem executou.
					req.setGroup(groupId);
					// Executa e armazena o resultado no value da Request.
					byte[] reqResult;
					reqResult = execute(req);
					req.setValue(reqResult);
					execd = true;
				} else if (groupsOnMyReach.contains(dest)) {
					// Se não somos o destino, mas estamos conectados
					// diretamente a grupo, encaminhamos.
					logger.debug(c.red() + "Não sou o grupo de destino, encaminhando: " + req.getUniqueId() + " STATUS: " + req.getStatus() + c.reset());
					fwded = true;
				} else if (routeToGroupsNotOnMyReach[dest] != -1) {
					// Grupo não está diretamente conectado, descobre
					// quem é o grupo conectado e encaminha.
					logger.debug(c.red() + "Não sou o grupo de destino nem estou conectado diretamente" + " com o grupo de destino. Utilizando um intermediário: " + req.getUniqueId() + " STATUS: " + req.getStatus() + c.reset());
					fwded = true;
				}

			}

			if (fwded) {
				req.setStatus(RequestStatus.FORWARDED);
				logger.debug("Marked as FORWARDED (37): " + req.getUniqueId());
				forwardRequest(req.getUniqueId());
			} else {
				req.setStatus(RequestStatus.EXECUTED);
				logger.debug("Marked as EXECUTED (40): " + req.getUniqueId());
			}

			// Atualiza o estatus da requisição.
			instancesLock.lock();
			instances.replace(req.getUniqueId(), req); // line 35 - replace it in the instances set
			instancesLock.unlock();

			if (execd) {
//				logger.debug("execd: " + req.getStatus());
				checkResponsesComplete(req);
			}
		} finally {

		}

	}

	private void forwardRequest(String reqUniqueId) {
		Request req = instances.get(reqUniqueId);

		boolean fwed[] = new boolean[proxyToGroup.length];
		Arrays.fill(fwed, false);

		for (Integer group : req.getDestination()) {
			if (groupsOnMyReach.contains(group) && !fwed[group]) {
				fwed[group] = true;
				Request r = new Request();
				r.setKey(req.getKey());
				r.setType(req.getType());
				r.setUniqueId(req.getUniqueId());
				r.setIsRealClient(false);
				r.setGroup(group);
				r.setDestination(new int[]{group});
				this.batchLock.lock();
				saveRequestToBatch(group, req);
				this.batchLock.unlock();
			} else if (routeToGroupsNotOnMyReach[group] != -1) {
				if (!fwed[routeToGroupsNotOnMyReach[group]]) {
					fwed[routeToGroupsNotOnMyReach[group]] = true;
					fwed[group] = true;
					Request r = new Request();
					r.setKey(req.getKey());
					r.setType(req.getType());
					r.setUniqueId(req.getUniqueId());
					r.setIsRealClient(false);
					r.setGroup(group);
					r.setDestination(new int[]{group});
					this.batchLock.lock();
					saveRequestToBatch(routeToGroupsNotOnMyReach[group], req);
					this.batchLock.unlock();
//  				r.setValue(req.toBytes());
//					byte[] toSend;
//					toSend = r.toBytes();
//					proxyToGroup[routeToGroupsNotOnMyReach[group]].invokeAsynchRequest(toSend, this, TOMMessageType.ORDERED_REQUEST);
				}
			}
		}

		if (remaining == 0) {
			batchLock.lock();
			batch();
			batchLock.unlock();
		}

	}

	private boolean checkResponsesComplete(Request request) {
		Request req = request;

		boolean quorumComplete = false;

		for (int g : req.getDestination()) {
			if (g == groupId) {
				quorumComplete = checkQuorum(req.getUniqueId(), g);
				if (!quorumComplete) {
					break;
				}
			} else if (groupsOnMyReach.contains(g)) {
				logger.debug("CHECKING QUORUM COMPLETE FOR " + g);
				// se temos contato, verificarmos normalmente.
				quorumComplete = checkQuorum(req.getUniqueId(), g);
				if (!quorumComplete) {
					logger.debug("QUORUM INCOMPLETE FOR " + g);
					break;
				}
			} else {
				if (routeToGroupsNotOnMyReach[g] != -1) {
					quorumComplete = checkQuorum(req.getUniqueId(), routeToGroupsNotOnMyReach[g]);
					if (!quorumComplete) {
						logger.debug("QUORUM INCOMPLETE FOR MEDIUM GROUP: " + g + " BY: " + routeToGroupsNotOnMyReach[g]);
						break;
					}
				}
			}
		}

		if (quorumComplete && (req.getStatus() != RequestStatus.REPLIED)) {
			// MARCA INSTANCIA RESPONDIDA
			req.setStatus(RequestStatus.REPLIED);
			instancesLock.lock();
			instances.replace(req.getUniqueId(), req);
			instancesLock.unlock();

			logger.debug(c.red() + "QUORUM COMPLETE: " + req.getUniqueId() + " REQ CONTENT SIZE: " + req.getValue().length + c.reset());

			byte[] response;
			req.setGroup(groupId);
			response = req.toBytes();

			Vector<TOMMessage> tms = replicatedTOMMessages.get(req.getUniqueId());


			for (TOMMessage msg : tms) {
				msg.reply.setContent(response);
				// Verificamos se é um cliente real para responder na ordem enviada pelo cliente.
				if (replySequence.containsKey(msg.getSender())) {
					replyClientLock.lock();
					saveReplySequenceTM(req.getUniqueId(), msg);
					replyClientLock.unlock();
				} else {
					rc.getServerCommunicationSystem().send(new int[]{msg.getSender()}, msg.reply);
				}
			}
			replyClientSync();
		} else {
			logger.debug(c.yellow() + "QUORUM INCOMPLETE OR REQUEST ALREADY REPLIED: " + req.getUniqueId() + " - " + req.getStatus() + c.reset());
		}

		return false;
	}

	private boolean checkRepliesComplete(Request reply) {
		Request rep = reply;
		Request req = instances.get(rep.getUniqueId());

		boolean quorumComplete = false;

		for (int g : req.getDestination()) {
			if (g == groupId) {
				quorumComplete = checkQuorum(req.getUniqueId(), g);
				if (!quorumComplete) {
					logger.debug("QUORUM INCOMPLETE FOR " + g);
					break;
				}
			} else if (groupsOnMyReach.contains(g)) {
				logger.debug("CHECKING QUORUM COMPLETE FOR " + g);
				// se temos contato, verificarmos normalmente.
				quorumComplete = checkQuorum(req.getUniqueId(), g);
				if (!quorumComplete) {
					logger.debug("QUORUM INCOMPLETE FOR " + g);
					break;
				}
			} else {
				if (routeToGroupsNotOnMyReach[g] != -1) {
					quorumComplete = checkQuorum(req.getUniqueId(), routeToGroupsNotOnMyReach[g]);
					if (!quorumComplete) {
						logger.debug("QUORUM INCOMPLETE FOR MEDIUM GROUP: " + g + " BY: " + routeToGroupsNotOnMyReach[g]);
						break;
					}
				}
			}
		}

		if (quorumComplete && (req.getStatus() != RequestStatus.REPLIED)) {
			// MARCA INSTANCIA RESPONDIDA
			req.setStatus(RequestStatus.REPLIED);
			instancesLock.lock();
			instances.replace(req.getUniqueId(), req);
			instancesLock.unlock();

			logger.debug(c.red() + "QUORUM COMPLETE: " + req.getUniqueId() + " REQ CONTENT SIZE: " + req.getValue().length + c.reset());

			byte[] response;
			req.setGroup(groupId);

			rep.setGroup(groupId);
			rep.setStatus(RequestStatus.REPLIED);

			//	Aqui definimos os modelos de execução
			// Concatenado ou Função aplicada.
			if (executeFunction) {
				Set<String> distinctValues = new HashSet<>();
				// Função simples que elimina valores duplicados.
				Vector<Request> replies = replicatedReplies.get(rep.getUniqueId());
				for (Request r : replies) {
					String value = new String(r.getValue());
					logger.debug(c.yellow() + "VALUE: " + value + c.reset());
					if (!distinctValues.contains(value)) {
						distinctValues.add(value);
					}
				}
				logger.debug(c.yellow() + "DISTINCT VALUES: " + distinctValues.toString() + c.reset());
				rep.setValue(distinctValues.toString().getBytes());
			} else {
				// concatena os valores recebidos de baixo e submete para cima
				Map<Integer, Set<String>> concatenateValues = new HashMap<>();
				Vector<Request> replies = replicatedReplies.get(rep.getUniqueId());
				for (Request r : replies) {
					Integer group = r.getGroup();
					String value = new String(r.getValue());
					Set<String> distinctValues = concatenateValues.computeIfAbsent(group, k -> new HashSet<>());
					distinctValues.add(value);
				}
				logger.debug(c.yellow() + "CONCATENATED VALUES: " + concatenateValues.toString() + c.reset());
				rep.setValue(concatenateValues.toString().getBytes());
			}

			response = rep.toBytes();

			Vector<TOMMessage> tms = replicatedTOMMessages.get(req.getUniqueId());

			for (int i = 0; i < tms.size(); i++) {
				TOMMessage msg = tms.get(i);
				msg.reply.setContent(response);
				// Verificamos se é um cliente real para responder na ordem enviada pelo cliente.
				if (replySequence.containsKey(msg.getSender())) {
					logger.debug(c.red() + "É UM CLIENTE REAL, SALVANDO PARA RESPONDER EM ORDEM:" + msg.getSender() + c.reset());
					replyClientLock.lock();
					saveReplySequenceTM(req.getUniqueId(), msg);
					replyClientLock.unlock();
				} else {
					rc.getServerCommunicationSystem().send(new int[]{msg.getSender()}, msg.reply);
				}
			}
			replyClientSync();
		} else {
			logger.debug(c.yellow() + "QUORUM INCOMPLETE OR REQUEST ALREADY REPLIED: "
					+ req.getUniqueId() + " - " + req.getStatus() + c.reset());
		}

		return false;
	}

	private void replyClientSync() {
		replyClientLock.lock();
		logger.debug(c.yellow() + " ENTROU NO FOR DO REPLY CLIENTS" + c.reset());

		for (Map.Entry<Integer, Vector<String>> entry : replySequence.entrySet()) {
			Integer sender = entry.getKey();
			Vector<String> uIds = entry.getValue();

			for (int i = 0; i < uIds.size(); i++) {
				String uId = uIds.get(i);

				for (Map.Entry<String, Vector<TOMMessage>> tmEntry : replySequenceTM.entrySet()) {
					String uId2 = tmEntry.getKey();
					Vector<TOMMessage> tms = tmEntry.getValue();
					if (Objects.equals(uId, uId2)) {
						for (TOMMessage msg : tms) {
							rc.getServerCommunicationSystem().send(new int[]{sender}, msg.reply);
							// Limpando mapas de entradas
							uIds.remove(i);
							replySequenceTM.remove(uId2);
						}
					}
					break;
				}
			}
		}
		replyClientLock.unlock();
	}

	private boolean checkQuorum(String uniqueId, int groupToCheck) {
		logger.debug("---> CHECKING QUORUM FOR UID: " + uniqueId + " GROUP: " + groupId);

		// We don't need a quorum for the same group
		if (groupToCheck == groupId) {
			return true;
		}

		Vector<Request> replies = replicatedReplies.get(uniqueId);
		logger.debug(c.yellow() + "REPLIES TO THIS UNIQUEID: " + uniqueId + " TOSTRING: " + replies.toString() + " TOTAL REPLIES: " + replies.size() + c.reset());

		// We assume that the number of replicas is equal in all groups
		int n = rc.getStaticConfiguration().getN();
		int f = rc.getStaticConfiguration().getF();

		int counter = 0;

		for (int i = 0; i < replies.size(); i++) {
			Request r = replies.get(i);
			if (groupsOnMyReach.contains(r.getGroup())) {
				if (r.getGroup() == groupToCheck) {
					counter++;
				}
			} else {
				if (routeToGroupsNotOnMyReach[r.getGroup()] != -1) {
					if (routeToGroupsNotOnMyReach[r.getGroup()] == groupToCheck) {
						counter++;
					}
				}
			}
		}

		// Check for quorum complete
		if (counter >= (n - f)) {
			return true;
		} else {
			logger.debug("---> QUORUM INCOMPLETE: " + uniqueId + " QUORUM: " + counter
					+ " FOR GROUP: " + groupId);
		}

		return false;
	}

	protected byte[] execute(Request req) {
		byte[] resultBytes;

		logger.debug(c.green() + "Executando: " + req.getKey() + c.reset());

		switch (req.getType()) {
			case PUT:
				executeLock.lock();
				resultBytes = table.put(req.getKey(), req.getValue());
				executeLock.lock();
				break;
			case GET:
				resultBytes = table.get(req.getKey());
				break;
			case REMOVE:
				resultBytes = table.remove(req.getKey());
				break;
			case SIZE:
				resultBytes = ByteBuffer.allocate(4).putInt(table.size()).array();
				break;
			default:
				resultBytes = null;
				System.err.println("Unknown request type: " + req.getType());
		}
		return resultBytes;
	}

	/**
	 * Used to deliver a reply from a replica
	 *
	 * @param context The context associated to the reply
	 * @param tm      the TOMMessage including the reply
	 */
	@Override
	public void replyReceived(RequestContext context, TOMMessage tm) {
		logger.debug(c.red() + "Chamou replyReceived" + c.reset());
		logger.debug("Asynchronously received reply from " + tm.getSender() + " with sequence number " + tm.getSequence() + " and operation ID " + tm.getOperationId() + " content size: " + tm.getContent().length);

		Request req = new Request();
		req.fromBytes(tm.getContent()); // Open the request (TOMMessage)

		logger.debug(c.green() + "REPLYRECEIVED:  " + tm.getSender() + " UID: " + req.getUniqueId() + " GROUP: " + req.getGroup() + " MSG TYPE: " + req.getType() + " UNIQUEID: " + req.getUniqueId() + c.reset());

		if (req.getType() == RequestType.BATCHREPLY) {
			Request[] reqs1 = Request.ArrayfromBytes(req.getValue());
			for (Request r : reqs1) {
				logger.debug(c.green() + "REPLYRECEIVED DENTRO DE BATCH:  " + tm.getSender() + " UID: " + r.getUniqueId() + " GROUP: " + r.getGroup() + " MSG TYPE: " + r.getType() + " UNIQUEID: " + r.getUniqueId() + c.reset());
				trataReplies(r);
			}
		} else {
			trataReplies(req);
		}
	}

	public void trataReplies(Request request) {
//		logger.debug(c.yellow() + "CHEGOU NO trataReplies " + c.reset());
		try {
			this.instancesLock.lock();
			Request instance = instances.get(request.getUniqueId());
			this.instancesLock.unlock();

			// CONTABILIZA MAIS UMA RESPOSTA
			this.replicatedRepliesLock.lock();
			saveReplicatedReply(instance.getUniqueId(), request);
			this.replicatedRepliesLock.unlock();
			// ATUALIZA CTL-RESP - ARMAZENA RESPOSTA DO GRUPO - VE SE ELE FORMOU QUORUM
			checkRepliesComplete(request);
		} catch (Exception ex) {
			logger.error("Error processing received request", ex);
		} finally {
		}
	}

	/**
	 * Method called to execute a request totally ordered.
	 * <p>
	 * The message context contains some useful information such as the command
	 * sender.
	 *
	 * @param command the command issue by the client
	 * @param msgCtx  information related with the command
	 * @return the reply for the request issued by the client
	 */
	@Override
	public byte[] executeUnordered(byte[] command, MessageContext msgCtx) {
		return new byte[0];
	}

	@Override
	public byte[] executeOrderedFIFO(byte[] command, MessageContext msgCtx, int clientId, int operationId) {
		return new byte[0];
	}

	/**
	 * @param command
	 * @param msgCtx
	 * @param clientId
	 * @param operationId
	 * @return
	 */
	@Override
	public byte[] executeUnorderedFIFO(byte[] command, MessageContext msgCtx, int clientId, int operationId) {
		return new byte[0];
	}

	@Override
	public byte[] executeOrdered(byte[] command, MessageContext msgCtx) {
		return new byte[0];
	}

	String generateUniqueId() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Indicates that the proxy re-issued the request and the listener should re-initialize
	 */
	@Override
	public void reset() {
	}

	void setBatchSize(int size) {
		try {
			remaining += size;
			logger.info("New batch of size " + size + ", remaining " + remaining);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void saveReplicatedReply(String uniqueId, Request request) {
		Vector<Request> messages = replicatedReplies.computeIfAbsent(uniqueId, k -> new Vector<>());
		messages.add(request);
	}

	private Map<Integer, Set<String>> getDistinctValuesByGroup(String uniqueId) {
		Map<Integer, Set<String>> distinctValuesByGroup = new HashMap<>();
		Vector<Request> messages = replicatedReplies.get(uniqueId);
		for (Request request : messages) {
			Integer group = request.getGroup();
			String value = new String(request.getValue());
			Set<String> distinctValues = distinctValuesByGroup.computeIfAbsent(group, k -> new HashSet<>());
			distinctValues.add(value);
		}
		return distinctValuesByGroup;
	}

	// Salva Request
	private Vector<TOMMessage> saveReplicatedTM(String uniqueId, TOMMessage tm) {
		Vector<TOMMessage> tms = replicatedTOMMessages.computeIfAbsent(uniqueId, k -> new Vector<>());
		tms.add(tm);
		return tms;
	}

	private void saveReplySequence(Integer sender, String reqUniqueId) {
		Vector<String> messages = replySequence.computeIfAbsent(sender, k -> new Vector<>());
		messages.add(reqUniqueId);
	}

	private void saveReplySequenceTM(String reqUniqueId, TOMMessage tm) {
		Vector<TOMMessage> messages = replySequenceTM.computeIfAbsent(reqUniqueId, k -> new Vector<>());
		messages.add(tm);
	}

	private Vector<Request> saveRequestToBatch(Integer uniqueId, Request request) {
		Vector<Request> messages = requestsToBatch.computeIfAbsent(uniqueId, k -> new Vector<>());
		messages.add(request);
		return messages;
	}

	// Busta request no batch por id
	private Vector<Request> getRequestToBatch(Integer uniqueId, Map<Integer, Vector<Request>> t) {
		Vector<Request> messages = t.computeIfAbsent(uniqueId, k -> new Vector<>());
		return messages;
	}

	public void popularTable(Integer qtd) {
		for (int i = 0; i < qtd; i++) {
			table.put(i, notRandomString(64).getBytes());
		}
		logger.debug(c.red() + "tabela populada" + c.reset());
	}

	String notRandomString(int len) {
		char[] buf = new char[len];

		for (int idx = 0; idx < buf.length; ++idx)
			buf[idx] = symbols[idx];
		return new String(buf);
	}

	public void batch() {
		//   ____       _______ _____ _    _
		//  |  _ \   /\|__   __/ ____| |  | |
		//  | |_) | /  \  | | | |    | |__| |
		//  |  _ < / /\ \ | | | |    |  __  |
		//  | |_) / ____ \| | | |____| |  | |
		//  |____/_/    \_\_|  \_____|_|  |_|

		// muda a referência do mapa com requisições a serem enviadas (t)
		// em um batch para todos os grupos e inicializa novo mapa para
		// inserir as novas requisições que chegarem
		this.batchLock.lock();
		Map<Integer, Vector<Request>> t = requestsToBatch;
		requestsToBatch = new TreeMap<>();
		this.batchLock.unlock();

		for (Integer group : groupsOnMyReach) {
			Vector<Request> message = getRequestToBatch(group, t);

			if (!message.isEmpty()) {
				Request reqs = new Request();
				reqs.setKey(batchId);
				reqs.setType(RequestType.BATCH);
				reqs.setUniqueId(generateUniqueId());
				reqs.setIsRealClient(false);
				reqs.setGroup(group);
				int[] destInArray = {group};
				reqs.setDestination(destInArray);

				List<Request> tempList = new ArrayList<>();
				for (int i = 0; i < message.size(); i++) {
					Request r = message.get(i);
					if (r.getStatus() != RequestStatus.REPLIED) {
						tempList.add(r);
						logger.debug(c.yellow() + "ADDED UID: " + r.getUniqueId() + "TO BATCH DOWN: " + reqs.getUniqueId() + c.reset());
					}
				}

				Request[] tempArray = new Request[tempList.size()];
				reqs.setValue(Request.ArrayToBytes(tempList.toArray(tempArray)));

				byte[] toSend2;
				toSend2 = reqs.toBytes();
				proxyToGroup[group].invokeAsynchRequest(toSend2, this, TOMMessageType.ORDERED_REQUEST);
				batchId++;
			}
		}
		t.clear();
	}
}