import os
import shutil

def main(params, nodes):
    nodesPerGroup = 3 * int(params['faultTolerance']) + 1
    
    globalGroups = int(params['globalGroups'])
    localGroups = int(params['localGroups'])
    clients = int(params['clients'])
    nodeIndex = 0

    for g in range(globalGroups):
        path = './group-g'+str(g)
        createGroup(nodes, nodesPerGroup, nodeIndex, path)
        nodeIndex = nodeIndex + nodesPerGroup

    for l in range(localGroups):
        path = './group-'+str(l)
        createGroup(nodes, nodesPerGroup, nodeIndex, path)
        nodeIndex = nodeIndex + nodesPerGroup 
    
    createClients(nodes, clients, nodeIndex + nodesPerGroup)

    createZoneFile(nodes)

def createClients(nodes, clients, nodeIndex):
    for c in range(clients):
        zone = list(nodes[nodeIndex + c])
        zone.append("client")
        zone.append("0")
        nodes[nodeIndex + c] = tuple(zone)


def createZoneFile(nodes):
    NUMBER_VALID_OF_COLUMNS = 5
    with open('./zones.txt', 'w') as f:
        for zone in nodes:
            if len(zone) == NUMBER_VALID_OF_COLUMNS:
                f.write(str("\t".join(zone))+"\n")    
            

def createFolder(path):
    if os.path.exists(path):
        shutil.rmtree(path)
    os.mkdir(path)

def createGroup(nodes, nodesPerGroup, nodeIndex, path ):
    INDEX_GROUP_DEFINITION = 8
    createFolder(path)

    shutil.copy("./system.config", path)
    with open(path+'/hosts.config', 'w') as f:
        groupName = path[8:]
        port = 10000
        f.write("#group "+str(groupName)+"\n")
        for i in range(nodesPerGroup):
            pos = nodeIndex + i
            address = nodes[pos][2]
            f.write(str(i)+" "+str(address)+" "+str(port+i)+"\n")
            
            zone = list(nodes[pos])
            zone.append("server")
            zone.append(path[INDEX_GROUP_DEFINITION:])
            nodes[pos] = tuple(zone)

    
            