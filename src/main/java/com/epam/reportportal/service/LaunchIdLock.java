package com.epam.reportportal.service;

/**
 * A service to perform blocking operation to get single launch UUID for multiple clients.
 */
public interface LaunchIdLock {

	/**
	 * Returns a Launch UUID for many clients.
	 *
	 * @param instanceUuid a Client instance UUID, which will be used to identify a Client and a Launch. If it the first one UUID passed to
	 *                     the method it will be returned to every other client instance.
	 * @return either instanceUuid, either the first UUID passed to the method.
	 */
	String obtainLaunchUuid(String instanceUuid);

	/**
	 * Remove self UUID from a lock, means that a Client finished its Launch.
	 *
	 * @param uuid a Client instance UUID.
	 */
	void finishInstanceUuid(String uuid);
}
