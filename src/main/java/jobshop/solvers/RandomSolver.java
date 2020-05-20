package jobshop.solvers;

import jobshop.*;
import jobshop.encodings.JobNumbers;

import java.util.Optional;
import java.util.Random;
import java.util.ArrayList;


public class RandomSolver implements Solver {
	
	public enum Priority {SPT, LPT, SRPT, LRPT};
	
	
    @Override
    public Result solve(Instance instance, long deadline) {

        JobNumbers sol = new JobNumbers(instance);
        for(int t = 0 ; t<instance.numTasks ; t++) {
            for(int j = 0 ; j<instance.numJobs ; j++) {
                sol.jobs[sol.nextToSet++] = j;
            }
        }

        return new Result(instance, sol.toSchedule(), Result.ExitCause.Blocked);
    }

    
}


