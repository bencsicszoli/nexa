package com.nexa.group;

/**
 * Egy felhasználó szerepe egy csoportban (#9). A csoport létrehozója automatikusan
 * {@link #ADMIN}; aki később csatlakozik, {@link #MEMBER}.
 */
public enum GroupRole {
    ADMIN,
    MEMBER
}
