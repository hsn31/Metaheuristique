package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;

import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.lang.*;
import java.util.ArrayList;


public class GreedySolver implements Solver {
	
	/*
	 * SPT (Shortest Processing Time) : donne priorité à la tâche la plus courte ;
	• LPT (Longest Processing Time) : donne priorité à la tâche la plus longue ;
	• SRPT (Shortest Remaining Processing Time) : donne la priorité à la tâche appartenant
		au job ayant la plus petite durée restante;
	• LRPT (Longest Remaining Processing Time) : donne la priorité à la tâche appartenant
		au job ayant la plus grande durée

	 * 
	 */
	
	public enum Priority {SPT, LPT, SRPT, LRPT, EST_SPT, EST_LRPT};
	private Priority priority;
	int [][] debut;
	int[] Machine;

//--solver basic greedyspt greedylpt greedysrpt greedylrpt greedyest_lrpt greedyest_spt descent --instance aaa1 ft06 ft10 ft20
	
	public GreedySolver(Priority priority){
        this.priority = priority;
    }
	
    @Override
    public Result solve(Instance instance, long deadline) {
    	ResourceOrder sol = new ResourceOrder(instance);
        // JobNumbers sol = new JobNumbers(instance);
    	
    	ArrayList<Task> tachesFaiseablesList = new ArrayList<Task>();
    	ArrayList<Task> tachesRealiseesList = new ArrayList<>();
    	
		debut = new int [instance.numJobs][instance.numTasks];
		Machine= new int[instance.numMachines];
		
        int numMachine = 0;
        int numJobs = 0;
        
    	/*
        for(int t = 0 ; t<instance.numTasks ; t++) {
            for(int j = 0 ; j<instance.numJobs ; j++) {
                sol.jobs[sol.nextToSet++] = j;
            }
        }
    	 */
    
    	//On commence par l'initialisation : Déterminer l’ensemble des tâches réalisables (initialement, les premières tâches de tous les jobs)

        for(int j = 0 ; j<instance.numJobs ; j++) {
        	tachesFaiseablesList.add(new Task(j,0)); //Test ok avec Marie-Jo
        }
        
        while(tachesFaiseablesList.size() != 0){   //tant qu’il y a des tâches faiseables
            
        	/*D
        	 * Choisir une tâche dans cet ensemble et placer cette tâche sur la ressource qu’elle
				demande (à la première place libre dans la représentation par ordre de passage)

        	 */
            Task task = selection(tachesFaiseablesList,tachesRealiseesList,instance);

			Machine[instance.machine(task.job,task.task)] = debut[task.job][task.task] + instance.duration(task.job, task.task);
			
            numMachine = instance.machine(task.job,task.task);
            numJobs = 0;

            while(sol.tasksByMachine[numMachine][numJobs] != null){ 
                numJobs++;
            }
            sol.tasksByMachine[numMachine][numJobs] = task;
           
            
            //Mettre à jour l’ensemble des tâches réalisables
            tachesRealiseesList.add(task);

            tachesFaiseablesList = Faiseables(tachesRealiseesList, instance);
        }
        
        Schedule res = sol.toSchedule();
        return new Result(instance, res, Result.ExitCause.Timeout);
        
    }
    
    private ArrayList<Task> Faiseables(ArrayList<Task> tachesRealisees, Instance instance){
    	
    	
    	/*
    	 * On dit qu’une tâche est réalisable (faiseable pour eviter de confondre avec realises) si tous ses prédécesseurs ont été traités (précédences liées
    	 * aux jobs). Il faut noter que chaque tâche doit apparaître exactement une fois dans l’ensemble
    	 * des tâches réalisables et que toutes les tâches doivent être traitées.
    	 */
		 
		ArrayList<Task> tachesfaiseables = new ArrayList<>();
      
        for(int i=0; i < instance.numTasks; i++){
            for(int j=0; j < instance.numJobs; j++) {
                if (!tachesRealisees.contains(new Task(j,i))){ 
                    if(i == 0 || tachesRealisees.contains(new Task(j, i - 1))){
                        tachesfaiseables.add(new Task(j,i));
                    }
                }
            }
        }
        return tachesfaiseables;
		
    }
    
