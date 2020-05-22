package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
import jobshop.solvers.DescentSolver.Block;
import jobshop.solvers.DescentSolver.Swap;
import jobshop.solvers.GreedySolver.Priority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabooSolver implements Solver {

    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    static class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
    }
    
    int dureeTaboo;
    int maxIter;
    
    //Constructeur de Taboo
    
    public TabooSolver(int dureeTaboo, int maxIter) {
        this.dureeTaboo = dureeTaboo;
        this.maxIter = maxIter;
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swam with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /** Apply this swap on the given resource order, transforming it into a new solution. */
        public void applyOn(ResourceOrder order) {

            Task temp = order.tasksByMachine[this.machine][this.t1];
            order.tasksByMachine[this.machine][this.t1] = order.tasksByMachine[this.machine][this.t2];
            order.tasksByMachine[this.machine][this.t2] = temp;
            
        }
    }


    /*
     * 
     * • Implémenter la méthode de descente dans solve (retournant un Schedule).
     * @see jobshop.Solver#solve(jobshop.Instance, long)
     * 
     */
    
//--solver greedyest_lrpt descent taboo --instance aaa1 ft06 ft10 ft20 la01 la02 la03 la04 la05 la06 la07 la08 la09
    
    Boolean bool;
    Schedule best;
    Schedule current;
    
    @Override
    public Result solve(Instance instance, long deadline) {
    	
    	
    	 //Initialisation : sinit  Glouton(P b) : // générer une solution réalisable avec la méthode de votre choix;
    	Result initialisation = new GreedySolver(Priority.EST_LRPT).solve(instance, deadline);
    	
    	//Mémoriser la meilleure solution : s sinit
    	best=initialisation.schedule;
    	
    	//solution courante
    	current =best;
    	
    	bool = true;
    	
    	//compteur itérations
    	int compt =0;
    	
    	// solutions tabou
    	int [][] Taboo = new int [instance.numJobs * instance.numMachines][instance.numJobs * instance.numMachines];
    	
    	//Répéter  // Exploration des voisinages successifs

    	while(System.currentTimeMillis() < deadline && bool && (compt <= maxIter)) {
    		
    		bool = false;
    		compt++;
    		
            ResourceOrder order = new ResourceOrder(current);
            Swap MeilleurSwap = null;
            
    		List<Block> criticalPathBlocks = blocksOfCriticalPath(order);

        	for (Block block: criticalPathBlocks) {
        		
        		List<Swap> swaps= neighbors(block);

        		for(Swap swap: swaps) {
        			ResourceOrder temp= order.copy();
        			
        			if (Taboo[swap.t1 + instance.numJobs * swap.machine][swap.t2 + instance.numJobs * swap.machine] <= compt) {
        			    swap.applyOn(temp);
        			
        			Schedule duration= temp.toSchedule();
        			//System.out.println("TEST MAKESPAN" + duration);
        			
    				if(order == null || (duration.makespan() < order.toSchedule().makespan())) {
    					order = temp.copy();
    					MeilleurSwap=  new Swap(swap.machine, swap.t1, swap.t2);
            			bool=true;
    				}
        			}
        		}
  
        	}
        	if (MeilleurSwap != null) {
        		/*
        	System.out.print("*******");
        		
        	//System.out.print("TEST taboo" + Taboo);
        	System.out.print("TEST taboo n° " + MeilleurSwap.t1 + instance.numJobs * MeilleurSwap.machine);
        	System.out.print("TEST taboo n° " + MeilleurSwap.t2 + instance.numJobs * MeilleurSwap.machine);
        	
        	System.out.print("*********");
        	*/
            
            Taboo[MeilleurSwap.t2 + instance.numJobs * MeilleurSwap.machine][MeilleurSwap.t1 + instance.numJobs * MeilleurSwap.machine] = compt + dureeTaboo;
            current = order.toSchedule();
        	}

            if (order.toSchedule().makespan() < best.makespan()) {
                best = order.toSchedule();
            }	
        	
        	}
        	
		return new Result(instance, best, Result.ExitCause.Timeout);
    }

    /*
     * 
     * Écrire la méthode blocksOfCriticalPath pour extraire la liste des blocs d’un chemin
critique

     */
    
    /** Returns a list of all blocks of the critical path. */
    List<Block> blocksOfCriticalPath(ResourceOrder order) {
    	
        List<Task> cheminCritique = order.toSchedule().criticalPath();
        List<Block> listeDesBlocs = new ArrayList<>();
        
        Task task = cheminCritique.get(0);
        
        int machine = order.instance.machine(cheminCritique.get(0));
        int firstTask = Arrays.asList(order.tasksByMachine[machine]).indexOf(task);
        int lastTask = firstTask;
        
        for (int i = 1; i < cheminCritique.size(); i++) {
        	
        	task = cheminCritique.get(i);
            
        	if (machine == order.instance.machine(task)) {
                lastTask++;
                
            } else {
                
            	if (firstTask != lastTask) {
                	listeDesBlocs.add(new Block(machine, firstTask, lastTask));
                	}
            	
                machine = order.instance.machine(task);
                firstTask = Arrays.asList(order.tasksByMachine[machine]).indexOf(task);
                lastTask = firstTask;
                
            }
        }
        return listeDesBlocs;

    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    
    
    /*
      
     * Écrire la méthode neighbors pour générer le voisinage d’un bloc, c’est à dire l’ensemble
des permutations (utiliser pour cela la classe Swap fournie). Le voisinage complet d’une
solution correspond à l’ensemble des voisins, il sera construit dans la méthode solve.
     
     */
    List<Swap> neighbors(Block block) {
        List<Swap> swap = new ArrayList<Swap>();
        int calc = block.lastTask - block.firstTask+1;

        if (calc >= 2) {
            swap.add(new Swap(block.machine, block.firstTask, block.firstTask+1 ));
            swap.add(new Swap(block.machine, block.lastTask-1, block.lastTask   ));
        }
        else {
            swap.add(new Swap(block.machine,block.firstTask,block.lastTask));
        }

        return swap;
        
    }

}
