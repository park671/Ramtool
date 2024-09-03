.text
.global sve2_add_arrays
.arch armv9-a+sve

sve2_add_arrays:
    ptrue   p0.b

    ld1b    z0.b, p0/z, [x0]
    ld1b    z1.b, p0/z, [x1]

    add     z2.b, z0.b, z1.b

    st1b    z2.b, p0, [x2]

    ret
