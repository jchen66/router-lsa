package socs.network.message;

import java.util.HashMap;
import java.util.LinkedList;
import socs.network.node.LinkStateDatabase;

public class WeightedGraph {
	int numRouter;
	//nodes and edges
	//index represent the 2 nodes, value= weight
	//based from LSD
	int[][] edgesWeight; 
	String[] myID;
	
	public WeightedGraph(LinkStateDatabase db) {
		numRouter = db.get_store().size();
		edgesWeight = new int[numRouter][numRouter]; 
		myID = new String[numRouter];
		int numIDIndex = 0;
		HashMap<String, LSA> myLinks=db.get_store();
		for(int i=0; i<numRouter;i++) {
			for(int j=0; j<numRouter;j++) {
				edgesWeight[i][j] = 0;
			}
		}
		//set up the myID array
		for (String keyID : myLinks.keySet()) {
			int r1Index;
			int r2Index;
			LSA lsa = myLinks.get(keyID);
			LinkedList<LinkDescription> clinks = lsa.links;
			//if linkedList is empty or size=0 --> skip
			//if size=1 --> will no go through the loop and there would be no links. 
			if(clinks.size() >= 1) {
				String ID1 = lsa.linkStateID;
				r1Index=hasID(myID, ID1);
				if(r1Index == -1) {
					//if (numIDIndex < numRouter+1) {
						r1Index = numIDIndex;
						myID[numIDIndex] = lsa.linkStateID;
						numIDIndex++;
					//}
				}
				
				LinkDescription l2; 
				//parse through all the link and establish the costs
				for(int i = 1; i < clinks.size(); i++){
			    	  l2 = clinks.get(i);
			    	  r2Index = hasID(myID, l2.linkID);
			    	  if(r2Index == -1) {
			    		  //if (numIDIndex < numRouter+1) {
				    		  r2Index = numIDIndex; 
				    		  myID[numIDIndex] = l2.linkID;
				    		  numIDIndex++;
			    	  }
				    		  edgesWeight[r1Index][r2Index] = l2.tosMetrics;
			    		 // }
			    		  
			    	  
			   
			    	 
			    }
			}	 
		}
		
		//make sure that 2d array has a 0 diagonal path(router cannot connect to itself)
		
		/*if allowed for some edges to have cost 0: */
	}
	
	/*
	 * returns the index of the router2 with min cost
	 * i.e if cost=0 in adjacency matrix --> there is not edges
	 * assuming there are no edges with 0 cost
	 * 
	 * returns: [INDEX, COST]
	 * */
	public int[] getMinCost(int[] eCosts) {
		int smallestIndex=-1;
		int smallestCost=Integer.MAX_VALUE;
		for(int i=0; i<eCosts.length;i++) {
			int tmpCost=eCosts[i];
			if(tmpCost<=smallestCost) {
				smallestIndex=i;
				smallestCost=tmpCost;
			}
		}
		int[]answer={smallestIndex, smallestCost};
		return answer;
	}
	
	
	/*
	 * 
	 * */
	public String getShortestPath(String source, String destination) {
		String answer = "";
		int numRouters = numRouter;
		boolean[] isAdded = new boolean[numRouters];
		int[] shortestDistances = new int[numRouters];
		int[] prevRouter = new int[numRouters+1];
		// init shortestdistance to infinite and isAdded to false
		for (int i = 0; i < numRouters; i++) {
			isAdded[i] = false;
			shortestDistances[i] = Integer.MAX_VALUE;
			prevRouter[i] = -1;
		}

		int hasSrc = hasID(this.myID, source);
		int hasDest = hasID(this.myID, destination);
		
		// make sure that the graph contains the routers
		if (hasSrc == -1 && hasDest == -1) {
			System.out.println("The Network does not have the routers: " + source + " and " + destination);
		} else if (hasSrc == -1) {
			System.out.println("The Network does not have the source router: " + source);
		} else if (hasDest == -1) {
			System.out.println("The Network does not have the destination router: " + destination);
		} else {
			// init distance for src node (obviously 0)
			shortestDistances[hasSrc] = 0;
			prevRouter[hasSrc] = -1; // -1 being no parent
			
			// assuming router at index 0 is always the source node
			// trying to find the shortest path from source to router[i]
			// calculate the shortest path for all routers
			
			for (int index = 1; index < numRouters; index++) {
				int nearRouter = -1;
				int sDist = Integer.MAX_VALUE;

				for (int i = 0; i < numRouters; i++) {
					// if the router is not added yet (i.e we have not found its shortest distance),
					// and its shortest distance is smaller than the minDistance(infinity at init
					// state)
					// --> set it as nearest router
					
					if ((!isAdded[i]) && shortestDistances[i] < sDist) {
						nearRouter = i;
						sDist = shortestDistances[i];
						
					}
				}
				if (nearRouter != -1) {
					isAdded[nearRouter] = true;

				// update values for all path
					for (int routerIndex = 0; routerIndex < numRouters; routerIndex++) {
						if (!isAdded[routerIndex]) {
							int edgeDistance = edgesWeight[nearRouter][routerIndex];
							// edgeDistance=cost of edge for router[routerIndex] and nearestRouter
							// if edgeDistance<=0, i.e-> the routers are not connected -> then do not update
							// shortestDistances 2d matrix
							// if minDistance+edgeDistance <shortestDistances[routerIndex] --> i.e if the
							// new calculated distance is smaller than the prev
							// --> Then, update the table
							if ((edgeDistance > 0) && ((sDist + edgeDistance) < shortestDistances[routerIndex])) {
								prevRouter[routerIndex] = nearRouter;
								if (routerIndex == hasSrc) prevRouter[routerIndex] = -1;
								shortestDistances[routerIndex] = sDist + edgeDistance;
							}
						}
					}
				}
			}
			// prevRouter contains all the optimal path
			answer = getPathToString(prevRouter, hasSrc, hasDest);
		}
		

		return answer;
	}
	
	/*if path frmo vertex 0 to 9 is 0->3->7->9
	 * returns  " 0 3 7 9"
	 * 
	 * 
	 * */
	public String getPathToString(int[] previousRouter, int source, int destination) {
		String answer= ""+myID[destination]; 
		int current = destination;
		int parent = previousRouter[destination];
		while(parent!=-1) {
			answer = myID[parent]+" ->(" + edgesWeight[current][parent] + ") "+ answer;
			current = parent;
			parent=previousRouter[current];
		}
		return answer;
	}
	
	
	/*if array contains string --> return index
	 * otherwise: return -1
	 * */
	public int hasID(String[] array, String str) {
		for(int i=0; i<array.length;i++) {
			if(array[i] != null && array[i].equals(str)) {
				return i;
			}
		}
		return -1;
	}
	

}