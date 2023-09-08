//
// Created by youngpark on 2023/4/3.
//

#ifndef SUPERCUBE_MATRIX_H
#define SUPERCUBE_MATRIX_H

#include "../global.h"

void orthoM(float m[], int mOffset,
            float left, float right, float bottom, float top,
            float near, float far);

#endif //SUPERCUBE_MATRIX_H
