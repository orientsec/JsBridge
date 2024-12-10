package com.orientsec.jsbridge

/**
 * Interface for objects that can be destroyed.
 *
 * This interface defines a contract for objects that require explicit cleanup or resource release.
 */
interface Destroyable {
    /**
     * Destroys the object, releasing any resources it holds.
     */
    fun destroy()
}
