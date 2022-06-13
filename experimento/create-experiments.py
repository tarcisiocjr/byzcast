import os
import shutil
import copy

ipRangeControl = [1,28]
nodes = 28
netAddress = "10.10.1."
localGroups = 4
globalGroups = 3
clients = 1
faultTolerance = 1
regions = ["REG1", "REG2", "REG3", "REG4"]
availabilityZones = ["AZ1"]
zones = []

def generateZonesFile():
    rangeControl = copy.deepcopy(ipRangeControl)
    for i in range(globalGroups):
        for r in regions:
            node = (r, "AZ1", str(netAddress)+str(rangeControl[0]), "server", "g"+str(i))
            if(rangeControl[1] != 0):
                rangeControl[0] += 1
                rangeControl[1] -= 1
                zones.append(node)
                
            
    for i in range(localGroups):
        for r in regions:
            if rangeControl[1] != 0:
                node = (r, "AZ1", str(netAddress)+str(rangeControl[0]), "server", str(i))
                rangeControl[0] += 1
                rangeControl[1] -= 1
                zones.append(node)
    for i in range(clients):
        for r in regions:
            if rangeControl[1] != 0:
                node = (r, "AZ1", str(netAddress)+str(rangeControl[0]), "client", str(i))
                rangeControl[0] += 1
                rangeControl[1] -= 1
                zones.append(node)

    with open('./zones.txt', 'w') as f:
        for zone in zones:
            result = [item for item in zone]
            f.write(str("\t".join(result))+"\n")

def createFilesOnFolder(folderName):
    shutil.copy("./system.config", folderName)
    with open(folderName+'/hosts.config', 'w') as f:
        f.write("#group "+str(folderName[8:])+"\n")
        port = 9998
        for i in range(3 * faultTolerance + 1):
            if ipRangeControl[1] != 1:
                port +=1003
                f.write(str(i)+" "+str(netAddress)+str(ipRangeControl[0])+" "+str(port)+"\n")
                ipRangeControl[0] += 1
                ipRangeControl[1] -= 1 

generateZonesFile()
for i in range(globalGroups):
    folderName = "./group-g"+str(i)
    os.mkdir(folderName)
    createFilesOnFolder(folderName)

for i in range(localGroups):
    folderName = "./group-"+str(i)
    os.mkdir(folderName)
    createFilesOnFolder(folderName)


