package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
import jobshop.solvers.GreedySolver.Priority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DescentSolver implements Solver {

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
     * � Impl�menter la m�thode de descente dans solve (retournant un Schedule).
     * @see jobshop.Solver#solve(jobshop.Instance, long)
     * 
     */
    

    Boolean bool;
    Schedule best;
    
    @Override
    public Result solve(Instance instance, long deadline) {
    	
    	
    	 //Initialisation : sinit  Glouton(P b) : // g�n�rer une solution r�alisable avec la m�thode de votre choix;
    	Result initialisation = new GreedySolver(Priority.EST_LRPT).solve(instance, deadline);
    	
    	//M�moriser la meilleure solution : s sinit
    	best=initialisation.schedule;
    	
    	bool = true;
    	
    	//R�p�ter
    	while(System.currentTimeMillis() < deadline && bool) {
    		
    		bool = false;
            ResourceOrder order = new ResourceOrder(best);
            
    		List<Block> criticalPathBlocks = blocksOfCriticalPath(order);

        	for (Block block: criticalPathBlocks) {
        		
        		List<Swap> swaps= neighbors(block);

        		for(Swap swap: swaps) {
        			ResourceOrder temp= new ResourceOrder(best);
        			swap.applyOn(temp);
        			
        			int duration= temp.toSchedule().makespan();
        			//System.out.println("TEST MAKESPAN" + duration);
        			
    				if(duration<best.makespan()) {
    					
            			best= temp.toSchedule() ;
            			bool=true;
    				}
        		}
  
        	}
        	
        	}
        	
		return new Result(instance, best, Result.ExitCause.Timeout);
    }
   

    /*
     * 
     * �crire la m�thode blocksOfCriticalPath pour extraire la liste des blocs d�un chemin
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
      
     * �crire la m�thode neighbors pour g�n�rer le voisinage d�un bloc, c�est � dire l�ensemble
des permutations (utiliser pour cela la classe Swap fournie). Le voisinage complet d�une
solution correspond � l�ensemble des voisins, il sera construit dans la m�thode solve.
     
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
