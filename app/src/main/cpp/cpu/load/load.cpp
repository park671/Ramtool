//
// Created by Park Yu on 2024/10/16.
//

#include "load.h"
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#define MATRIX_SIZE 500

// 矩阵乘法
void matrix_multiply(double** A, double** B, double** C, int size) {
    for (int i = 0; i < size; i++) {
        for (int j = 0; j < size; j++) {
            C[i][j] = 0;
            for (int k = 0; k < size; k++) {
                C[i][j] += A[i][k] * B[k][j];
            }
        }
    }
}

// 初始化矩阵
double** create_matrix(int size) {
    double** matrix = (double**)malloc(size * sizeof(double*));
    for (int i = 0; i < size; i++) {
        matrix[i] = (double*)malloc(size * sizeof(double));
    }
    return matrix;
}

// 释放矩阵内存
void free_matrix(double** matrix, int size) {
    for (int i = 0; i < size; i++) {
        free(matrix[i]);
    }
    free(matrix);
}

// 填充矩阵随机数
void fill_matrix(double** matrix, int size) {
    for (int i = 0; i < size; i++) {
        for (int j = 0; j < size; j++) {
            matrix[i][j] = (double)(rand() % 100) / 10.0;
        }
    }
}

// 占用CPU的复杂计算任务
void occupy_cpu_with_load(int occupy_duration) {
    int size = MATRIX_SIZE;

    // 创建并填充矩阵
    double** A = create_matrix(size);
    double** B = create_matrix(size);
    double** C = create_matrix(size);

    fill_matrix(A, size);
    fill_matrix(B, size);

    // 开始时间
    time_t start_time = time(NULL);
    time_t current_time;

    // 持续执行矩阵乘法以占用 CPU
    do {
        matrix_multiply(A, B, C, size);
        current_time = time(NULL);
    } while (difftime(current_time, start_time) < occupy_duration);

    // 释放内存
    free_matrix(A, size);
    free_matrix(B, size);
    free_matrix(C, size);

    printf("复杂任务完成，CPU 占用结束\n");
}
