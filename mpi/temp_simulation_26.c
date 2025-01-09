#include <mpi.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

// Group number: 26
// Group members: Christian Mariano, Armando Fiorini, MohammadAmin Rahimi 

const double L = 100.0;                 // Length of the 1d domain
const int n = 1000;                     // Total number of points
const int iterations_per_round = 1000;  // Number of iterations for each round of simulation
const double allowed_diff = 0.001;      // Stopping condition: maximum allowed difference between values


double initial_condition(double x, double L) {
    return fabs(x - L / 2);
}


int main(int argc, char **argv) {
    MPI_Init(&argc, &argv);

    int rank, size;
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &size);

    int points_per_process = n / size;

    // Variables declaration and initialization
    double *all_points = NULL;
    double *my_points = (double*) malloc(sizeof(double) * points_per_process);
    double *new_points = NULL;


    // Set initial conditions
    if (rank == 0) {
        all_points = (double*) malloc(sizeof(double) * n);
        all_points[0] = initial_condition(0, L); //first point in x = 0
        for (int i = 1; i < n-1; i++) { //intermediate points
            double interval = L / (n-1);
            all_points[i] = initial_condition(interval * i, L);
        }
        all_points[n-1] = initial_condition(L, L); //last point in x = L
    }

    // Process 0 distributes points array and deletes the global array
    MPI_Scatter(all_points, points_per_process, MPI_DOUBLE, my_points, points_per_process, MPI_DOUBLE, 0, MPI_COMM_WORLD); 
    if (rank == 0) {
        free(all_points);
    }
    //TODO 1st process and last process to handle
    int round = 0;
    while (1) {
        // Perform one round of iterations
        round++;
        for (int t = 0; t < iterations_per_round; t++) {
            // Implement the code for each iteration
            new_points = (double*) malloc(sizeof(double) * points_per_process);
            MPI_Request request[2];
            double right_value;
            double left_value;

            //sending first and/or last values
            if (rank != size-1) { 
                MPI_Send(&my_points[points_per_process-1], 1, MPI_DOUBLE, rank+1, 0, MPI_COMM_WORLD);
                // printf("[P%i] sending\n", rank);
            }
            // MPI_Wait(&request, MPI_STATUS_IGNORE);
            if (rank != 0) {
                MPI_Send(&my_points[0], 1, MPI_DOUBLE, rank-1, 0, MPI_COMM_WORLD);
                // printf("[P%i] sending\n", rank);
            }
            if(rank != size-1){
                MPI_Recv(&right_value, 1, MPI_DOUBLE, rank+1, 0, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
            }
            if(rank!= 0){
                MPI_Recv(&left_value, 1, MPI_DOUBLE, rank-1, 0, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
            }
            // MPI_Wait(&request, MPI_STATUS_IGNORE);

            // receiving values from other processes
            if (rank != size-1 && rank != 0) {
                new_points[points_per_process-1] = (right_value + my_points[points_per_process-1] + my_points[points_per_process-2]) / 3;
                new_points[0] = (left_value + my_points[0] + my_points[1]) / 3;
            }
            else if (rank == size-1) {
                new_points[0] = (left_value+ my_points[0] + my_points[1]) / 3;
                new_points[points_per_process-1] = (my_points[points_per_process-1] + my_points[points_per_process-2]) / 2;
            } else if (rank == 0) {
                new_points[points_per_process-1] = (right_value + my_points[points_per_process-1] + my_points[points_per_process-2]) / 3;
                new_points[0] = (my_points[0] + my_points[1]) / 2;                    
            }

            //intermediate points
            for (int index = 1; index < points_per_process-1; index++) {
                new_points[index] = (my_points[index] + my_points[index-1] + my_points[index+1]) / 3;
            }

            double *temp = NULL;
            temp = new_points;
            new_points = my_points;
            my_points = temp;
            free(new_points);
            // for (int i = 0; i < points_per_process; i++) {
            //     my_points[i] = new_points[i];
            // }

            MPI_Barrier(MPI_COMM_WORLD);
        }

        double local_min = my_points[0];
        double local_max = my_points[0];
        for (int i = 1; i < points_per_process; i++) {
            if (my_points[i] < local_min) local_min = my_points[i];
            if (my_points[i] > local_max) local_max = my_points[i];
        }

        // Compute global minimum and maximum
        double global_min, global_max, max_diff;
        MPI_Reduce(&local_min, &global_min, 1, MPI_DOUBLE, MPI_MIN, 0, MPI_COMM_WORLD);
        MPI_Reduce(&local_max, &global_max, 1, MPI_DOUBLE, MPI_MAX, 0, MPI_COMM_WORLD);
        
        if (rank == 0) {
            max_diff = global_max - global_min;
            printf("Round: %d\tMin: %f.5\tMax: %f.5\tDiff: %f.5\n", round, global_min, global_max, max_diff);
        }
        MPI_Bcast(&max_diff, 1, MPI_DOUBLE, 0, MPI_COMM_WORLD);
        
        // Implement stopping conditions (break)
        if (max_diff < allowed_diff) {
            break;
        }
    }

    //Deallocation
    free(my_points);

    MPI_Finalize();
    return 0;
}
