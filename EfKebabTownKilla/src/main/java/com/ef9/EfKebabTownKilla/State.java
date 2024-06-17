package com.ef9.EfKebabTownKilla;

// KebabTown State
/**
 * Acquire Kebabs
 * ---
 * RUNNING_KEBABMAN
 * TALK_KEBABMAN
 * CONTINUE
 * GET_KEBAB
 *
 * Train combat
 * ---
 * RUNNING_MOBS
 * ATTACK_MOB
 * ATTACKING_MOB
 *
 * Find Room
 *
 * misc
 * ---
 * BREAK
 * WAITING
 * TIMEOUT
 */

public enum State {
    RUNNING_MOBS,
    ATTACK_MOB,
    ATTACKING_MOB,
    RUNNING_KEBABMAN,
    TALK_KEBABMAN,
    BREAK,
    WAITING,
    TIMEOUT,
    CONTINUE,
    GET_KEBAB
}

// Obtaining Food
// Fish

// Kebab room
// NE: 3275, 3183
// SE: 3275, 3179
// SW: 3271, 3179
// NW: 3271, 3183

// Center room
// NE: 3298, 3178
// SE: 3298, 3167
// SW: 3287, 3167
// NW: 3287, 3178

// East room
// NE: 3303, 3177
// SE: 3303, 3167
// SW: 3287, 3167
// NW: 3299, 3177

// West room
// NE: 3286, 3177
// SE: 3286, 3167
// SW: 3282, 3167
// NW: 3282, 3177