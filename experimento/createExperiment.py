import os
import createGroups

def getNodeList(nodeListPath): 
    nodes = []
    nodeFile = open(nodeListPath, 'r')
    nodeList = [(line.strip()).split() for line in nodeFile]

    for node in nodeList:
        if len(node) != 0:
            nodes.append(tuple(i for i in node))
    return nodes
    

def checkErrors(params):
    faultTolerance = 3 * int(params['faultTolerance']) + 1
    possibleGroups = int(params['nodeQuantity']) / faultTolerance
    totalGroups = int(params['localGroups']) + int(params['globalGroups'])
    if  possibleGroups <  totalGroups:
        raise ValueError("You don't have nodes enough for "+str(totalGroups)+" groups with "
                            +str(params['faultTolerance'])+" node faulty.")
    
    totalNodes = totalGroups * faultTolerance + int(params['clients'])
    if totalNodes < int(params['nodeQuantity']):
        raise ValueError("You need "+str(totalNodes)+" nodes, but you have only "+str(params['nodeQuantity'])+".")

def main():
    params = {}
    paramsList = open("byzcast.config", "r").read().split()
    for param in paramsList:
        key, value = param.split('=')
        params[key] = value
    
    checkErrors(params)

    nodes = getNodeList(params['nodeList'])

    createGroups.main(params=params, nodes=nodes)
main()