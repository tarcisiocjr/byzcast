/*
  BFT-SWARM - A (hopeful) high-throughput BFT-SMART implementation
  Copyright (C) 2017, University of Lugano

  This file is part of BFT-SWARM.

  BFT-SWARM is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this library; if not, write to the Free Software
  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package ch.usi.dslab.ceolin.bftswarm;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceProxy;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;

/**
 * @author Tarcisio Ceolin - tarcisio.ceolin.junior@usi.ch
 */
public class ServerGlobal extends Server {

    private static Integer serverGroupId;
    private static Integer serverLocalId;
    private static Integer clientLocalId;
    private static ServiceProxy clientTeste123;

    public static class Proxy {
        public Proxy(int clientId, int groupId) {
            if(clientId == 10000) {
                System.out.println("Testing: " + clientId);
                String configTeste123 = "config-local";
                clientTeste123 = new ServiceProxy(clientId, configTeste123);
            }
        }
    }

    public ServerGlobal(int serverGlobalId, int groupId, int localId, int cLocalId) {
        super(serverGlobalId);
        serverGroupId = groupId;
        serverLocalId = localId;
        clientLocalId = cLocalId;
    }

    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Usage: ServerGlobal <server global id> <group global id> <server local id> <client local id>");
            System.exit(0);
        }

        // Start the client of the local group (ServiceProxy Class).
        // Need be in this order. You can expect a bug if you start the ServerReplica fist.
        new Proxy(Integer.parseInt(args[3]), Integer.parseInt(args[1]));

        // Start the global server (ServiceReplica Class).
        new ServerGlobal(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
    }

    @Override
    public byte[][] appExecuteBatch(byte[][] command, MessageContext[] mcs) {
        byte[][] replies = new byte[command.length][];
        for (int i = 0; i < command.length; i++) {
            replies[i] = executeLocal1(command[i], mcs[i]);
        }
        return replies;
    }

    public byte[] executeLocal1(byte[] command, MessageContext msgCtx) {
        ByteArrayInputStream in = new ByteArrayInputStream(command);
        DataInputStream dis = new DataInputStream(in);
        int reqType;
        try {
            reqType = dis.readInt();
            if (reqType == RequestType.PUT) {
                String key = dis.readUTF();
                String value = dis.readUTF();
                if(serverLocalId == 0){
                    JSONObject objValue = new JSONObject(value);
                    JSONArray arr = objValue.getJSONArray("targetGroup");
                    for (int i = 0; i < arr.length(); i++){
                        if(i == serverGroupId){
                            System.out.println("Received key/value to forward: " + key + " / " + value);
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            DataOutputStream dos = new DataOutputStream(out);
                            dos.writeInt(RequestType.PUT);
                            dos.writeUTF(key);
                            dos.writeUTF(value);
                            byte[] reply123 = clientTeste123.invokeOrdered(out.toByteArray());
                            out.close();
                        }
                    }
                }
//                System.out.println("Received key/value to forward: " + key + " / " + value);
                return null;
            } else {
                System.out.println("Unknown request type: " + reqType);
                return null;
            }
        } catch (IOException e) {
            System.out.println("Exception reading data in the replica: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

}