    private Task selection(ArrayList<Task> tachesFaiseables, ArrayList<Task> tachesRealisees, Instance instance){
        if ((this.priority).equals(Priority.SPT)){
                return traitementSPT(tachesFaiseables, instance);
                
        }else if ((this.priority).equals(Priority.EST_SPT)) {
				return traitementESTSPT(tachesFaiseables, instance);
                
        }else if ((this.priority).equals(Priority.LPT)) {
                return traitementLPT(tachesFaiseables, instance);
                
        }else if ((this.priority).equals(Priority.SRPT)) {
                return traitementSRPT(tachesFaiseables, tachesRealisees, instance);
                
        }else if ((this.priority).equals(Priority.LRPT)) {
                return traitementLRPT(tachesFaiseables, tachesRealisees, instance);
                
        }else if ((this.priority).equals(Priority.EST_LRPT)) {
                return traitementESTLRPT(tachesFaiseables, tachesRealisees, instance);
                
        }else  {
                return tachesFaiseables.get(0);
        }
        
    }
    
    private Task traitementSPT (ArrayList<Task> tachesFaiseables, Instance instance){
    	//donne priorité à la tâche la plus courte ;
    	int mintime = Integer.MAX_VALUE;
        Task taskMin = null;
        for (Task t : tachesFaiseables){
            if (instance.duration(t.job,t.task) < mintime) {
                mintime = instance.duration(t.job,t.task);
                taskMin = t;
            }
        }
        return taskMin;
    }
    
    private Task traitementLPT (ArrayList<Task> tachesFaiseables, Instance instance){
    	//donne priorité à la tâche la plus longue ;
		int maxtime = Integer.MIN_VALUE;
        Task taskMin = null;
        for (Task t : tachesFaiseables){
            if (instance.duration(t.job,t.task) > maxtime) {
                maxtime = instance.duration(t.job,t.task);
                taskMin = t;
            }
        }
        return taskMin;
    }
    
    private Task traitementSRPT (ArrayList<Task> tachesFaiseables, ArrayList<Task> tachesRealisees, Instance instance){
    	// donne la priorité à la tâche appartenant au job ayant la plus petite durée restante;
		
		int mintime = Integer.MAX_VALUE;
		int dureejob=0;
		int job=-1;
        Task taskMin = null;
		
        for (Task t : tachesFaiseables){
			
			int j=t.job;
			
            for(int k=0; k< instance.numTasks; k++){
				
				if (!tachesRealisees.contains(new Task(j,k))) {
					dureejob = dureejob+instance.duration(j,k);
				}
			}
			
			if (dureejob != 0 && dureejob < mintime){
                mintime = dureejob;
                dureejob = 0;
                job = j;
            }
        }
		
        for (Task t : tachesFaiseables){
            if (t.job == job) {
                taskMin = t;
                break;
            }
        }
		
        
        return taskMin;

    }
    
    private Task traitementLRPT (ArrayList<Task> tachesFaiseables, ArrayList<Task> tachesRealisees,  Instance instance){
    	// donne la priorité à la tâche appartenant au job ayant la plus grande durée
    			
		int maxtime = Integer.MIN_VALUE;
		int dureejob=0;
		int job=-1;
        Task taskMin = null;
		
        for (Task t : tachesFaiseables){
			
			int j=t.job;
			
            for(int k=0; k< instance.numTasks; k++){
				
				if (!tachesRealisees.contains(new Task(j,k))) {
					dureejob = dureejob+instance.duration(j,k);
				}
			}
			
			if (dureejob != 0 && dureejob > maxtime){
                maxtime = dureejob;
                dureejob = 0;
                job = j;
            }
        }
		
        for (Task t : tachesFaiseables){
            if (t.job == job) {
                taskMin = t;
                break;
            }
        }
		
        
        return taskMin;
    
    }
	
	   private Task traitementESTSPT (ArrayList<Task> tachesFaiseables, Instance instance){
    	// EST
    	return traitementSPT(traitementEST(tachesFaiseables, instance), instance);
    
    }
	
	   private Task traitementESTLRPT (ArrayList<Task> tachesFaiseables, ArrayList<Task> tachesRealisees, Instance instance){
    	// EST
    	return traitementLRPT(traitementEST(tachesFaiseables, instance),tachesRealisees, instance);
    }
	
	
	private ArrayList<Task> traitementEST(ArrayList<Task> tachesFaiseables, Instance instance){
			
        ArrayList<Task> tachesFaiseablesEST = new ArrayList<Task>();

        int EST = Integer.MAX_VALUE;
      
	  
        for (Task t : tachesFaiseables){
            int machine = instance.machine(t.job, t.task);
    
            int test=0;
			
            if(t.task == 0){
                test = 0;
            } else{
                test = debut[t.job][t.task-1] + instance.duration(t.job, t.task-1);
            }
            test = Math.max(test, Machine[machine]);
            debut[t.job][t.task] = test;

            if (test < EST){
                EST = test;
            }
        }


        for (Task t : tachesFaiseables){
            if (debut[t.job][t.task] == EST){
                tachesFaiseablesEST.add(t);
            }
        }
        return tachesFaiseablesEST;
    }
	
	
}
