/*
 * Copyright (C) 2015 SFINA Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package agent;

import event.Event;
import event.EventType;
import event.NetworkComponent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import network.FlowNetwork;
import network.Link;
import network.LinkState;
import network.Node;
import org.apache.log4j.Logger;

/**
 * Cascade if link limits violated. Domain independent.
 * @author Ben
 */
public class CascadeAgent extends BenchmarkSimulationAgent{
    
    private static final Logger logger = Logger.getLogger(CascadeAgent.class);
    private HashMap<Integer,LinkedHashMap<FlowNetwork, Boolean>> temporalIslandStatus = new HashMap();
    
    public CascadeAgent(String experimentID){
            super(experimentID);
    }
    
    /**
     * Implements cascade as a result of overloaded links. 
     * Continues until system stabilizes, i.e. no more link overloads occur. 
     * Calls mitigateOverload method before finally calling linkOverload method, 
     * therefore mitigation strategies can be implemented by overwriting the method mitigateOverload.
     * Variable int iter = getIteration()-1
     */
    @Override
    public void runFlowAnalysis(){
        for(FlowNetwork currentIsland : getFlowNetwork().computeIslands()){
            logger.info("treating island with " + currentIsland.getNodes().size() + " nodes");
            boolean converged = flowConvergenceStrategy(currentIsland); 
            if(converged){
                mitigateOverload(currentIsland);
                boolean linkOverloaded = linkOverload(currentIsland);
                boolean nodeOverloaded = nodeOverload(currentIsland);
                if(!linkOverloaded || !nodeOverloaded){
                    logger.info("Island converged: " + currentIsland.getNodes().size() + " nodes :)");
                }
            }
            else{
                updateNonConvergedIsland(currentIsland);
                logger.info("Island didn't converged: " + currentIsland.getNodes().size() + " nodes :(");
            }
        }
    }
    
    
    /**
     * Domain specific strategy and/or necessary adjustments before backend is executed. 
     *
     * @param flowNetwork
     * @return true if flow analysis finally converged, else false
     */ 
    public boolean flowConvergenceStrategy(FlowNetwork flowNetwork){
        return getFlowDomainAgent().flowAnalysis(flowNetwork);
    }
    
    /**
     * Method to mitigate overload. Strategy to respond to (possible) overloading can be implemented here. This method is called before the OverLoadAlgo is called which deactivates affected links/nodes.
     * @param flowNetwork 
     */
    public void mitigateOverload(FlowNetwork flowNetwork){
        logger.info("..no overload mitigation strategy implemented.");
    }
    
    /**
     * Checks link limits. If a limit is violated, an event 
     * for deactivating the link at the current simulation time is created.
     * @param flowNetwork
     * @return if overload happened
     */
    public boolean linkOverload(FlowNetwork flowNetwork){
        boolean overloaded = false;
        for (Link link : flowNetwork.getLinks()){
            if(link.isActivated() && Math.abs(link.getFlow()) > Math.abs(link.getCapacity())){
                logger.info("..violating link " + link.getIndex() + " limit: " + link.getFlow() + " > " + link.getCapacity());
                updateOverloadLink(link);
                overloaded = true;
            }
        }
        return overloaded;
    }
    
    /**
     * Changes the parameters of the link after an overload happened.
     * @param link which is overloaded
     */
    public void updateOverloadLink(Link link){
        Event event = new Event(getSimulationTime(),EventType.TOPOLOGY,NetworkComponent.LINK,link.getIndex(),LinkState.STATUS,false);
        this.getEvents().add(event);
    }
    
    /**
     * Checks node limits. If a limit is violated, an event 
     * for deactivating the node at the current simulation time is created.
     * @param flowNetwork
     * @return if overload happened
     */
    public boolean nodeOverload(FlowNetwork flowNetwork){
        boolean overloaded = false;
        for (Node node : flowNetwork.getNodes()){
            if(node.isActivated() && Math.abs(node.getFlow()) > Math.abs(node.getCapacity())){
                logger.info("..violating node " + node.getIndex() + " limit: " + node.getFlow() + " > " + node.getCapacity());
                updateOverloadNode(node);
                // Uncomment if node overload should be included
                // overloaded = true;
            }
        }
        return overloaded;
    }
    
    /**
     * Changes the parameters of the node after an overload happened.
     * @param node which is overloaded
     */
    public void updateOverloadNode(Node node){
        logger.info("..doing nothing to overloaded node.");
    }
    
    /**
     * Adjust the network part which didn't converge.
     * @param flowNetwork 
     */
    public void updateNonConvergedIsland(FlowNetwork flowNetwork){
        logger.info("..not changing anything in non-converged part of the network.");
    }
    
}
